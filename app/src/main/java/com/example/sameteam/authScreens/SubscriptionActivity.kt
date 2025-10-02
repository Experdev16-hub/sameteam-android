package com.example.sameteam.authScreens

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.example.sameteam.R
import com.example.sameteam.authScreens.viewModel.LoginVM
import com.example.sameteam.base.BaseActivity
import com.example.sameteam.base.BaseViewModel
import com.example.sameteam.databinding.ActivityLoginBinding
import com.example.sameteam.homeScreen.HomeActivity

class SubscriptionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)
    }

    fun initActivity(mBinding: ViewDataBinding) {
        println("[Activity Rendered]")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing to disable back navigation
    }

    private lateinit var billingClient: BillingClient
    private var isBillingClientReady = false

    fun launchBillingClient(view: View) {
        billingClient = BillingClient.newBuilder(this)
            .enablePendingPurchases()
            .setListener { billingResult, purchases ->
                // Purchase updates listener: not used in this example since we're only querying product details.
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        println("Bulling flow 1")
                        val intent = Intent(this@SubscriptionActivity, LoginActivity::class.java)
                        startActivity(intent)
                    }
                } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                    // Handle an error caused by a user canceling the purchase flow.
                    println("Bulling flow 2")
                } else {
                    // Handle any other error codes.
                    println("Bulling flow 3")
                    val intent = Intent(this@SubscriptionActivity, LoginActivity::class.java)
                    startActivity(intent)
                }
            }
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isBillingClientReady = true
                    Log.d("SubscriptionBilling", "Billing Client connected.")
                    val subscriptionIds = listOf("yearly_subscription")



                    billingClient.queryPurchasesAsync(BillingClient.ProductType.SUBS){ billingResult, list ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            // Activate premium feature
                            fetchSubscriptionProducts(
                                subscriptionIds = subscriptionIds,
                                onResult = { productDetailsList ->
                                    // Handle the list of ProductDetails (e.g., update UI, etc.)
                                    Log.d("SubscriptionFragment", " : $productDetailsList")
                                    if (productDetailsList.isNotEmpty() and isBillingClientReady) {
                                        val selectedProductDetails = productDetailsList.first()
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            launchSubscriptionBillingFlow(selectedProductDetails)
                                        }, 800)
                                    }
                                },
                                onError = { errorMessage ->
                                    // Handle error (e.g., show a Toast, log the error, etc.)
                                    Log.e("SubscriptionFragment", "Error fetching products: $errorMessage")
                                }
                            )
                        }
                    }


                } else {
                    Log.e("SubscriptionBilling", "Billing Client connection failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                isBillingClientReady = false
                Log.e("SubscriptionBilling", "Billing Service disconnected.")
                // Optionally, implement reconnection logic here.
            }
        })
    }

    fun launchSubscriptionBillingFlow(productDetails: ProductDetails) {
        if (!isBillingClientReady) {
            Log.e("SubscriptionBilling", "Billing Client is not ready")
            return
        }

        val subscriptionOffers = productDetails.subscriptionOfferDetails

        // Check if there is at least one offer available.
        if (subscriptionOffers.isNullOrEmpty()) {
            Log.e("SubscriptionBilling", "No subscription offers available for this product.")
            return
        }

        // For this example, we'll choose the first available offer.
        // In a real app, you might let the user select an offer or implement additional logic.
        val selectedOffer = subscriptionOffers.first()
        val offerToken = selectedOffer.offerToken

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                .setProductDetails(productDetails)
                // For One-time products, "setOfferToken" method shouldn't be called.
                // For subscriptions, to get an offer token, call ProductDetails.subscriptionOfferDetails()
                // for a list of offers that are available to the user
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        // Launch the billing flow
        val bResult = billingClient.launchBillingFlow(this@SubscriptionActivity, billingFlowParams)

        if (bResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d("SubscriptionBilling", "Billing flow launched successfully.")
        } else {
            Log.e("SubscriptionBilling", "Failed to launch billing flow: ${bResult.debugMessage}")
        }
    }

    /**
     * Public method to query product details for a list of subscription product IDs.
     *
     * @param subscriptionIds List of subscription product IDs defined in the Play Console.
     * @param onResult Callback returning a list of [ProductDetails] on success.
     * @param onError Optional callback returning an error message if the query fails.
     */
    fun fetchSubscriptionProducts(
        subscriptionIds: List<String>,
        onResult: (List<ProductDetails>) -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        if (!isBillingClientReady) {
            onError?.invoke("Billing Client is not ready")
            return
        }

        // Build a list of Product query objects for subscriptions.
        val productList = subscriptionIds.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onResult(productDetailsList)
            } else {
                onError?.invoke("Error fetching product details: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Optional public method to end the billing client connection.
     * Call this when the billing functionality is no longer needed.
     */
    fun endBillingConnection() {
        if (this::billingClient.isInitialized && billingClient.isReady) {
            billingClient.endConnection()
            isBillingClientReady = false
            Log.d("SubscriptionBilling", "Billing Client connection ended.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        endBillingConnection()
    }

    fun setupBillingClient(view: View) {}

}

