package com.example.app_lock_things

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.app_lock_things.ui.theme.App_lock_thingsTheme

class MainActivity : ComponentActivity() {

    private val lockViewModel: LockViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            lockViewModel.updateNfcSupport(false)
        }

        setContent {
            App_lock_thingsTheme {
                val statusMessage by lockViewModel.statusMessage.collectAsState()
                val isLockOpen by lockViewModel.isLockOpen.collectAsState()
                val isNfcSupported by lockViewModel.isNfcSupported.collectAsState()

                MainScreen(
                    statusMessage = statusMessage,
                    isLockOpen = isLockOpen,
                    onAssignKey = { lockViewModel.assignKey() },
                    onOpenLock = { lockViewModel.openLock() },
                    onCloseLock = { lockViewModel.closeLock() },
                    isNfcSupported = isNfcSupported
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.let {
            val intent = Intent(this, javaClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val filter = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
            it.enableForegroundDispatch(this, pendingIntent, filter, null)
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
        lockViewModel.cancelTimer()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) ?: return
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                processNdefTag(ndef)
            } else {
                lockViewModel.processTag(tag)
            }
        }
    }

    private fun processNdefTag(ndef: Ndef) {
        val ndefMessage = ndef.cachedNdefMessage
        val tagId = ndef.tag.id.joinToString(separator = "") { String.format("%02X", it) }
        Log.d("NFC_TAG", "NDEF Tag ID: $tagId")
        lockViewModel.processTag(ndef.tag)
    }

    @Composable
    fun MainScreen(
        statusMessage: String,
        isLockOpen: Boolean,
        onAssignKey: () -> Unit,
        onOpenLock: () -> Unit,
        onCloseLock: () -> Unit,
        isNfcSupported: Boolean
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Image(
                painter = painterResource(R.drawable.background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Column(
                modifier = Modifier
                    .padding(50.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = onAssignKey, enabled = isNfcSupported) {
                    Text("Atribuir chave ao cadeado", color = Color.White)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onOpenLock,
                        enabled = isNfcSupported
                    ) {
                        Text("Abrir cadeado", color = Color.White)
                    }
                    Button(
                        onClick = onCloseLock,
                        enabled = isNfcSupported
                    ) {
                        Text("Fechar cadeado", color = Color.White)
                    }
                }
                Image(
                    painter = painterResource(if (isLockOpen) R.drawable.aberto else R.drawable.fechado),
                    contentDescription = if (isLockOpen) "Cadeado Aberto" else "Cadeado Fechado",
                    modifier = Modifier.size(138.dp)
                )
                Text(text = statusMessage, modifier = Modifier.padding(top = 10.dp))
                if (!isNfcSupported) {
                    Text(text = "Este dispositivo não tem suporte a NFC")
                }
            }
        }
    }


    @Composable
    @Preview(showBackground = true)
    fun DefaultPreview() {
        App_lock_thingsTheme {
            MainScreen(
                statusMessage = "Cadeado Fechado",
                isLockOpen = false,
                onAssignKey = {},
                onOpenLock = {},
                onCloseLock = {},
                isNfcSupported = true
            )
        }
    }
}
