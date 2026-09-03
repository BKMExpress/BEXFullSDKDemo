package com.bkm.mobil.sdk.demo

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bkm.mobil.sdk.api.BexEnvironment
import com.bkm.mobil.sdk.api.BexFullSdk
import com.bkm.mobil.sdk.api.BexSdkError
import com.bkm.mobil.sdk.api.CardSelectionResult
import com.bkm.mobil.sdk.api.PaymentSecurity
import com.bkm.mobil.sdk.api.SdkMode
import com.bkm.mobil.sdk.api.TransactionType
import kotlinx.coroutines.launch
import java.util.UUID

class XmlExampleActivity : AppCompatActivity() {

    private var loadedDefaults: DemoFormState? = null

    private lateinit var spinnerTheme: Spinner
    private lateinit var spinnerUxMode: Spinner
    private lateinit var spinnerDirectAction: Spinner
    private lateinit var spinnerPaymentSecurity: Spinner
    private lateinit var spinnerTransactionType: Spinner

    private lateinit var editToken: EditText
    private lateinit var editMerchantId: EditText
    private lateinit var editPhoneNumber: EditText
    private lateinit var editMerchantUserId: EditText
    private lateinit var editPaymentAmount: EditText
    private lateinit var editPaymentCurrency: EditText
    private lateinit var editOrderId: EditText
    private lateinit var editInstallmentCount: EditText
    private lateinit var editSuccessUrl: EditText
    private lateinit var editFailUrl: EditText

    private lateinit var buttonEnvironment: Button
    private lateinit var buttonResetDefaults: Button
    private lateinit var buttonSaveDefaultsCurrent: Button
    private lateinit var buttonSetDefaultsDialog: Button
    private lateinit var buttonSdkStart: Button

    private lateinit var selectedCardContainer: LinearLayout
    private lateinit var selectedCardName: TextView
    private lateinit var selectedCardDetail: TextView

    private var selectedTheme: DemoTheme = DemoTheme.Default
    private var selectedUxMode: DemoUxMode = DemoUxMode.FullScreen
    private var selectedDirectAction: DirectSdkAction = DirectSdkAction.StartPayment
    private var selectedPaymentSecurity: PaymentSecurity = PaymentSecurity.NONE
    private var selectedTransactionType: TransactionType = TransactionType.SALE
    private var selectedEnvironment: BexEnvironment = BexEnvironment.DEV

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_xml_example)

        spinnerTheme = findViewById(R.id.spinner_theme)
        spinnerUxMode = findViewById(R.id.spinner_ux_mode)
        spinnerDirectAction = findViewById(R.id.spinner_direct_action)
        spinnerPaymentSecurity = findViewById(R.id.spinner_payment_security)
        spinnerTransactionType = findViewById(R.id.spinner_transaction_type)

        editToken = findViewById(R.id.edit_token)
        editMerchantId = findViewById(R.id.edit_merchant_id)
        editPhoneNumber = findViewById(R.id.edit_phone_number)
        editMerchantUserId = findViewById(R.id.edit_merchant_user_id)
        editPaymentAmount = findViewById(R.id.edit_payment_amount)
        editPaymentCurrency = findViewById(R.id.edit_payment_currency)
        editOrderId = findViewById(R.id.edit_order_id)
        editInstallmentCount = findViewById(R.id.edit_installment_count)
        editSuccessUrl = findViewById(R.id.edit_success_url)
        editFailUrl = findViewById(R.id.edit_fail_url)

        buttonEnvironment = findViewById(R.id.button_environment)
        buttonResetDefaults = findViewById(R.id.button_reset_defaults)
        buttonSaveDefaultsCurrent = findViewById(R.id.button_save_defaults_current)
        buttonSetDefaultsDialog = findViewById(R.id.button_set_defaults_dialog)
        buttonSdkStart = findViewById(R.id.button_sdk_start)

        selectedCardContainer = findViewById(R.id.selected_card_container)
        selectedCardName = findViewById(R.id.selected_card_name)
        selectedCardDetail = findViewById(R.id.selected_card_detail)
        selectedCardContainer.visibility = LinearLayout.GONE

        setupSpinners()
        setupButtons()
        updateEnvironmentButtonLabel()

        lifecycleScope.launch {
            val defaults = applicationContext.loadDefaults()
            loadedDefaults = defaults
            applyFormState(defaults)
        }
    }

    private fun updateEnvironmentButtonLabel() {
        buttonEnvironment.text =
            getString(R.string.xml_environment_button, selectedEnvironment.name)
    }

    private fun setupSpinners() {
        val themes = DemoTheme.entries.map { it.name }
        spinnerTheme.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, themes)
        spinnerTheme.setSelection(0)
        spinnerTheme.onItemSelectedListener = SimpleSpinnerListener { position ->
            selectedTheme = DemoTheme.entries[position]
        }

        val uxLabels = listOf("Full Screen", "Bottom Sheet")
        spinnerUxMode.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, uxLabels)
        spinnerUxMode.setSelection(0)
        spinnerUxMode.onItemSelectedListener = SimpleSpinnerListener { position ->
            selectedUxMode = when (position) {
                0 -> DemoUxMode.FullScreen
                else -> DemoUxMode.BottomSheet
            }
        }

        val directLabels = listOf("Ödeme", "Kart Seçimi")
        spinnerDirectAction.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, directLabels)
        spinnerDirectAction.setSelection(0)
        spinnerDirectAction.onItemSelectedListener = SimpleSpinnerListener { position ->
            selectedDirectAction = when (position) {
                0 -> DirectSdkAction.StartPayment
                else -> DirectSdkAction.CardSelect
            }
        }

        val paymentLabels = listOf("3DS (TDS)", "OTP", "Non-OTP")
        spinnerPaymentSecurity.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, paymentLabels)
        spinnerPaymentSecurity.setSelection(2)
        spinnerPaymentSecurity.onItemSelectedListener = SimpleSpinnerListener { position ->
            selectedPaymentSecurity = when (position) {
                0 -> PaymentSecurity.TDS
                1 -> PaymentSecurity.OTP
                else -> PaymentSecurity.NONE
            }
        }

        val transactionLabels = listOf(
            "Satış (SALE)",
            "Ön Provizyon (PRE_AUTH)",
            "Tekrarlayan (RECURRING)"
        )
        spinnerTransactionType.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, transactionLabels)
        spinnerTransactionType.setSelection(0)
        spinnerTransactionType.onItemSelectedListener = SimpleSpinnerListener { position ->
            selectedTransactionType = when (position) {
                1 -> TransactionType.PRE_AUTH
                2 -> TransactionType.RECURRING
                else -> TransactionType.SALE
            }
        }
    }

    private fun setupButtons() {
        buttonEnvironment.setOnClickListener {
            showEnvironmentDialog()
        }

        buttonResetDefaults.setOnClickListener {
            loadedDefaults?.let { applyFormState(it) }
        }

        buttonSaveDefaultsCurrent.setOnClickListener {
            saveCurrentDefaultsWithValidation()
        }

        buttonSetDefaultsDialog.setOnClickListener {
            showSetDefaultsDialog()
        }

        buttonSdkStart.setOnClickListener {
            startSdk()
        }
    }

    private fun showEnvironmentDialog() {
        val names = BexEnvironment.entries.map { it.name }.toTypedArray()
        val currentIndex = BexEnvironment.entries.indexOf(selectedEnvironment).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(R.string.xml_environment_dialog_title)
            .setSingleChoiceItems(names, currentIndex) { dialog, which ->
                selectedEnvironment = BexEnvironment.entries[which]
                updateEnvironmentButtonLabel()
                lifecycleScope.launch {
                    val s = readFormState()
                    saveDefaults(s)
                    loadedDefaults = s
                }
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun startSdk() {
        val state = readFormState()
        val params = state.toInitParams()
        if (params == null) {
            Toast.makeText(
                this,
                "Please fill all init fields and valid amount",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val paramsWithTransactionId = params.copy(
            transactionId = UUID.randomUUID().toString()
        )

        val mode = when (selectedDirectAction) {
            DirectSdkAction.StartPayment -> SdkMode.PAYMENT
            DirectSdkAction.CardSelect -> SdkMode.CARD_SELECTION_ONLY
        }

        initPaymentSdk(
            context = this,
            initParams = paramsWithTransactionId,
            theme = selectedTheme.toPaymentSdkTheme(),
            mode = mode,
            onError = ::handleError,
            onCardSelected = ::onCardSelected
        )

        when (selectedUxMode) {
            DemoUxMode.FullScreen -> BexFullSdk.start()
            DemoUxMode.BottomSheet -> BexFullSdk.showAsBottomSheet(this)
        }
    }

    private fun onCardSelected(result: CardSelectionResult) {
        val selected = result.selectedCard
        val name = selected.cardAlias ?: selected.bexBankInformation.bankShortName
        val detail =
            "${selected.bexBankInformation.cardBrand} • ${selected.maskCardNumber} • ${selected.bexBankInformation.bankCode}"

        runOnUiThread {
            selectedCardName.text = name
            selectedCardDetail.text = detail
            selectedCardContainer.visibility = LinearLayout.VISIBLE
        }
    }

    private fun handleError(error: BexSdkError) {
        Toast.makeText(this, error.displayMessage, Toast.LENGTH_SHORT).show()
    }

    private fun showMissingSaveDefaultsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Eksik Bilgi")
            .setMessage("Varsayılanları kaydetmek için tüm zorunlu alanları doldurun.")
            .setPositiveButton("Tamam", null)
            .show()
    }

    private fun saveCurrentDefaultsWithValidation() {
        val current = readFormState()
        if (current.token.isBlank() ||
            current.merchantId.isBlank() ||
            current.gsmNo.isBlank() ||
            current.merchantUserId.isBlank()
        ) {
            showMissingSaveDefaultsDialog()
            return
        }

        lifecycleScope.launch {
            saveDefaults(current)
            loadedDefaults = current
            Toast.makeText(this@XmlExampleActivity, "Varsayılanlar kaydedildi", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun readFormState(): DemoFormState =
        DemoFormState(
            token = editToken.text.toString(),
            merchantId = editMerchantId.text.toString(),
            gsmNo = editPhoneNumber.text.toString(),
            merchantUserId = editMerchantUserId.text.toString(),
            paymentAmount = editPaymentAmount.text.toString(),
            paymentCurrency = editPaymentCurrency.text.toString(),
            orderId = editOrderId.text.toString(),
            installmentCount = editInstallmentCount.text.toString(),
            paymentSecurity = selectedPaymentSecurity,
            transactionType = selectedTransactionType,
            environment = selectedEnvironment,
            successUrl = editSuccessUrl.text.toString(),
            failUrl = editFailUrl.text.toString()
        )

    private fun applyFormState(state: DemoFormState) {
        editToken.setText(state.token)
        editMerchantId.setText(state.merchantId)
        editPhoneNumber.setText(state.gsmNo)
        editMerchantUserId.setText(state.merchantUserId)
        editPaymentAmount.setText(state.paymentAmount)
        editPaymentCurrency.setText(state.paymentCurrency)
        editOrderId.setText(state.orderId)
        editInstallmentCount.setText(state.installmentCount)
        editSuccessUrl.setText(state.successUrl)
        editFailUrl.setText(state.failUrl)
        selectedPaymentSecurity = state.paymentSecurity
        spinnerPaymentSecurity.setSelection(
            when (state.paymentSecurity) {
                PaymentSecurity.TDS -> 0
                PaymentSecurity.OTP -> 1
                PaymentSecurity.NONE -> 2
            }
        )
        selectedTransactionType = state.transactionType
        spinnerTransactionType.setSelection(
            when (state.transactionType) {
                TransactionType.SALE -> 0
                TransactionType.PRE_AUTH -> 1
                TransactionType.RECURRING -> 2
            }
        )
        selectedEnvironment = state.environment
        updateEnvironmentButtonLabel()
    }

    private fun showSetDefaultsDialog() {
        val defaultsForDialog = loadedDefaults ?: readFormState()

        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(32, 16, 32, 16)

        fun makeEditText(label: String, current: String): EditText {
            return EditText(this).apply {
                hint = label
                setText(current)
            }
        }

        val tokenEdit = makeEditText("Token", defaultsForDialog.token).apply {
            setSingleLine(false)
        }
        val merchantIdEdit = makeEditText("Merchant ID", defaultsForDialog.merchantId)
        val phoneEdit = makeEditText("Phone Number", defaultsForDialog.gsmNo).apply {
            inputType = InputType.TYPE_CLASS_PHONE
        }
        val merchantUserIdEdit = makeEditText("Merchant User ID", defaultsForDialog.merchantUserId)
        val amountEdit = makeEditText("Payment Amount", defaultsForDialog.paymentAmount).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val currencyEdit = makeEditText("Currency", defaultsForDialog.paymentCurrency)
        val successUrlEdit = makeEditText("Success URL", defaultsForDialog.successUrl)
        val failUrlEdit = makeEditText("Error URL", defaultsForDialog.failUrl)

        val scroll = ScrollView(this)
        scroll.addView(container.apply {
            addView(tokenEdit)
            addView(merchantIdEdit)
            addView(phoneEdit)
            addView(merchantUserIdEdit)
            addView(amountEdit)
            addView(currencyEdit)
            addView(successUrlEdit)
            addView(failUrlEdit)
        })

        AlertDialog.Builder(this)
            .setTitle("Set Defaults")
            .setView(scroll)
            .setPositiveButton("Save") { _, _ ->
                val newState = DemoFormState(
                    token = tokenEdit.text.toString(),
                    merchantId = merchantIdEdit.text.toString(),
                    gsmNo = phoneEdit.text.toString(),
                    merchantUserId = merchantUserIdEdit.text.toString(),
                    paymentAmount = amountEdit.text.toString(),
                    paymentCurrency = currencyEdit.text.toString(),
                    orderId = defaultsForDialog.orderId,
                    installmentCount = defaultsForDialog.installmentCount,
                    paymentSecurity = defaultsForDialog.paymentSecurity,
                    transactionType = defaultsForDialog.transactionType,
                    environment = defaultsForDialog.environment,
                    successUrl = successUrlEdit.text.toString(),
                    failUrl = failUrlEdit.text.toString()
                )
                lifecycleScope.launch {
                    saveDefaults(newState)
                    loadedDefaults = newState
                    applyFormState(newState)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private class SimpleSpinnerListener(
        private val onSelected: (Int) -> Unit
    ) : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
            parent: android.widget.AdapterView<*>,
            view: android.view.View?,
            position: Int,
            id: Long
        ) {
            onSelected(position)
        }

        override fun onNothingSelected(parent: android.widget.AdapterView<*>) = Unit
    }
}
