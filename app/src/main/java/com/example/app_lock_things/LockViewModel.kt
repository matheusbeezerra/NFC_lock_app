package com.example.app_lock_things

import android.app.Application
import android.media.MediaPlayer
import android.nfc.Tag
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LockViewModel(application: Application) : AndroidViewModel(application) {
    private val _statusMessage = MutableStateFlow("Cadeado fechado")
    val statusMessage: StateFlow<String> = _statusMessage

    private val _isLockOpen = MutableStateFlow(false)
    val isLockOpen: StateFlow<Boolean> = _isLockOpen

    private val _isNfcSupported = MutableStateFlow(true)
    val isNfcSupported: StateFlow<Boolean> = _isNfcSupported

    var assignedTagId: String? = null
    private var timerJob: Job? = null

    private val mediaPlayer: MediaPlayer = MediaPlayer.create(application, R.raw.error_sound)

    fun assignKey() {
        assignedTagId = null
        _statusMessage.value = "Aproxime a tag NFC para atribuir chave"
    }

    fun openLock() {
        if (assignedTagId == null) {
            _statusMessage.value = "Atribua uma chave primeiro"
        } else {
            _statusMessage.value = "Aproxime a tag NFC para abrir o cadeado"
            startTimer()
        }
    }

    fun closeLock() {
        _isLockOpen.value = false
        _statusMessage.value = "Cadeado fechado"
    }

    fun processTag(tag: Tag) {
        val tagId = tag.id.joinToString(separator = "") { String.format("%02X", it) }
        Log.d("NFC_TAG", "Tag ID: $tagId")
        if (assignedTagId == null) {
            assignedTagId = tagId
            _statusMessage.value = "Chave atribuída com sucesso!"
        } else {
            if (tagId == assignedTagId) {
                _statusMessage.value = "Cadeado aberto com sucesso!"
                _isLockOpen.value = true
            } else {
                _statusMessage.value = "Chave incorreta!"
                mediaPlayer.start()
            }
        }
    }

    fun updateNfcSupport(isSupported: Boolean) {
        _isNfcSupported.value = isSupported
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (i in 30 downTo 1) {
                delay(1000L)
                _statusMessage.value = "Tempo restante: $i s"
            }
            _statusMessage.value = "Tempo esgotado, ação cancelada."
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
    }
}
