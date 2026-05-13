package com.bouyahyaa.composebasedapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bouyahyaa.payment.PaymentSDK
import com.bouyahyaa.payment.models.PaymentError
import com.bouyahyaa.payment.models.TerminalOrientation
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the SDK right when the app opens
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
                    MerchantDashboard()
                }
            }
        }
    }
}

@Composable
fun MerchantDashboard() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Reactive State for UI
    val logs = remember { mutableStateListOf("✅ PaymentSDK Initialized.") }
    var lastTransactionId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // Helper to add logs and auto-scroll to the bottom
    fun log(message: String) {
        logs.add(message)
        coroutineScope.launch {
            if (logs.isNotEmpty()) {
                listState.animateScrollToItem(logs.size - 1)
            }
        }
    }

    // Unified error handler
    fun handlePaymentError(action: String, error: PaymentError) {
        when (error) {
            is PaymentError.BatteryLow -> log("❌ $action failed: Please plug in your device.")
            is PaymentError.Network -> log("❌ $action failed: Check your internet connection.")
            is PaymentError.UserCancelled -> log("⚠️ $action was cancelled by the user.")
            else -> log("❌ $action failed: ${error.message} (Code: ${error.rawCode})")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- Header ---
        Text(
            text = "POS Dashboard",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        // --- Action Buttons (Scrollable) ---
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    log("\n--- Configuring Terminal ---")
                    PaymentSDK.config.configureSdk(
                        context = context,
                        tid = "12345678",
                        password = "SecurePassword123!",
                        orientation = TerminalOrientation.PORTRAIT,
                        onSuccess = { log("✅ Terminal Configuration Successful") },
                        onError = { err -> handlePaymentError("Configuration", err) }
                    )
                }
            ) { Text("1. Configure Terminal") }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    log("\n--- Opening Terminal Settings ---")
                    PaymentSDK.config.showSettings(context = context)
                    log("⚙️ Settings UI launched.")
                }
            ) { Text("2. Show Terminal Settings") }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    log("\n--- Checking Device Usability ---")
                    PaymentSDK.config.getUsabilityReport { report ->
                        log("NFC Supported: ${report.isNfcSupportedByDevice}")
                        log("Root / Attestation: ${report.keyAttestationResult}")
                    }
                }
            ) { Text("3. Check Device Usability") }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val amount = 25.50
                    log("\n--- Starting Tap-to-Pay (€$amount) ---")
                    PaymentSDK.transaction.processPayment(
                        context = context,
                        amount = amount,
                        onSuccess = { result ->
                            lastTransactionId = result.transactionId
                            log("✅ Payment Success! ID: ${result.transactionId}")

                            log("🖨️ Printing receipt...")
                            PaymentSDK.hardware.printCustomerReceipt(
                                context = context,
                                lines = result.customerReceiptLines,
                                qrCodeBytes = null,
                                onSuccess = { log("✅ Receipt printed successfully.") },
                                onError = { err -> log("❌ Print failed: ${err.message}") }
                            )
                        },
                        onError = { err -> handlePaymentError("Payment", err) }
                    )
                }
            ) { Text("4. Process Payment (€25.50)") }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val txnId = lastTransactionId
                    if (txnId == null) {
                        log("❌ No previous transaction to refund.")
                        return@Button
                    }
                    log("\n--- Starting Refund for ID: $txnId ---")
                    PaymentSDK.transaction.refundPayment(
                        context = context,
                        transactionId = txnId,
                        amount = 25.50,
                        onSuccess = { result -> log("✅ Refund Success! Status: ${result.status}") },
                        onError = { err -> handlePaymentError("Refund", err) }
                    )
                }
            ) { Text("5. Refund Last Payment") }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    log("\n--- Voiding Last Transaction ---")
                    PaymentSDK.transaction.voidTransaction(
                        context = context,
                        transactionId = lastTransactionId,
                        onSuccess = { result -> log("✅ Void Success! Status: ${result.status}") },
                        onError = { err -> handlePaymentError("Void", err) }
                    )
                }
            ) { Text("6. Void Last Transaction") }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    log("\n--- Fetching Transaction History ---")
                    PaymentSDK.transaction.getTransactionHistory(
                        page = 1,
                        itemCount = 5,
                        isMerchantScope = true,
                        onSuccess = { history ->
                            log("✅ Found ${history.transactions.size} records.")
                            history.transactions.forEach { txn ->
                                log("  -> ${txn.transactionTime}: €${txn.amount} (${txn.answerText})")
                            }
                        },
                        onError = { err -> handlePaymentError("History", err) }
                    )
                }
            ) { Text("7. Get Transaction History") }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    log("\n--- Adjusting NFC Volume ---")
                    val newLevel = PaymentSDK.hardware.setNfcChirpVolume(context, 5)
                    log("✅ NFC volume set to level $newLevel")
                }
            ) { Text("8. Maximize NFC Volume") }
        }

        // --- Console Logs Area ---
        Text(
            text = "Console Logs:",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFFE0E0E0))
                .padding(8.dp)
        ) {
            items(logs) { logMsg ->
                Text(
                    text = logMsg,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}