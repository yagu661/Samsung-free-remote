package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    object Processing : VoiceState()
    data class Success(val recognizedText: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

data class SpeechLanguage(
    val code: String,
    val displayName: String
)

class VoiceInputManager(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val isDelivered = AtomicBoolean(false)

    companion object {
        val SUPPORTED_LANGUAGES = listOf(
            SpeechLanguage("en-US", "English (United States)"),
            SpeechLanguage("en-IN", "English (India)"),
            SpeechLanguage("bn-IN", "Bengali (বাংলা)"),
            SpeechLanguage("hi-IN", "Hindi (हिन्दी)"),
            SpeechLanguage("default", "System Default")
        )
    }

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(languageCode: String = "en-US", onResult: (String) -> Unit) {
        mainHandler.post {
            stopListeningInternal()
            isDelivered.set(false)
            _partialText.value = ""

            val recognizer = try {
                SpeechRecognizer.createSpeechRecognizer(context) ?: SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
            } catch (e: Exception) {
                Log.w("VoiceInputManager", "createSpeechRecognizer(context) failed, trying applicationContext", e)
                try {
                    SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                } catch (e2: Exception) {
                    Log.e("VoiceInputManager", "Failed to create SpeechRecognizer", e2)
                    null
                }
            }

            if (recognizer == null) {
                _voiceState.value = VoiceState.Error(
                    "Background speech service unavailable. Tap 'Google Voice Dialog' below to speak via standard Android dialog."
                )
                return@post
            }

            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _voiceState.value = VoiceState.Listening
                }

                override fun onBeginningOfSpeech() {
                    _voiceState.value = VoiceState.Listening
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _voiceState.value = VoiceState.Processing
                }

                override fun onError(error: Int) {
                    if (isDelivered.get()) return

                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check microphone."
                        SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error. Try speaking again."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                        SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout while processing speech."
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try speaking clearly."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy. Please try again."
                        SpeechRecognizer.ERROR_SERVER -> "Google Speech recognition server error."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap the mic to try again."
                        else -> "Speech recognition error (Code $error)"
                    }

                    _voiceState.value = VoiceState.Error(errorMsg)
                    stopListeningInternal()
                }

                override fun onResults(results: Bundle?) {
                    if (isDelivered.getAndSet(true)) return

                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim() ?: ""

                    if (text.isNotBlank()) {
                        _voiceState.value = VoiceState.Success(text)
                        _partialText.value = text
                        onResult(text)
                    } else {
                        _voiceState.value = VoiceState.Error("No speech detected.")
                    }
                    stopListeningInternal()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    if (isDelivered.get()) return
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim() ?: ""
                    if (text.isNotBlank()) {
                        _partialText.value = text
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                if (languageCode != "default") {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                } else {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                }
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            try {
                recognizer.startListening(intent)
                _voiceState.value = VoiceState.Listening
            } catch (e: Exception) {
                Log.e("VoiceInputManager", "Failed to start listening", e)
                _voiceState.value = VoiceState.Error("Could not start microphone: ${e.message}")
                stopListeningInternal()
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            stopListeningInternal()
            if (_voiceState.value is VoiceState.Listening) {
                _voiceState.value = VoiceState.Idle
            }
        }
    }

    fun cancelListening() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
            stopListeningInternal()
            _voiceState.value = VoiceState.Idle
            _partialText.value = ""
        }
    }

    private fun stopListeningInternal() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    fun reset() {
        cancelListening()
    }
}
