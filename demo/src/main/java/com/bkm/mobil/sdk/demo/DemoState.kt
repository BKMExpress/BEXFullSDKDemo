package com.bkm.mobil.sdk.demo

import com.bkm.mobil.sdk.api.BexEnvironment
import com.bkm.mobil.sdk.api.BexFullSdkTheme
import com.bkm.mobil.sdk.api.PaymentSecurity
import com.bkm.mobil.sdk.api.SdkInitParams
import com.bkm.mobil.sdk.api.SdkPaymentInfo
import com.bkm.mobil.sdk.api.TransactionType

enum class DemoTheme {
    Default,
    Akbank,
    Getir
}

enum class DemoUxMode {
    FullScreen,
    BottomSheet
}

data class DemoFormState(
    val token: String = "",
    val merchantId: String = "",
    val gsmNo: String = "",
    val merchantUserId: String = "",
    val paymentAmount: String = "",
    val paymentCurrency: String = "",
    val orderId: String = "",
    val installmentCount: String = "",
    val paymentSecurity: PaymentSecurity = PaymentSecurity.NONE,
    val transactionType: TransactionType = TransactionType.SALE,
    val environment: BexEnvironment = BexEnvironment.DEV,
    val successUrl: String = "",
    val failUrl: String = ""
) {
    fun toInitParams(): SdkInitParams? {
        val amount = paymentAmount.toDoubleOrNull() ?: return null
        val installments = installmentCount.toIntOrNull() ?: return null
        if (token.isBlank() || merchantId.isBlank() || gsmNo.isBlank() || merchantUserId.isBlank()) return null
        return SdkInitParams(
            token = token.trim(),
            merchantId = merchantId.trim(),
            transactionId = "",
            gsmNo = gsmNo.trim(),
            merchantUserId = merchantUserId.trim(),
            environment = environment,
            paymentInfo = SdkPaymentInfo(
                amount = amount,
                orderId = orderId.trim(),
                transactionDate = System.currentTimeMillis().toString(),
                paymentSecurity = paymentSecurity,
                currency = paymentCurrency.trim(),
                installmentCount = installments,
                transactionType = transactionType,
                successUrl = successUrl.trim(),
                failUrl = failUrl.trim()
            )
        )
    }
}

fun DemoTheme.toPaymentSdkTheme(): BexFullSdkTheme? = when (this) {
    DemoTheme.Default -> null
    DemoTheme.Getir -> BexFullSdkTheme(
        colors = BexFullSdkTheme.Colors(
            primary = 0xFF5c3cbb.toInt(),
            textPrimary = 0xFF000000.toInt(),
            buttonPrimary = 0xFF5c3cbb.toInt(),
            buttonPrimaryText = 0xFFFFFFFF.toInt(),
            buttonSecondaryText = 0xFF5c3cbb.toInt(),
            buttonSecondaryBorder = 0xFF5c3cbb.toInt()
        ),
        shape = BexFullSdkTheme.Shape(
            buttonCornerRadius = 8f,
            buttonBorderWidth = 1.5f
        )
    )

    DemoTheme.Akbank -> BexFullSdkTheme(
        colors = BexFullSdkTheme.Colors(
            primary = 0xFFDB3931.toInt(),
            textPrimary = 0xFF000000.toInt(),
            buttonPrimary = 0xFFDB3931.toInt(),
            buttonPrimaryText = 0xFFFFFFFF.toInt(),
            buttonSecondaryBorder = 0xFFDB3931.toInt(),
            buttonSecondaryText = 0xFFDB3931.toInt()
        ),
        shape = BexFullSdkTheme.Shape(
            buttonCornerRadius = 24f,
            buttonBorderWidth = 2f
        )
    )
}
