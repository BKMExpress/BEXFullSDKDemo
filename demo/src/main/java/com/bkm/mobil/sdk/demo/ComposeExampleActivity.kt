package com.bkm.mobil.sdk.demo

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bkm.mobil.sdk.api.BexEnvironment
import com.bkm.mobil.sdk.api.BexFullSdk
import com.bkm.mobil.sdk.api.BexFullSdkConfig
import com.bkm.mobil.sdk.api.BexFullSdkTheme
import com.bkm.mobil.sdk.api.BexSdkError
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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
    var selectedDirectAction by remember { mutableStateOf<DirectSdkAction?>(DirectSdkAction.StartPayment) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showSetDefaultsDialog by remember { mutableStateOf(false) }
    var showEnvironmentDialog by remember { mutableStateOf(false) }
    var showSaveDefaultsMissingDialog by remember { mutableStateOf(false) }
    var paymentInfoExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val loaded = context.loadDefaults()
        defaultsState.value = loaded
        formStateState.value = loaded
    }

    val defaults = defaultsState.value ?: DemoFormState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = { Text("BEX FULL SDK Demo") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var themeExpanded by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tema",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentSize(Alignment.TopStart)
                        ) {
                            OutlinedButton(
                                onClick = { themeExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(theme.name)
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = themeExpanded,
                                onDismissRequest = { themeExpanded = false }
                            ) {
                                DemoTheme.entries.forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t.name) },
                                        onClick = {
                                            theme = t
                                            themeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    var uxExpanded by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "UX Modu",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentSize(Alignment.TopStart)
                        ) {
                            OutlinedButton(
                                onClick = { uxExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        when (uxMode) {
                                            DemoUxMode.FullScreen -> "Full Screen"
                                            DemoUxMode.BottomSheet -> "Bottom Sheet"
                                        }
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = uxExpanded,
                                onDismissRequest = { uxExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Full Screen") },
                                    onClick = { uxMode = DemoUxMode.FullScreen; uxExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Bottom Sheet") },
                                    onClick = {
                                        uxMode = DemoUxMode.BottomSheet; uxExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var directSdkExpanded by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SDK Modu",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentSize(Alignment.TopStart)
                        ) {
                            OutlinedButton(
                                onClick = { directSdkExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        when (selectedDirectAction) {
                                            DirectSdkAction.StartPayment -> "Ödeme"
                                            DirectSdkAction.CardSelect -> "Kart Seçimi"
                                            null -> "Seçiniz…"
                                        }
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = directSdkExpanded,
                                onDismissRequest = { directSdkExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Ödeme") },
                                    onClick = {
                                        selectedDirectAction = DirectSdkAction.StartPayment
                                        directSdkExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Kart Seçimi") },
                                    onClick = {
                                        selectedDirectAction = DirectSdkAction.CardSelect
                                        directSdkExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    var paymentTypeExpanded by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ödeme Türü",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentSize(Alignment.TopStart)
                        ) {
                            OutlinedButton(
                                onClick = { paymentTypeExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        when (formStateState.value.paymentSecurity) {
                                            PaymentSecurity.TDS -> "3DS (TDS)"
                                            PaymentSecurity.OTP -> "OTP"
                                            PaymentSecurity.NONE -> "Non-OTP"
                                        }
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = paymentTypeExpanded,
                                onDismissRequest = { paymentTypeExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("3DS (TDS)") },
                                    onClick = {
                                        formStateState.value =
                                            formStateState.value.copy(
                                                paymentSecurity = PaymentSecurity.TDS
                                            )
                                        paymentTypeExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("OTP") },
                                    onClick = {
                                        formStateState.value =
                                            formStateState.value.copy(
                                                paymentSecurity = PaymentSecurity.OTP
                                            )
                                        paymentTypeExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Non-OTP") },
                                    onClick = {
                                        formStateState.value =
                                            formStateState.value.copy(
                                                paymentSecurity = PaymentSecurity.NONE
                                            )
                                        paymentTypeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Init params inputs
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),

                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SDK Init Parameters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row {
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
                    }
                }
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
                Spacer(modifier = Modifier.height(8.dp))
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
                Spacer(modifier = Modifier.height(8.dp))

                // SdkPaymentInfo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ödeme Bilgileri",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = { paymentInfoExpanded = !paymentInfoExpanded }) {
                        Icon(
                            imageVector = if (paymentInfoExpanded) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                            contentDescription = if (paymentInfoExpanded) "Daralt" else "Genişlet"
                        )
                    }
                }
                AnimatedVisibility(visible = paymentInfoExpanded) {
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
                                        IconButton(
                                            onClick = {
                                                formStateState.value =
                                                    formStateState.value.copy(paymentAmount = "")
                                            }
                                        ) {
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
                        var transactionTypeExpanded by remember { mutableStateOf(false) }
                        Text(
                            text = "İşlem Türü",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentSize(Alignment.TopStart)
                        ) {
                            OutlinedButton(
                                onClick = { transactionTypeExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        when (formStateState.value.transactionType) {
                                            TransactionType.SALE -> "Satış (SALE)"
                                            TransactionType.PRE_AUTH -> "Ön Provizyon (PRE_AUTH)"
                                            TransactionType.RECURRING -> "Tekrarlayan (RECURRING)"
                                        }
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = transactionTypeExpanded,
                                onDismissRequest = { transactionTypeExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Satış (SALE)") },
                                    onClick = {
                                        formStateState.value =
                                            formStateState.value.copy(
                                                transactionType = TransactionType.SALE
                                            )
                                        transactionTypeExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Ön Provizyon (PRE_AUTH)") },
                                    onClick = {
                                        formStateState.value =
                                            formStateState.value.copy(
                                                transactionType = TransactionType.PRE_AUTH
                                            )
                                        transactionTypeExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Tekrarlayan (RECURRING)") },
                                    onClick = {
                                        formStateState.value =
                                            formStateState.value.copy(
                                                transactionType = TransactionType.RECURRING
                                            )
                                        transactionTypeExpanded = false
                                    }
                                )
                            }
                        }
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

                // Selected Card (if any)
                selectedCard?.let { card ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Selected Card",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = card.selectedCard.cardAlias
                                    ?: card.selectedCard.bexBankInformation.bankShortName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${card.selectedCard.bexBankInformation.cardBrand} • ${card.selectedCard.maskCardNumber}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
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
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
private fun SetDefaultsDialog(
    currentDefaults: DemoFormState,
    onDismiss: () -> Unit,
    onSave: (DemoFormState) -> Unit
) {
    var token by remember(currentDefaults) { mutableStateOf(currentDefaults.token) }
    var merchantId by remember(currentDefaults) { mutableStateOf(currentDefaults.merchantId) }
    var gsmNo by remember(currentDefaults) { mutableStateOf(currentDefaults.gsmNo) }
    var merchantUserId by remember(currentDefaults) { mutableStateOf(currentDefaults.merchantUserId) }
    var paymentAmount by remember(currentDefaults) { mutableStateOf(currentDefaults.paymentAmount) }
    var paymentCurrency by remember(currentDefaults) { mutableStateOf(currentDefaults.paymentCurrency) }
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
                            failUrl = failUrl
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

fun initPaymentSdk(
    context: android.content.Context,
    initParams: com.bkm.mobil.sdk.api.SdkInitParams,
    theme: BexFullSdkTheme?,
    mode: SdkMode,
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
        },
        theme = theme,
        config = BexFullSdkConfig(troySonicSoundEnabled = true, mode = mode)
    )
}
