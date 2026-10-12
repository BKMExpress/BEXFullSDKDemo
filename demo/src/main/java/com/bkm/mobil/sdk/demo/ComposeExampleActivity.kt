package com.bkm.mobil.sdk.demo

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bkm.mobil.sdk.api.BexEnvironment
import com.bkm.mobil.sdk.api.BexFullSdk
import com.bkm.mobil.sdk.api.BexFullSdkConfig
import com.bkm.mobil.sdk.api.BexFullSdkTheme
import com.bkm.mobil.sdk.api.BexSdkError
import com.bkm.mobil.sdk.api.CardDeletionResult
import com.bkm.mobil.sdk.api.CardSelectionResult
import com.bkm.mobil.sdk.api.PaymentCallback
import com.bkm.mobil.sdk.api.PaymentResult
import com.bkm.mobil.sdk.api.PaymentSecurity
import com.bkm.mobil.sdk.api.SdkMode
import com.bkm.mobil.sdk.api.TransactionType
import com.bkm.mobil.sdk.demo.ui.theme.BEXSDKTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class ComposeExampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BEXSDKTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    DemoApp(
                        modifier = Modifier.systemBarsPadding(),
                        onError = ::handleError
                    )
                }
            }
        }
    }

    fun handleError(error: BexSdkError) {
        Toast.makeText(this, error.displayMessage, Toast.LENGTH_SHORT).show()
    }
}

enum class DirectSdkAction {
    StartPayment,
    CardSelect
}

@Composable
fun DemoApp(
    modifier: Modifier = Modifier,
    onError: (BexSdkError) -> Unit
) {
    DemoScreen(
        modifier = modifier,
        onError = onError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoScreen(
    modifier: Modifier = Modifier,
    onError: (BexSdkError) -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()

    var theme by remember { mutableStateOf(DemoTheme.Default) }
    var uxMode by remember { mutableStateOf(DemoUxMode.FullScreen) }
    val formStateState = remember { mutableStateOf(DemoFormState()) }
    val defaultsState = remember { mutableStateOf<DemoFormState?>(null) }
    var selectedCard by remember { mutableStateOf<CardSelectionResult?>(null) }
    var selectedDirectAction by remember {
        mutableStateOf<DirectSdkAction?>(DirectSdkAction.StartPayment)
    }
    var menuExpanded by remember { mutableStateOf(false) }
    var showSetDefaultsDialog by remember { mutableStateOf(false) }
    var showEnvironmentDialog by remember { mutableStateOf(false) }
    var showSaveDefaultsMissingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val loaded = context.loadDefaults()
        defaultsState.value = loaded
        formStateState.value = loaded
    }

    val defaults = defaultsState.value ?: DemoFormState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("BEX FULL SDK Demo") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { formStateState.value = defaults }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset to defaults"
                        )
                    }
                    IconButton(
                        onClick = {
                            val current = formStateState.value
                            if (current.token.isBlank() ||
                                current.merchantId.isBlank() ||
                                current.gsmNo.isBlank() ||
                                current.merchantUserId.isBlank()
                            ) {
                                showSaveDefaultsMissingDialog = true
                                return@IconButton
                            }
                            scope.launch {
                                context.saveDefaults(current)
                                Toast.makeText(
                                    context,
                                    "Varsayılanlar kaydedildi",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Set as default"
                        )
                    }
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Set Defaults") },
                                onClick = {
                                    menuExpanded = false
                                    showSetDefaultsDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Environment") },
                                onClick = {
                                    menuExpanded = false
                                    showEnvironmentDialog = true
                                }
                            )
                        }
                    }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                DemoCollapsibleGroup(
                    title = "SDK Konfigürasyonu",
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    DemoChoiceGrid {
                        DemoChoicePicker(
                            title = "Tema",
                            options = DemoTheme.entries.map { DemoChoice(it, it.name) },
                            selected = theme,
                            onSelect = { theme = it },
                            modifier = Modifier.weight(1f)
                        )
                        DemoChoicePicker(
                            title = "UX Modu",
                            options = listOf(
                                DemoChoice(DemoUxMode.FullScreen, "Full Screen"),
                                DemoChoice(DemoUxMode.BottomSheet, "Bottom Sheet")
                            ),
                            selected = uxMode,
                            onSelect = { uxMode = it },
                            modifier = Modifier.weight(1f)
                        )
                        DemoChoicePicker(
                            title = "SDK Modu",
                            options = listOf(
                                DemoChoice(DirectSdkAction.StartPayment, "Ödeme"),
                                DemoChoice(DirectSdkAction.CardSelect, "Kart Seçimi")
                            ),
                            selected = selectedDirectAction ?: DirectSdkAction.StartPayment,
                            onSelect = { selectedDirectAction = it },
                            modifier = Modifier.weight(1f)
                        )
                        DemoChoicePicker(
                            title = "Ödeme Türü",
                            options = listOf(
                                DemoChoice(PaymentSecurity.TDS, "3DS (TDS)"),
                                DemoChoice(PaymentSecurity.OTP, "OTP"),
                                DemoChoice(PaymentSecurity.NONE, "Non-OTP")
                            ),
                            selected = formStateState.value.paymentSecurity,
                            onSelect = {
                                formStateState.value =
                                    formStateState.value.copy(paymentSecurity = it)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = formStateState.value.troySonicSoundEnabled,
                            onCheckedChange = {
                                formStateState.value =
                                    formStateState.value.copy(troySonicSoundEnabled = it)
                            }
                        )
                        Text(
                            text = "Troy Sonic Sound",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = formStateState.value.showExitConfirmation,
                            onCheckedChange = {
                                formStateState.value =
                                    formStateState.value.copy(showExitConfirmation = it)
                            }
                        )
                        Text(
                            text = "Çıkış onay pop-up’ı",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                DemoCollapsibleGroup(
                    title = "Token",
                    initiallyExpanded = false,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    OutlinedTextField(
                        value = formStateState.value.token,
                        onValueChange = {
                            formStateState.value = formStateState.value.copy(token = it)
                        },
                        label = { Text("Token") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        trailingIcon = {
                            if (formStateState.value.token.isNotBlank()) {
                                IconButton(onClick = {
                                    formStateState.value = formStateState.value.copy(token = "")
                                }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    )
                }

                DemoCollapsibleGroup(
                    title = "Kullanıcı Bilgileri",
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    OutlinedTextField(
                        value = formStateState.value.merchantId,
                        onValueChange = {
                            formStateState.value = formStateState.value.copy(merchantId = it)
                        },
                        label = { Text("Merchant ID") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (formStateState.value.merchantId.isNotBlank()) {
                                IconButton(onClick = {
                                    formStateState.value =
                                        formStateState.value.copy(merchantId = "")
                                }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = formStateState.value.gsmNo,
                        onValueChange = {
                            formStateState.value = formStateState.value.copy(gsmNo = it)
                        },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            if (formStateState.value.gsmNo.isNotBlank()) {
                                IconButton(onClick = {
                                    formStateState.value =
                                        formStateState.value.copy(gsmNo = "")
                                }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = formStateState.value.merchantUserId,
                        onValueChange = {
                            formStateState.value = formStateState.value.copy(merchantUserId = it)
                        },
                        label = { Text("Merchant User ID") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (formStateState.value.merchantUserId.isNotBlank()) {
                                IconButton(onClick = {
                                    formStateState.value =
                                        formStateState.value.copy(merchantUserId = "")
                                }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    )
                }

                DemoCollapsibleGroup(
                    title = "Ödeme Bilgileri",
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = formStateState.value.paymentAmount,
                                onValueChange = {
                                    formStateState.value =
                                        formStateState.value.copy(paymentAmount = it)
                                },
                                label = { Text("Payment Amount") },
                                modifier = Modifier.weight(0.75f),
                                trailingIcon = {
                                    if (formStateState.value.paymentAmount.isNotBlank()) {
                                        IconButton(onClick = {
                                            formStateState.value =
                                                formStateState.value.copy(paymentAmount = "")
                                        }) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            )
                            OutlinedTextField(
                                value = formStateState.value.paymentCurrency,
                                onValueChange = {
                                    formStateState.value =
                                        formStateState.value.copy(paymentCurrency = it)
                                },
                                label = { Text("Currency") },
                                modifier = Modifier.weight(0.25f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = formStateState.value.orderId,
                            onValueChange = {
                                formStateState.value = formStateState.value.copy(orderId = it)
                            },
                            label = { Text("Order ID") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (formStateState.value.orderId.isNotBlank()) {
                                    IconButton(onClick = {
                                        formStateState.value =
                                            formStateState.value.copy(orderId = "")
                                    }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = formStateState.value.installmentCount,
                            onValueChange = {
                                formStateState.value =
                                    formStateState.value.copy(installmentCount = it)
                            },
                            label = { Text("Taksit Sayısı") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = {
                                if (formStateState.value.installmentCount.isNotBlank()) {
                                    IconButton(onClick = {
                                        formStateState.value =
                                            formStateState.value.copy(installmentCount = "")
                                    }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        DemoChoicePicker(
                            title = "İşlem Türü",
                            options = listOf(
                                DemoChoice(TransactionType.SALE, "Satış (SALE)"),
                                DemoChoice(TransactionType.PRE_AUTH, "Ön Provizyon (PRE_AUTH)"),
                                DemoChoice(TransactionType.RECURRING, "Tekrarlayan (RECURRING)")
                            ),
                            selected = formStateState.value.transactionType,
                            onSelect = {
                                formStateState.value =
                                    formStateState.value.copy(transactionType = it)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = formStateState.value.successUrl,
                            onValueChange = {
                                formStateState.value =
                                    formStateState.value.copy(successUrl = it)
                            },
                            label = { Text("Success URL") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (formStateState.value.successUrl.isNotBlank()) {
                                    IconButton(onClick = {
                                        formStateState.value =
                                            formStateState.value.copy(successUrl = "")
                                    }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = formStateState.value.failUrl,
                            onValueChange = {
                                formStateState.value = formStateState.value.copy(failUrl = it)
                            },
                            label = { Text("Error URL") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (formStateState.value.failUrl.isNotBlank()) {
                                    IconButton(onClick = {
                                        formStateState.value =
                                            formStateState.value.copy(failUrl = "")
                                    }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                selectedCard?.let { card ->
                    val selected = card.selectedCard
                    val placeholder =
                        painterResource(com.bkm.mobil.sdk.R.drawable.img_card_placeholder)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Selected Card",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = selected.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .width(128.dp)
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop,
                                    placeholder = placeholder,
                                    error = placeholder,
                                    fallback = placeholder
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selected.cardAlias
                                            ?: selected.bexBankInformation.bankShortName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${selected.bexBankInformation.cardBrand} • ${selected.maskCardNumber}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Bank Code: ${selected.bexBankInformation.bankCode}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (showSaveDefaultsMissingDialog) {
                AlertDialog(
                    onDismissRequest = { showSaveDefaultsMissingDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showSaveDefaultsMissingDialog = false }) {
                            Text("Tamam")
                        }
                    },
                    title = { Text("Eksik Bilgi") },
                    text = {
                        Text("Varsayılanları kaydetmek için tüm zorunlu alanları doldurun.")
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val action = selectedDirectAction
                            if (action == null) {
                                Toast.makeText(
                                    context,
                                    "Select an action first",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }
                            val params = formStateState.value.toInitParams()
                            if (params == null) {
                                Toast.makeText(
                                    context,
                                    "Please fill all init fields and valid amount",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }
                            val paramsWithTransactionId = params.copy(
                                transactionId = UUID.randomUUID().toString()
                            )
                            val mode = when (action) {
                                DirectSdkAction.StartPayment -> SdkMode.PAYMENT
                                DirectSdkAction.CardSelect -> SdkMode.CARD_SELECTION_ONLY
                            }
                            initPaymentSdk(
                                context = context,
                                initParams = paramsWithTransactionId,
                                theme = theme.toPaymentSdkTheme(),
                                mode = mode,
                                troySonicSoundEnabled = formStateState.value.troySonicSoundEnabled,
                                showExitConfirmation = formStateState.value.showExitConfirmation,
                                onError = onError,
                                onCardSelected = { selectedCard = it }
                            )
                            when (uxMode) {
                                DemoUxMode.FullScreen -> BexFullSdk.start()
                                DemoUxMode.BottomSheet -> activity?.let {
                                    BexFullSdk.showAsBottomSheet(it)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SDK Başlat")
                }
            }
        }
    }

    if (showSetDefaultsDialog) {
        SetDefaultsDialog(
            currentDefaults = defaults,
            onDismiss = { showSetDefaultsDialog = false },
            onSave = { newDefaults ->
                scope.launch {
                    context.saveDefaults(newDefaults)
                    defaultsState.value = newDefaults
                    showSetDefaultsDialog = false
                }
            }
        )
    }

    if (showEnvironmentDialog) {
        EnvironmentChooserDialog(
            current = formStateState.value.environment,
            onDismiss = { showEnvironmentDialog = false },
            onSelect = { env ->
                scope.launch {
                    val updated = formStateState.value.copy(environment = env)
                    formStateState.value = updated
                    context.saveDefaults(updated)
                    showEnvironmentDialog = false
                }
            }
        )
    }
}

@Composable
private fun DemoCollapsibleGroup(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = true,
    titleBadge: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { expanded = !expanded }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (titleBadge != null) {
                        Text(
                            text = titleBadge,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (expanded) "Daralt" else "Genişlet"
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(bottom = 8.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun SetDefaultsDialog(
    currentDefaults: DemoFormState,
    onDismiss: () -> Unit,
    onSave: (DemoFormState) -> Unit
) {
    var token by remember(currentDefaults) { mutableStateOf(currentDefaults.token) }
    var merchantId by remember(currentDefaults) { mutableStateOf(currentDefaults.merchantId) }
    var gsmNo by remember(currentDefaults) { mutableStateOf(currentDefaults.gsmNo) }
    var merchantUserId by remember(currentDefaults) {
        mutableStateOf(currentDefaults.merchantUserId)
    }
    var paymentAmount by remember(currentDefaults) {
        mutableStateOf(currentDefaults.paymentAmount)
    }
    var paymentCurrency by remember(currentDefaults) {
        mutableStateOf(currentDefaults.paymentCurrency)
    }
    var successUrl by remember(currentDefaults) { mutableStateOf(currentDefaults.successUrl) }
    var failUrl by remember(currentDefaults) { mutableStateOf(currentDefaults.failUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Defaults") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Token") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                OutlinedTextField(
                    value = merchantId,
                    onValueChange = { merchantId = it },
                    label = { Text("Merchant ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = gsmNo,
                    onValueChange = { gsmNo = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = merchantUserId,
                    onValueChange = { merchantUserId = it },
                    label = { Text("Merchant User ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = paymentAmount,
                        onValueChange = { paymentAmount = it },
                        label = { Text("Payment Amount") },
                        modifier = Modifier.weight(0.75f)
                    )
                    OutlinedTextField(
                        value = paymentCurrency,
                        onValueChange = { paymentCurrency = it },
                        label = { Text("Currency") },
                        modifier = Modifier.weight(0.25f)
                    )
                }
                OutlinedTextField(
                    value = successUrl,
                    onValueChange = { successUrl = it },
                    label = { Text("Success URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = failUrl,
                    onValueChange = { failUrl = it },
                    label = { Text("Error URL") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        DemoFormState(
                            token = token,
                            merchantId = merchantId,
                            gsmNo = gsmNo,
                            merchantUserId = merchantUserId,
                            paymentAmount = paymentAmount,
                            paymentCurrency = paymentCurrency,
                            orderId = currentDefaults.orderId,
                            installmentCount = currentDefaults.installmentCount,
                            paymentSecurity = currentDefaults.paymentSecurity,
                            transactionType = currentDefaults.transactionType,
                            environment = currentDefaults.environment,
                            successUrl = successUrl,
                            failUrl = failUrl,
                            troySonicSoundEnabled = currentDefaults.troySonicSoundEnabled,
                            showExitConfirmation = currentDefaults.showExitConfirmation
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EnvironmentChooserDialog(
    current: BexEnvironment,
    onDismiss: () -> Unit,
    onSelect: (BexEnvironment) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Environment") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                BexEnvironment.entries.forEach { env ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = env == current,
                                onClick = { onSelect(env) }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = env == current,
                            onClick = { onSelect(env) }
                        )
                        Text(
                            text = env.name,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Suppress("UNUSED_PARAMETER")
fun initPaymentSdk(
    context: android.content.Context,
    initParams: com.bkm.mobil.sdk.api.SdkInitParams,
    theme: BexFullSdkTheme?,
    mode: SdkMode,
    troySonicSoundEnabled: Boolean = true,
    showExitConfirmation: Boolean = true,
    onError: (BexSdkError) -> Unit,
    onCardSelected: (CardSelectionResult) -> Unit
) {
    BexFullSdk.init(
        context = context,
        initParams = initParams,
        callback = object : PaymentCallback {
            override fun onPaymentSuccess(result: PaymentResult) {
                Log.d("DEMO_APP", "Payment Success: $result")
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(
                        context,
                        "Payment Success: ${result.transactionId}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onError(error: BexSdkError) {
                Log.e("DEMO_APP", "Payment Error: $error")
                onError(error)
            }

            override fun onCardSelected(result: CardSelectionResult) {
                Log.d("DEMO_APP", "Card Selected: $result")
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(
                        context,
                        "Card Selected: ${result.selectedCard.cardAlias ?: result.selectedCard.bexBankInformation.bankShortName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                onCardSelected(result)
            }

            override fun onCardDeleted(result: CardDeletionResult) {
                Log.d("DEMO_APP", "Card Deleted: $result")
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(
                        context,
                        "Card Deleted: ${result.deletedCard.cardAlias ?: result.deletedCard.bexBankInformation.bankShortName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        },
        theme = theme,
        config = BexFullSdkConfig(
            troySonicSoundEnabled = troySonicSoundEnabled,
            mode = mode,
            showExitConfirmation = showExitConfirmation,
        )
    )
}
