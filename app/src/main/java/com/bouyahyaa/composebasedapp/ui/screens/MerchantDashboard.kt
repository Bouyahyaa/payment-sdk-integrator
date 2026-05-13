package com.bouyahyaa.composebasedapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bouyahyaa.payment.PaymentSDK
import com.bouyahyaa.payment.models.PaymentError
import kotlinx.coroutines.launch

@Composable
fun MerchantDashboard() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Reactive State for UI
    val logs = remember { mutableStateListOf("✅ Terminal Personalised.", "✅ POS Dashboard Ready.") }
    var lastTransactionId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    fun log(message: String) {
        logs.add(message)
        coroutineScope.launch {
            if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
        }
    }

    fun handlePaymentError(action: String, error: PaymentError) {
        when (error) {
            is PaymentError.UserCancelled -> log("⚠️ $action was cancelled by the user.")
            else -> log("❌ $action failed: ${error.message}")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "POS Dashboard",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

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
                    PaymentSDK.config.showSettings(context = context)
                    log("⚙️ Settings UI launched.")
                }
            ) { Text("1. Show Terminal Settings") }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    log("\n--- Checking Device Usability ---")
                    PaymentSDK.config.getUsabilityReport { report ->
                        log("NFC Supported: ${report.isNfcSupportedByDevice}")
                        log("Root / Attestation: ${report.keyAttestationResult}")
                    }
                }
            ) { Text("2. Check Device Usability") }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val amount = 25.50
                    log("\n--- Starting Tap-to-Pay (€$amount) ---")
                    PaymentSDK.transaction.processPayment(
                        context = context, amount = amount,
                        onSuccess = { result ->
                            lastTransactionId = result.transactionId
                            log("✅ Payment Success! ID: ${result.transactionId}")
                        },
                        onError = { err -> handlePaymentError("Payment", err) }
                    )
                }
            ) { Text("3. Process Payment (€25.50)") }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val txnId = lastTransactionId
                    if (txnId == null) {
                        log("❌ No previous transaction to refund."); return@Button
                    }
                    PaymentSDK.transaction.refundPayment(
                        context = context, transactionId = txnId, amount = 25.50,
                        onSuccess = { result -> log("✅ Refund Success! Status: ${result.status}") },
                        onError = { err -> handlePaymentError("Refund", err) }
                    )
                }
            ) { Text("4. Refund Last Payment") }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val txnId = lastTransactionId
                    if (txnId == null) {
                        log("❌ No previous transaction to void."); return@Button
                    }

                    PaymentSDK.transaction.voidTransaction(
                        context = context,
                        transactionId = lastTransactionId,
                        onSuccess = { result -> log("✅ Void Success! Status: ${result.status}") },
                        onError = { err -> handlePaymentError("Void", err) }
                    )
                }
            ) { Text("5. Void Last Transaction") }

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
            ) { Text("6. Get Transaction History") }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    log("\n--- Adjusting NFC Volume ---")
                    val newLevel = PaymentSDK.hardware.setNfcChirpVolume(context, 5)
                    log("✅ NFC volume set to level $newLevel")
                }
            ) { Text("7. Maximize NFC Volume") }
        }

        // --- Console Logs Area ---
        Text(
            text = "Console Logs:",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
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