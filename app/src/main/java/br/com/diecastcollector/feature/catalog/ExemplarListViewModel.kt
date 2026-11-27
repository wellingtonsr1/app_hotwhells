package br.com.diecastcollector.feature.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.DuplicateAvailability
import br.com.diecastcollector.domain.model.DuplicateOffer
import br.com.diecastcollector.domain.model.Exemplar
import br.com.diecastcollector.domain.model.ExemplarStatus
import br.com.diecastcollector.domain.model.Location
import br.com.diecastcollector.domain.model.MoneyCents
import br.com.diecastcollector.domain.model.Negotiation
import br.com.diecastcollector.domain.repository.DuplicateRepository
import br.com.diecastcollector.domain.repository.ExemplarRepository
import br.com.diecastcollector.domain.repository.LocationRepository
import br.com.diecastcollector.domain.repository.NegotiationRepository
import br.com.diecastcollector.domain.repository.PhotoRepository
import br.com.diecastcollector.domain.usecase.UniqueExemplarPolicy
import br.com.diecastcollector.feature.duplicates.PendingOffer
import br.com.diecastcollector.navigation.RouteArgs
import br.com.diecastcollector.ui.UiMessage
import br.com.diecastcollector.ui.toUiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Lista os exemplares possuídos de uma variante e a disponibilidade de cada um.
 *
 * É o degrau que faltava entre o cartão do catálogo e o exemplar: sem ela, editar ou mover
 * uma unidade exigiria saber o id de cor. Arquivados não aparecem (FR-044).
 *
 * A disponibilidade e a negociação vêm de **uma consulta por fluxo** (`offers` e
 * `activeNegotiations`), nunca de uma consulta por linha (NFR-503); e a lista serve de
 * origem tanto para marcar oferta quanto para negociar exemplar único (FR-501, FR-502).
 */
public data class ExemplarListUiState(
    val variantId: String = "",
    val exemplars: List<Exemplar> = emptyList(),
    val locationNames: Map<String, String> = emptyMap(),
    /** Foto de capa por id de exemplar; quem não tem foto fica de fora e recebe o marcador. */
    val photoPaths: Map<String, String> = emptyMap(),
    /** Oferta atual por id de exemplar; sem oferta o exemplar simplesmente não está aqui. */
    val offers: Map<String, DuplicateOffer> = emptyMap(),
    /** Negociação ativa por id de exemplar (FR-505). */
    val activeNegotiations: Map<String, Negotiation> = emptyMap(),
    val selectedId: String? = null,
    val sheetOpen: Boolean = false,
    val pendingOffer: PendingOffer? = null,
    val uniqueWarning: Boolean = false,
    /** Exemplar à espera do **Continuar** do aviso de negociação (FR-515b). */
    val pendingNegotiationId: String? = null,
    /** Exemplar que a tela deve abrir na negociação; consumido por `onNegotiationOpened`. */
    val openNegotiationId: String? = null,
    val savingOffer: Boolean = false,
    val message: UiMessage? = null,
    val loading: Boolean = true,
) {
    public val isEmpty: Boolean get() = !loading && exemplars.isEmpty()

    public val selected: Exemplar? get() = exemplars.firstOrNull { it.id == selectedId }

    public fun hasActiveNegotiation(exemplarId: String): Boolean =
        activeNegotiations.containsKey(exemplarId)
}

@HiltViewModel
public class ExemplarListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    exemplarRepository: ExemplarRepository,
    locationRepository: LocationRepository,
    photoRepository: PhotoRepository,
    private val duplicates: DuplicateRepository,
    negotiations: NegotiationRepository,
) : ViewModel() {

    private val variantId: String = savedStateHandle[RouteArgs.VARIANT_ID] ?: ""

    private data class Sources(
        val exemplars: List<Exemplar>,
        val locations: List<Location>,
        val photos: Map<String, String>,
        val offers: Map<String, DuplicateOffer>,
        val negotiations: Map<String, Negotiation>,
    )

    /** Só o que a interação cria: seleção, folha, aviso e mensagem. */
    private data class SheetState(
        val selectedId: String? = null,
        val sheetOpen: Boolean = false,
        val pendingOffer: PendingOffer? = null,
        val uniqueWarning: Boolean = false,
        val pendingNegotiationId: String? = null,
        val openNegotiationId: String? = null,
        val saving: Boolean = false,
        val message: UiMessage? = null,
    )

    private val sheet = MutableStateFlow(SheetState())

    private val sources = combine(
        exemplarRepository.observeByVariant(variantId),
        locationRepository.observeActive(),
        photoRepository.observePrimaryPathsByVariant(variantId),
        duplicates.observeOffersByVariant(variantId),
        negotiations.observeActiveByVariant(variantId),
    ) { exemplars, locations, photos, offers, active ->
        Sources(exemplars, locations, photos, offers, active)
    }

    public val state: StateFlow<ExemplarListUiState> = combine(sources, sheet) { loaded, ui ->
        ExemplarListUiState(
            variantId = variantId,
            exemplars = loaded.exemplars,
            locationNames = loaded.locations.associate { it.id to it.name },
            photoPaths = loaded.photos,
            offers = loaded.offers,
            activeNegotiations = loaded.negotiations,
            selectedId = ui.selectedId,
            sheetOpen = ui.sheetOpen,
            pendingOffer = ui.pendingOffer,
            uniqueWarning = ui.uniqueWarning,
            pendingNegotiationId = ui.pendingNegotiationId,
            openNegotiationId = ui.openNegotiationId,
            savingOffer = ui.saving,
            message = ui.message,
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = ExemplarListUiState(variantId = variantId),
    )

    // ---- Disponibilidade ------------------------------------------------------------

    /** Abre a folha já com a oferta atual; exemplar fora do acervo não oferece a ação. */
    public fun onOpenOfferSheet(exemplarId: String) {
        val exemplar = state.value.exemplars.firstOrNull { it.id == exemplarId } ?: return
        if (exemplar.status != ExemplarStatus.OWNED) return
        sheet.update {
            it.copy(selectedId = exemplarId, sheetOpen = true, pendingOffer = null, uniqueWarning = false)
        }
    }

    public fun onDismissSheet() = sheet.update { SheetState() }

    /**
     * A janela foi recriada (rotação, redimensionamento): a intenção pendente é só
     * memória e não pode ser restaurada (SC-505). O ViewModel sobrevive à recriação, então
     * a tela limpa aqui o que estava à espera de confirmação; a folha e o estado do
     * repositório seguem como estão.
     */
    public fun onWindowRecreated() = sheet.update {
        it.copy(pendingOffer = null, uniqueWarning = false, pendingNegotiationId = null)
    }

    public fun onMessageShown() = sheet.update { it.copy(message = null) }

    /**
     * Salvar a folha. Com uma única unidade na variante e oferta que a torna negociável, a
     * gravação espera o **Continuar** do aviso (FR-515a, RN-003).
     */
    public fun onSaveOffer(
        availability: DuplicateAvailability,
        condition: Condition?,
        priceText: String,
        notes: String,
    ) {
        val current = state.value
        val exemplar = current.selected ?: return
        if (current.savingOffer) return

        val askingPrice = if (priceText.isBlank()) {
            null
        } else {
            MoneyCents.parsePtBrOrNull(priceText)
                ?: return sheet.update { it.copy(message = UiMessage(R.string.error_invalid_money)) }
        }
        val pending = PendingOffer(
            availability = availability,
            condition = condition,
            askingPrice = askingPrice,
            notes = notes,
        )
        val warns = UniqueExemplarPolicy.warnsOnAvailability(
            ownedInVariant = current.exemplars.size,
            status = exemplar.status,
            availability = availability,
            hasActiveNegotiation = current.hasActiveNegotiation(exemplar.id),
        )
        if (warns) {
            sheet.update { it.copy(pendingOffer = pending, uniqueWarning = true) }
        } else {
            persist(exemplar.id, pending)
        }
    }

    public fun onContinueUniqueWarning() {
        val current = state.value
        // O mesmo diálogo serve aos dois momentos: negociar e disponibilizar.
        current.pendingNegotiationId?.let { exemplarId ->
            sheet.update {
                it.copy(
                    openNegotiationId = exemplarId,
                    pendingNegotiationId = null,
                    uniqueWarning = false,
                )
            }
            return
        }
        val exemplarId = current.selectedId ?: return
        val pending = current.pendingOffer ?: return
        persist(exemplarId, pending)
    }

    /** Cancelar o aviso não grava nem navega: volta para a origem como estava. */
    public fun onCancelUniqueWarning() = sheet.update {
        it.copy(pendingOffer = null, pendingNegotiationId = null, uniqueWarning = false)
    }

    /**
     * Intenção de negociar (FR-513, FR-515b).
     *
     * Com uma única unidade no acervo o aviso aparece **antes** de navegar; com duas ou
     * mais, ou com negociação já ativa, vai direto (a tela de negociação mostra o estado
     * ativo e não oferece uma segunda abertura).
     */
    public fun onNegotiate(exemplarId: String) {
        val current = state.value
        val exemplar = current.exemplars.firstOrNull { it.id == exemplarId } ?: return
        if (exemplar.status != ExemplarStatus.OWNED) return

        val warns = !current.hasActiveNegotiation(exemplarId) &&
            UniqueExemplarPolicy.warnsOnNegotiation(
                ownedInVariant = current.exemplars.size,
                status = exemplar.status,
                hasActiveNegotiation = false,
            )
        if (warns) {
            sheet.update {
                it.copy(pendingNegotiationId = exemplarId, uniqueWarning = true)
            }
        } else {
            sheet.update { it.copy(openNegotiationId = exemplarId) }
        }
    }

    /** A tela já navegou: limpa o pedido para não reabrir na próxima composição. */
    public fun onNegotiationOpened() = sheet.update {
        it.copy(openNegotiationId = null, pendingNegotiationId = null, uniqueWarning = false)
    }

    private fun persist(exemplarId: String, pending: PendingOffer) {
        sheet.update { it.copy(saving = true) }
        viewModelScope.launch {
            duplicates.setOffer(
                exemplarId = exemplarId,
                availability = pending.availability,
                condition = pending.condition,
                askingPrice = pending.askingPrice,
                notes = pending.notes,
            ).fold(
                onSuccess = {
                    sheet.update {
                        it.copy(
                            saving = false,
                            selectedId = null,
                            sheetOpen = false,
                            pendingOffer = null,
                            uniqueWarning = false,
                            message = UiMessage(R.string.availability_updated),
                        )
                    }
                },
                onFailure = { error ->
                    // A folha continua aberta com o que foi digitado (FR-512).
                    sheet.update {
                        it.copy(
                            saving = false,
                            pendingOffer = null,
                            uniqueWarning = false,
                            message = error.toUiMessage(R.string.duplicates_error_save),
                        )
                    }
                },
            )
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
