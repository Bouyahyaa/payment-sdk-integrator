package com.bouyahyaa.composebasedapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.bouyahyaa.composebasedapp.ui.screens.LoginScreen
import com.bouyahyaa.composebasedapp.ui.screens.MerchantDashboard
import com.bouyahyaa.payment.PaymentSDK

enum class AppScreen {
    LOGIN,
    DASHBOARD
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PaymentSDK.initEnvironment(
            context = this,
            apiKey = "MERCHANT_API_KEY_123",
            apiSecret = "MERCHANT_SECRET_456"
        )

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(AppScreen.LOGIN) }

                    // Simple Navigation
                    when (currentScreen) {
                        AppScreen.LOGIN -> {
                            LoginScreen(
                                onLoginSuccess = { currentScreen = AppScreen.DASHBOARD }
                            )
                        }

                        AppScreen.DASHBOARD -> {
                            MerchantDashboard()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PaymentSDK.cleanup(this)
    }
}