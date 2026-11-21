package br.com.hotwheelscollector.feature.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.hotwheelscollector.R
import br.com.hotwheelscollector.domain.error.DomainError
import br.com.hotwheelscollector.domain.model.Condition
import br.com.hotwheelscollector.domain.model.IsoDate
import br.com.hotwheelscollector.domain.model.Location
import br.com.hotwheelscollector.domain.model.MoneyCents
import br.com.hotwheelscollector.domain.model.PackagingCondition
import br.com.hotwheelscollector.domain.repository.ExemplarRepository
import br.com.hotwheelscollector.domain.repository.LocationRepository
import br.com.hotwheelscollector.domain.repository.NewAcquisition
import br.com.hotwheelscollector.domain.repository.NewExemplar
import br.com.hotwheelscollector.domain.usecase.AddExemplarUseCase
import br.com.hotwheelscollector.navigation.RouteArgs
import br.com.hotwheelscollector.ui.UiMessage
import br.com.hotwheelscollector.ui.toUiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado da tela de cadastro rápido (US1).
 *
 * O `variantId` vem do `SavedStateHandle`, nunca de campo editável: quem chega aqui já
 * escolheu a variante na tela anterior, e o formulário só pergunta o que diferencia
 * **este** exemplar (condição, local, preço, data, observação).
 */
public data class AddExemplarUiState(
    val variantId: String = "",
    val condition: Condition = Condition.SEALED,
    val packagingCondition: PackagingCondition? = null,
    val locationId: String? = null,
    val locations: List<Location> = emptyList(),
    val notes: String = "",
    val priceText: String = "",
    val shippingText: String = "",
    val discountText: String = "",
    val dateText: String = IsoDate.today().formatPtBr(),
    val store: String = "",
    val saving: Boolean = false,
    val message: UiMessage? = null,
    val duplicateNotice: UiMessage? = null,
    val savedExemplarId: String? = null,
)

@HiltViewModel
public class AddExemplarViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val addExemplar: AddExemplarUseCase,
    private val exemplarRepository: ExemplarRepository,
    locationRepository: LocationRepository,
    clock: Clock,
) : ViewModel() {

    private val variantId: String = savedStateHandle[RouteArgs.VARIANT_ID] ?: ""

    private val _state = MutableStateFlow(
        AddExemplarUiState(
            variantId = variantId,
            // Data padrão vem do mesmo relógio injetado que valida a data: tela e regra
            // nunca discordam sobre que dia é hoje.
            dateText = IsoDate.today(clock).formatPtBr(),
            priceText = savedStateHandle[DRAFT_PRICE] ?: "",
            notes = savedStateHandle[DRAFT_NOTES] ?: "",
        ),
    )
    public val state: StateFlow<AddExemplarUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            locationRepository.observeActive().collect { locations ->
                _state.update { it.copy(locations = locations) }
            }
        }
        viewModelScope.launch {
            if (variantId.isNotBlank()) {
                val existing = exemplarRepository.countByVariant(variantId)
                if (existing > 0) {
                    _state.update {
                        it.copy(
                            duplicateNotice = UiMessage(
                                R.string.add_exemplar_duplicate_variant,
                                existing,
                            ),
                        )
                    }
                }
            }
        }
    }

    public fun onConditionChange(condition: Condition) = _state.update { it.copy(condition = condition) }

    public fun onPackagingChange(packaging: PackagingCondition?) =
        _state.update { it.copy(packagingCondition = packaging) }

    public fun onLocationChange(locationId: String?) = _state.update { it.copy(locationId = locationId) }

    public fun onNotesChange(value: String) {
        savedStateHandle[DRAFT_NOTES] = value
        _state.update { it.copy(notes = value) }
    }

    public fun onPriceChange(value: String) {
        savedStateHandle[DRAFT_PRICE] = value
        _state.update { it.copy(priceText = value) }
    }

    public fun onShippingChange(value: String) = _state.update { it.copy(shippingText = value) }

    public fun onDiscountChange(value: String) = _state.update { it.copy(discountText = value) }

    public fun onDateChange(value: String) = _state.update { it.copy(dateText = value) }

    public fun onStoreChange(value: String) = _state.update { it.copy(store = value) }

    public fun onMessageShown() = _state.update { it.copy(message = null) }

    /** Só a moeda digitada vira centavos aqui; texto inválido é recusado com mensagem. */
    public fun onSave() {
        val current = _state.value
        if (current.saving) return

        val price = parseMoney(current.priceText)
        val shipping = parseMoney(current.shippingText)
        val discount = parseMoney(current.discountText)
        if (price.isInvalid || shipping.isInvalid || discount.isInvalid) {
            _state.update { it.copy(message = UiMessage(R.string.add_exemplar_invalid_value)) }
            return
        }
        val date = IsoDate.parsePtBrOrNull(current.dateText)
        if (date == null) {
            _state.update { it.copy(message = UiMessage(R.string.add_exemplar_invalid_date)) }
            return
        }

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = addExemplar(
                NewExemplar(
                    variantId = current.variantId,
                    condition = current.condition,
                    packagingCondition = current.packagingCondition,
                    locationId = current.locationId,
                    notes = current.notes,
                    acquisition = if (price.value == null && current.store.isBlank()) {
                        null
                    } else {
                        NewAcquisition(
                            date = date,
                            price = price.value,
                            shipping = shipping.value,
                            discount = discount.value,
                            store = current.store,
                        )
                    },
                ),
            )

            result.fold(
                onSuccess = { outcome ->
                    val notice = if (outcome.sameVariantCount > 1) {
                        UiMessage(
                            R.string.add_exemplar_success_multiple,
                            outcome.sameVariantCount,
                        )
                    } else {
                        UiMessage(
                            R.string.add_exemplar_success_single,
                            outcome.exemplar.internalCode,
                        )
                    }
                    _state.update {
                        it.copy(saving = false, message = notice, savedExemplarId = outcome.exemplar.id)
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(saving = false, message = error.toMessage())
                    }
                },
            )
        }
    }

    private fun parseMoney(text: String): ParsedMoney {
        if (text.isBlank()) return ParsedMoney(null, isInvalid = false)
        val value = MoneyCents.parsePtBrOrNull(text)
        return ParsedMoney(value, isInvalid = value == null)
    }

    private fun Throwable.toMessage(): UiMessage = when (this) {
        DomainError.Duplicate -> UiMessage(R.string.error_duplicate_exemplar)
        else -> toUiMessage(R.string.error_generic_save)
    }

    private data class ParsedMoney(val value: MoneyCents?, val isInvalid: Boolean)

    private companion object {
        const val DRAFT_PRICE = "draft_price"
        const val DRAFT_NOTES = "draft_notes"
    }
}
