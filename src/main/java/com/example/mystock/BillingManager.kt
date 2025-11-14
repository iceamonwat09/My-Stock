package com.example.mystock

import android.app.Activity
import android.content.Context
import androidx.datastore.preferences.core.edit
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BillingManager(private val context: Context) {

    companion object {
        const val PRODUCT_ID_PRO = "pro_version_unlimited"
        const val FREE_ROW_LIMIT = 50
    }

    private lateinit var billingClient: BillingClient

    val isProVersionFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DataStoreKeys.PRO_VERSION_KEY] ?: false
    }

    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    handlePurchases(purchases)
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Retry connection
            }
        })
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.products.contains(PRODUCT_ID_PRO) && purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
                saveProStatus(true)
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                saveProStatus(true)
            }
        }
    }

    private fun saveProStatus(isPro: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.PRO_VERSION_KEY] = isPro
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, onResult: (Boolean, String) -> Unit) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_PRO)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]

                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                        )
                    )
                    .build()

                val result = billingClient.launchBillingFlow(activity, flowParams)
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    onResult(true, "กำลังเปิดหน้าซื้อ...")
                } else {
                    onResult(false, "ไม่สามารถเปิดหน้าซื้อได้")
                }
            } else {
                onResult(false, "ไม่พบสินค้า")
            }
        }
    }

    suspend fun canAddMoreRows(currentRowCount: Int): Boolean {
        val isPro = isProVersionFlow.first()
        return isPro || currentRowCount < FREE_ROW_LIMIT
    }

    fun destroy() {
        billingClient.endConnection()
    }
}
