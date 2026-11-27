package br.com.diecastcollector.feature.catalog

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.diecastcollector.R
import br.com.diecastcollector.domain.model.Acquisition
import br.com.diecastcollector.domain.model.Condition
import br.com.diecastcollector.domain.model.DuplicateAvailability
import br.com.diecastcollector.domain.model.ExemplarStatus
import br.com.diecastcollector.domain.model.IsoDate
import br.com.diecastcollector.domain.model.Location
import br.com.diecastcollector.domain.model.LocationType
import br.com.diecastcollector.domain.model.MoneyCents
import br.com.diecastcollector.domain.model.Movement
import br.com.diecastcollector.domain.model.PackagingCondition
import br.com.diecastcollector.domain.model.Photo
import br.com.diecastcollector.domain.model.PhotoAngle
import br.com.diecastcollector.domain.model.PhotoEdit
import br.com.diecastcollector.domain.model.Valuation
import br.com.diecastcollector.domain.repository.ExemplarDependents
import br.com.diecastcollector.feature.duplicates.ExemplarOfferSheet
import br.com.diecastcollector.feature.duplicates.OfferSheetSubject
import br.com.diecastcollector.feature.duplicates.UniqueExemplarDialog
import br.com.diecastcollector.feature.duplicates.labelRes
import br.com.diecastcollector.ui.components.AdaptiveFieldPair
import br.com.diecastcollector.ui.components.FlowChipGroup
import br.com.diecastcollector.ui.theme.DiecastTheme
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

/**
 * Edição de um exemplar físico (FR-028, FR-091): condição, embalagem, observações,
 * aquisição, avaliações, movimentação com histórico e a escolha entre arquivar e excluir.
 *
 * A exclusão física só é oferecida quando [ExemplarDependents] está vazio; com qualquer
 * vínculo, a tela explica o bloqueio e mantém o arquivamento como caminho (Art. I.6–I.7).
 * Valores estimados aparecem sempre como estimativa do colecionador, nunca como preço de
 * mercado (FR-086).
 */
@Composable
public fun ExemplarDetailScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenNegotiation: (exemplarId: String) -> Unit = {},
    viewModel: ExemplarDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Quem navega é a tela; o ViewModel decide se o aviso vem antes (FR-513, FR-515b).
    LaunchedEffect(state.negotiationRequested) {
        if (state.negotiationRequested) {
            onOpenNegotiation(state.exemplarId)
            viewModel.onNegotiationOpened()
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val permissionDeniedMessage = stringResource(R.string.photo_permission_denied)

    val messageText = state.message?.text()

    LaunchedEffect(state.message) {
        messageText?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onMessageShown()
        }
    }
    // Arquivou ou excluiu: volta para a lista, que já reflete o novo estado.
    LaunchedEffect(state.finished) {
        if (state.finished) onDone()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        ExemplarDetailContent(
            state = state,
            contentPadding = padding,
            onConditionChange = viewModel::onConditionChange,
            onPackagingChange = viewModel::onPackagingChange,
            onNotesChange = viewModel::onNotesChange,
            onTargetLocationChange = viewModel::onTargetLocationChange,
            onMoveNoteChange = viewModel::onMoveNoteChange,
            onSaveDetails = viewModel::onSaveDetails,
            onMove = viewModel::onMove,
            onArchive = viewModel::onArchive,
            onDelete = viewModel::onDelete,
            onAcquisitionPriceChange = viewModel::onAcquisitionPriceChange,
            onAcquisitionShippingChange = viewModel::onAcquisitionShippingChange,
            onAcquisitionDiscountChange = viewModel::onAcquisitionDiscountChange,
            onAcquisitionDateChange = viewModel::onAcquisitionDateChange,
            onAcquisitionStoreChange = viewModel::onAcquisitionStoreChange,
            onAcquisitionSellerChange = viewModel::onAcquisitionSellerChange,
            onAcquisitionMethodChange = viewModel::onAcquisitionMethodChange,
            onAcquisitionNotesChange = viewModel::onAcquisitionNotesChange,
            onSaveAcquisition = viewModel::onSaveAcquisition,
            onAddPhoto = viewModel::onAddPhoto,
            onSetPrimaryPhoto = viewModel::onSetPrimaryPhoto,
            onRemovePhoto = viewModel::onRemovePhoto,
            onSavePhotoEdit = viewModel::onSavePhotoEdit,
            onRestorePhotoOriginal = viewModel::onRestorePhotoOriginal,
            onPhotoPermissionDenied = {
                scope.launch { snackbarHostState.showSnackbar(permissionDeniedMessage) }
            },
            onValuationValueChange = viewModel::onValuationValueChange,
            onValuationDateChange = viewModel::onValuationDateChange,
            onValuationSourceChange = viewModel::onValuationSourceChange,
            onValuationNotesChange = viewModel::onValuationNotesChange,
            onAddValuation = viewModel::onAddValuation,
            onOpenOfferSheet = viewModel::onOpenOfferSheet,
            onDismissOfferSheet = viewModel::onDismissOfferSheet,
            onSaveOffer = viewModel::onSaveOffer,
            onContinueUniqueWarning = viewModel::onContinueUniqueWarning,
            onCancelUniqueWarning = viewModel::onCancelUniqueWarning,
            onNegotiate = viewModel::onNegotiate,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExemplarDetailContent(
    state: ExemplarDetailUiState,
    contentPadding: PaddingValues,
    onConditionChange: (Condition) -> Unit,
    onPackagingChange: (PackagingCondition?) -> Unit,
    onNotesChange: (String) -> Unit,
    onTargetLocationChange: (String?) -> Unit,
    onMoveNoteChange: (String) -> Unit,
    onSaveDetails: () -> Unit,
    onMove: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onAcquisitionPriceChange: (String) -> Unit = {},
    onAcquisitionShippingChange: (String) -> Unit = {},
    onAcquisitionDiscountChange: (String) -> Unit = {},
    onAcquisitionDateChange: (String) -> Unit = {},
    onAcquisitionStoreChange: (String) -> Unit = {},
    onAcquisitionSellerChange: (String) -> Unit = {},
    onAcquisitionMethodChange: (String) -> Unit = {},
    onAcquisitionNotesChange: (String) -> Unit = {},
    onSaveAcquisition: () -> Unit = {},
    onAddPhoto: (ByteArray, PhotoAngle) -> Unit = { _, _ -> },
    onSetPrimaryPhoto: (String) -> Unit = {},
    onRemovePhoto: (String) -> Unit = {},
    onSavePhotoEdit: (photoId: String, bytes: ByteArray, edit: PhotoEdit) -> Unit = { _, _, _ -> },
    onRestorePhotoOriginal: (String) -> Unit = {},
    onPhotoPermissionDenied: () -> Unit = {},
    onValuationValueChange: (String) -> Unit = {},
    onValuationDateChange: (String) -> Unit = {},
    onValuationSourceChange: (String) -> Unit = {},
    onValuationNotesChange: (String) -> Unit = {},
    onAddValuation: () -> Unit = {},
    onOpenOfferSheet: () -> Unit = {},
    onDismissOfferSheet: () -> Unit = {},
    onSaveOffer: (DuplicateAvailability, Condition?, String, String) -> Unit = { _, _, _, _ -> },
    onContinueUniqueWarning: () -> Unit = {},
    onCancelUniqueWarning: () -> Unit = {},
    onNegotiate: () -> Unit = {},
) {
    var showArchiveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var photoToRemove by remember { mutableStateOf<String?>(null) }
    var photoBeingEdited by remember { mutableStateOf<Photo?>(null) }
    val context = LocalContext.current

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            state.loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.notFound -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.exemplar_not_found),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = state.internalCode,
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (state.createdAt != null && state.updatedAt != null) {
                    Text(
                        text = stringResource(
                            R.string.exemplar_dates,
                            MOVEMENT_FORMAT.format(state.createdAt),
                            MOVEMENT_FORMAT.format(state.updatedAt),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.isArchived) {
                    Text(
                        text = stringResource(R.string.exemplar_archived_badge),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Text(
                    text = stringResource(R.string.exemplar_condition),
                    style = MaterialTheme.typography.labelLarge,
                )
                // Cinco condições num grupo que quebra linha (T326, FR-313).
                FlowChipGroup {
                    Condition.entries.forEach { condition ->
                        FilterChip(
                            selected = state.condition == condition,
                            onClick = { onConditionChange(condition) },
                            label = { Text(stringResource(condition.labelRes())) },
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.exemplar_packaging),
                    style = MaterialTheme.typography.labelLarge,
                )
                // Embalagem incluindo «não informado» no mesmo grupo: em 360 dp com
                // fonte ampliada o chip de «não informado» sumia da borda (T326).
                FlowChipGroup {
                    PackagingCondition.entries.forEach { packaging ->
                        FilterChip(
                            selected = state.packagingCondition == packaging,
                            onClick = { onPackagingChange(packaging) },
                            label = { Text(stringResource(packaging.labelRes())) },
                        )
                    }
                    FilterChip(
                        selected = state.packagingCondition == null,
                        onClick = { onPackagingChange(null) },
                        label = { Text(stringResource(R.string.packaging_unknown)) },
                    )
                }

                OutlinedTextField(
                    value = state.notes,
                    onValueChange = onNotesChange,
                    label = { Text(stringResource(R.string.exemplar_notes)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = onSaveDetails,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.exemplar_save))
                }

                // As fotos do cadastro ficam gravadas no exemplar: é aqui, na ficha dele,
                // que o colecionador revê, troca a principal e acrescenta novos ângulos.
                PhotoGrid(
                    photos = state.photos.map { photo ->
                        PhotoTile(
                            id = photo.id,
                            angle = photo.angle,
                            isPrimary = photo.isPrimary,
                            source = File(context.filesDir, photo.path),
                            isEdited = photo.isEdited,
                        )
                    },
                    onAddPhotoBytes = onAddPhoto,
                    onSetPrimary = onSetPrimaryPhoto,
                    onRemovePhoto = { photoId -> photoToRemove = photoId },
                    onPermissionDenied = onPhotoPermissionDenied,
                    onEditPhoto = { tile ->
                        photoBeingEdited = state.photos.firstOrNull { it.id == tile.id }
                    },
                )

                photoBeingEdited?.let { photo ->
                    PhotoEditDialog(
                        photo = photo,
                        onDismiss = { photoBeingEdited = null },
                        onSave = { bytes, edit ->
                            photoBeingEdited = null
                            onSavePhotoEdit(photo.id, bytes, edit)
                        },
                        onRestoreOriginal = {
                            photoBeingEdited = null
                            onRestorePhotoOriginal(photo.id)
                        },
                    )
                }

                HorizontalDivider()

                // ---- Disponibilidade e negociação ---------------------------------
                Text(
                    text = stringResource(R.string.exemplar_negotiation_section),
                    style = MaterialTheme.typography.titleMedium,
                )
                val availability = state.offer?.availability
                when {
                    state.activeNegotiation != null -> Text(
                        text = stringResource(R.string.duplicates_reserved),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    availability != null -> Text(
                        text = stringResource(
                            R.string.exemplar_negotiation_offer_now,
                            stringResource(availability.labelRes()),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (state.status != ExemplarStatus.OWNED) {
                    Text(
                        text = stringResource(R.string.exemplar_negotiation_blocked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = onOpenOfferSheet,
                    enabled = state.status == ExemplarStatus.OWNED && !state.savingOffer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.duplicates_offer_action))
                }
                OutlinedButton(
                    onClick = onNegotiate,
                    enabled = state.status == ExemplarStatus.OWNED,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.duplicates_negotiate_action))
                }

                HorizontalDivider()

                // ---- Aquisição ----------------------------------------------------
                Text(
                    text = stringResource(R.string.acquisition_section),
                    style = MaterialTheme.typography.titleMedium,
                )
                val totalCost = state.acquisition?.totalCost()
                Text(
                    text = when {
                        state.acquisition == null -> stringResource(R.string.acquisition_none)
                        totalCost == null -> stringResource(R.string.acquisition_without_value)
                        else -> stringResource(R.string.acquisition_total, totalCost.formatPtBr())
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                AdaptiveFieldPair(
                    first = { fieldModifier ->
                        OutlinedTextField(
                            value = state.acquisitionPrice,
                            onValueChange = onAcquisitionPriceChange,
                            label = { Text(stringResource(R.string.add_exemplar_price)) },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                            ),
                            modifier = fieldModifier,
                        )
                    },
                    second = { fieldModifier ->
                        OutlinedTextField(
                            value = state.acquisitionShipping,
                            onValueChange = onAcquisitionShippingChange,
                            label = { Text(stringResource(R.string.add_exemplar_shipping)) },
                            singleLine = true,
                            modifier = fieldModifier,
                        )
                    },
                )
                AdaptiveFieldPair(
                    first = { fieldModifier ->
                        OutlinedTextField(
                            value = state.acquisitionDiscount,
                            onValueChange = onAcquisitionDiscountChange,
                            label = { Text(stringResource(R.string.add_exemplar_discount)) },
                            singleLine = true,
                            modifier = fieldModifier,
                        )
                    },
                    second = { fieldModifier ->
                        OutlinedTextField(
                            value = state.acquisitionDate,
                            onValueChange = onAcquisitionDateChange,
                            label = { Text(stringResource(R.string.add_exemplar_date)) },
                            singleLine = true,
                            modifier = fieldModifier,
                        )
                    },
                )
                AdaptiveFieldPair(
                    first = { fieldModifier ->
                        OutlinedTextField(
                            value = state.acquisitionStore,
                            onValueChange = onAcquisitionStoreChange,
                            label = { Text(stringResource(R.string.add_exemplar_store)) },
                            singleLine = true,
                            modifier = fieldModifier,
                        )
                    },
                    second = { fieldModifier ->
                        OutlinedTextField(
                            value = state.acquisitionSeller,
                            onValueChange = onAcquisitionSellerChange,
                            label = { Text(stringResource(R.string.acquisition_seller)) },
                            singleLine = true,
                            modifier = fieldModifier,
                        )
                    },
                )
                OutlinedTextField(
                    value = state.acquisitionMethod,
                    onValueChange = onAcquisitionMethodChange,
                    label = { Text(stringResource(R.string.acquisition_method)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.acquisitionNotes,
                    onValueChange = onAcquisitionNotesChange,
                    label = { Text(stringResource(R.string.exemplar_notes)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = onSaveAcquisition,
                    enabled = !state.savingAcquisition,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.acquisition_save))
                }

                HorizontalDivider()

                // ---- Avaliações ---------------------------------------------------
                Text(
                    text = stringResource(R.string.valuation_section),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.valuation_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val currentValuation = state.currentValuation
                if (currentValuation == null) {
                    Text(
                        text = stringResource(R.string.valuation_none),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        text = stringResource(
                            R.string.valuation_current_value,
                            currentValuation.value.formatPtBr(),
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = listOfNotNull(
                            stringResource(R.string.valuation_on_date, currentValuation.date.formatPtBr()),
                            currentValuation.source,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.valuations.size > 1) {
                    state.valuations.drop(1).forEach { valuation ->
                        Text(
                            text = listOfNotNull(
                                valuation.date.formatPtBr(),
                                "R$ ${valuation.value.formatPtBr()}",
                                valuation.source,
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                AdaptiveFieldPair(
                    first = { fieldModifier ->
                        OutlinedTextField(
                            value = state.valuationValue,
                            onValueChange = onValuationValueChange,
                            label = { Text(stringResource(R.string.valuation_value)) },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                            ),
                            modifier = fieldModifier,
                        )
                    },
                    second = { fieldModifier ->
                        OutlinedTextField(
                            value = state.valuationDate,
                            onValueChange = onValuationDateChange,
                            label = { Text(stringResource(R.string.valuation_date)) },
                            singleLine = true,
                            modifier = fieldModifier,
                        )
                    },
                )
                OutlinedTextField(
                    value = state.valuationSource,
                    onValueChange = onValuationSourceChange,
                    label = { Text(stringResource(R.string.valuation_source)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.valuationNotes,
                    onValueChange = onValuationNotesChange,
                    label = { Text(stringResource(R.string.valuation_notes)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = onAddValuation,
                    enabled = !state.savingValuation,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.valuation_add))
                }

                HorizontalDivider()

                Text(
                    text = stringResource(R.string.exemplar_location),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = locationLabel(state.currentLocationId, state.locations),
                    style = MaterialTheme.typography.bodyMedium,
                )

                var locationMenuOpen by remember { mutableStateOf(false) }
                val targetName = state.targetLocationId?.let { id ->
                    state.locations.firstOrNull { it.id == id }?.name
                } ?: stringResource(R.string.add_exemplar_no_location)
                ExposedDropdownMenuBox(
                    expanded = locationMenuOpen,
                    onExpandedChange = { locationMenuOpen = !locationMenuOpen },
                ) {
                    OutlinedTextField(
                        value = targetName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.exemplar_move_target)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationMenuOpen)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = locationMenuOpen,
                        onDismissRequest = { locationMenuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_exemplar_no_location)) },
                            onClick = {
                                onTargetLocationChange(null)
                                locationMenuOpen = false
                            },
                        )
                        state.locations.forEach { location ->
                            DropdownMenuItem(
                                text = { Text(location.name) },
                                onClick = {
                                    onTargetLocationChange(location.id)
                                    locationMenuOpen = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = state.moveNote,
                    onValueChange = onMoveNoteChange,
                    label = { Text(stringResource(R.string.exemplar_move_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = onMove,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.exemplar_move))
                }

                Text(
                    text = stringResource(R.string.exemplar_history_title),
                    style = MaterialTheme.typography.labelLarge,
                )
                if (state.movements.isEmpty()) {
                    Text(
                        text = stringResource(R.string.exemplar_history_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.movements.forEach { movement ->
                        MovementRow(movement = movement, locations = state.locations)
                    }
                }

                HorizontalDivider()

                OutlinedButton(
                    onClick = { showArchiveDialog = true },
                    enabled = !state.saving && !state.isArchived,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.exemplar_archive))
                }
                TextButton(
                    onClick = { showDeleteDialog = true },
                    enabled = !state.saving && state.canDelete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.exemplar_delete),
                        color = if (state.canDelete) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                if (!state.canDelete) {
                    Text(
                        text = stringResource(R.string.exemplar_delete_blocked_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (state.sheetOpen) {
        ExemplarOfferSheet(
            subject = OfferSheetSubject(
                internalCode = state.internalCode,
                title = "",
                condition = state.condition,
                offer = state.offer,
            ),
            saving = state.savingOffer,
            onDismiss = onDismissOfferSheet,
            onSave = onSaveOffer,
        )
    }
    if (state.uniqueWarning) {
        UniqueExemplarDialog(
            onContinue = onContinueUniqueWarning,
            onCancel = onCancelUniqueWarning,
        )
    }
    if (showArchiveDialog) {
        ArchiveExemplarDialog(
            onConfirm = {
                showArchiveDialog = false
                onArchive()
            },
            onDismiss = { showArchiveDialog = false },
        )
    }
    if (showDeleteDialog) {
        DeleteExemplarDialog(
            dependents = state.dependents,
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onArchive = {
                showDeleteDialog = false
                onArchive()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
    photoToRemove?.let { photoId ->
        AlertDialog(
            onDismissRequest = { photoToRemove = null },
            title = { Text(stringResource(R.string.photo_remove_title)) },
            // Aqui a foto já está gravada: remover apaga o registro e o arquivo.
            text = { Text(stringResource(R.string.photo_remove_saved_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        photoToRemove = null
                        onRemovePhoto(photoId)
                    },
                ) { Text(stringResource(R.string.photo_remove_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { photoToRemove = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun MovementRow(movement: Movement, locations: List<Location>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = MOVEMENT_FORMAT.format(movement.at),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(
                R.string.exemplar_history_entry,
                locationLabel(movement.fromLocationId, locations),
                locationLabel(movement.toLocationId, locations),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        movement.note?.takeIf { it.isNotBlank() }?.let { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun locationLabel(id: String?, locations: List<Location>): String =
    id?.let { locationId -> locations.firstOrNull { it.id == locationId }?.name }
        ?: stringResource(R.string.add_exemplar_no_location)

/** Rótulo pt-BR da embalagem; o texto mora em `strings.xml` (T131, Art. IV). */
@StringRes
internal fun PackagingCondition.labelRes(): Int = when (this) {
    PackagingCondition.SEALED -> R.string.packaging_sealed
    PackagingCondition.OPENED -> R.string.packaging_opened
    PackagingCondition.DAMAGED -> R.string.packaging_damaged
    PackagingCondition.NONE -> R.string.packaging_none
}

private val MOVEMENT_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())

@Preview(showBackground = true)
@Composable
private fun ExemplarDetailPreview() {
    val now = Instant.parse("2026-11-18T12:00:00Z")
    val location = Location(
        id = "loc1",
        type = LocationType.RACK,
        name = "Estante da sala",
        createdAt = now,
        updatedAt = now,
    )
    DiecastTheme {
        ExemplarDetailContent(
            state = ExemplarDetailUiState(
                exemplarId = "e1",
                internalCode = "HW-000001",
                status = ExemplarStatus.OWNED,
                condition = Condition.SEALED,
                packagingCondition = PackagingCondition.SEALED,
                notes = "Caixa com marca de estoque",
                locations = listOf(location),
                currentLocationId = "loc1",
                targetLocationId = "loc1",
                movements = listOf(
                    Movement(
                        id = "mv1",
                        exemplarId = "e1",
                        at = now,
                        fromLocationId = null,
                        toLocationId = "loc1",
                        note = "Cadastro inicial",
                    ),
                ),
                dependents = ExemplarDependents(photos = 2, acquisitions = 1, movements = 1),
                acquisition = Acquisition(
                    id = "a1",
                    exemplarId = "e1",
                    date = IsoDate(LocalDate.of(2026, 11, 2)),
                    price = MoneyCents(5900),
                    shipping = MoneyCents(1200),
                    store = "Loja do shopping",
                    createdAt = now,
                    updatedAt = now,
                ),
                valuations = listOf(
                    Valuation(
                        id = "val1",
                        exemplarId = "e1",
                        date = IsoDate(LocalDate.of(2026, 11, 10)),
                        value = MoneyCents(8500),
                        source = "Colecionador",
                        createdAt = now,
                    ),
                ),
                loading = false,
            ),
            contentPadding = PaddingValues(0.dp),
            onConditionChange = {},
            onPackagingChange = {},
            onNotesChange = {},
            onTargetLocationChange = {},
            onMoveNoteChange = {},
            onSaveDetails = {},
            onMove = {},
            onArchive = {},
            onDelete = {},
        )
    }
}
