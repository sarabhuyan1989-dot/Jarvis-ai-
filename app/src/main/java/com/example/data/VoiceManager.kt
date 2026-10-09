package com.example.data

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class DetectedVoice(
    val id: String,
    val name: String,
    val localeTag: String,
    val gender: String, // "Male", "Female", or "Documentary / Standard"
    val quality: String,
    val isOdia: Boolean
)

class VoiceManager(
    private val context: Context,
    private val onTranscriptionFinal: (String, SpeechConfidence) -> Unit,
    private val onTranscriptionPartial: (String) -> Unit,
    private val onTtsFinished: () -> Unit,
    private val onErrorLog: (String) -> Unit,
    private val onStatusLog: (String) -> Unit = {}
) : TextToSpeech.OnInitListener {

    companion object {
        const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
        const val ODIA_LANGUAGE_TAG = "or-IN"
        const val ODIA_SAMPLE_PHRASE = "ମୁଁ ଭଲ ଅଛି ବସ୍! ତୁମେ କେମିତି ଅଛ? ଆଜି ତୁମ ପାଇଁ କଣ କରିପାରିବି?"
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    // Engine and voice state
    private val _engineName = MutableStateFlow("Google Speech Services")
    val engineName: StateFlow<String> = _engineName.asStateFlow()

    private val _activeVoiceName = MutableStateFlow("Odia Neutral Voice")
    val activeVoiceName: StateFlow<String> = _activeVoiceName.asStateFlow()

    private val _isOdiaVoiceActive = MutableStateFlow(false)
    val isOdiaVoiceActive: StateFlow<Boolean> = _isOdiaVoiceActive.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<DetectedVoice>>(emptyList())
    val availableVoices: StateFlow<List<DetectedVoice>> = _availableVoices.asStateFlow()

    private val _speechRate = MutableStateFlow(0.95f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(1.0f)
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    // Phonetic speech recognition confidence
    private val _lastConfidence = MutableStateFlow<SpeechConfidence?>(null)
    val lastConfidence: StateFlow<SpeechConfidence?> = _lastConfidence.asStateFlow()

    private var currentLanguageTag: String = ODIA_LANGUAGE_TAG

    init {
        initHighQualityTts()
    }

    private fun initHighQualityTts() {
        try {
            textToSpeech = TextToSpeech(context, this, GOOGLE_TTS_PACKAGE)
            _engineName.value = "Google Speech Services ($GOOGLE_TTS_PACKAGE)"
        } catch (e: Exception) {
            Log.w("VoiceManager", "Preferred engine $GOOGLE_TTS_PACKAGE failed, falling back: ${e.message}")
            try {
                textToSpeech = TextToSpeech(context, this)
                _engineName.value = "System Default TTS Engine"
            } catch (fallbackEx: Exception) {
                Log.e("VoiceManager", "TTS initialization failed: ${fallbackEx.message}", fallbackEx)
                _engineName.value = "TTS Initialization Error"
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val tts = textToSpeech ?: return
            isTtsReady = true

            val currentEngine = tts.defaultEngine ?: GOOGLE_TTS_PACKAGE
            _engineName.value = currentEngine
            onStatusLog("TTS ENGINE ACTIVE: $currentEngine")

            try {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .build()
                tts.setAudioAttributes(audioAttributes)
            } catch (e: Exception) {
                Log.w("VoiceManager", "Could not set custom AudioAttributes: ${e.message}")
            }

            tts.setSpeechRate(_speechRate.value)
            tts.setPitch(_speechPitch.value)

            scanAndConfigureVoices(tts, currentLanguageTag)

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    mainHandler.post {
                        _isSpeaking.value = true
                    }
                }

                override fun onDone(utteranceId: String?) {
                    mainHandler.post {
                        _isSpeaking.value = false
                        onTtsFinished()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    mainHandler.post {
                        _isSpeaking.value = false
                        onTtsFinished()
                    }
                }
            })
        } else {
            Log.e("VoiceManager", "TTS initialization failed with status: $status")
            _engineName.value = "TTS Error ($status)"
        }
    }

    private fun scanAndConfigureVoices(tts: TextToSpeech, targetLanguageTag: String) {
        val detectedList = mutableListOf<DetectedVoice>()
        var bestOdiaVoice: Voice? = null

        try {
            val allVoices = tts.voices
            if (!allVoices.isNullOrEmpty()) {
                for (v in allVoices) {
                    val tag = v.locale.toLanguageTag().lowercase()
                    val lang = v.locale.language.lowercase()
                    val name = v.name.lowercase()

                    val isOdia = tag.startsWith("or") || lang == "or" || lang == "ori" || name.contains("or-in") || name.contains("ori")
                    val isRelevant = isOdia || tag.startsWith("en-in") || tag.startsWith("hi-in")

                    if (isRelevant && !v.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) {
                        val gender = when {
                            v.features.contains("male") || name.contains("male") || name.contains("#male") -> "Male"
                            v.features.contains("female") || name.contains("female") || name.contains("#female") -> "Female"
                            else -> "Standard / Documentary"
                        }

                        val quality = when (v.quality) {
                            Voice.QUALITY_VERY_HIGH -> "Neural / HD"
                            Voice.QUALITY_HIGH -> "High"
                            Voice.QUALITY_NORMAL -> "Normal"
                            else -> "Standard"
                        }

                        detectedList.add(
                            DetectedVoice(
                                id = v.name,
                                name = v.name,
                                localeTag = v.locale.toLanguageTag(),
                                gender = gender,
                                quality = quality,
                                isOdia = isOdia
                            )
                        )

                        if (isOdia && (bestOdiaVoice == null || v.quality > bestOdiaVoice.quality)) {
                            bestOdiaVoice = v
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("VoiceManager", "Error reading tts.voices: ${e.message}")
        }

        _availableVoices.value = detectedList

        if (bestOdiaVoice != null) {
            try {
                tts.voice = bestOdiaVoice
                val genderDesc = if (bestOdiaVoice.features.contains("male")) "Male" else "Documentary"
                _activeVoiceName.value = "${bestOdiaVoice.name} ($genderDesc, HD)"
                _isOdiaVoiceActive.value = true
                onStatusLog("ODIA VOICE ACTIVE: ${bestOdiaVoice.name} ($genderDesc)")
                return
            } catch (e: Exception) {
                Log.w("VoiceManager", "Error setting best voice: ${e.message}")
            }
        }

        val locale = Locale.forLanguageTag(targetLanguageTag)
        val result = tts.setLanguage(locale)
        if (result >= TextToSpeech.LANG_AVAILABLE) {
            _activeVoiceName.value = "${locale.displayLanguage} (System Voice)"
            _isOdiaVoiceActive.value = targetLanguageTag.startsWith("or")
            onStatusLog("LOCALE SET: ${locale.displayName}")
        } else {
            val fallback = Locale.forLanguageTag("en-IN")
            tts.setLanguage(fallback)
            _activeVoiceName.value = "English (India) [Odia pack download recommended]"
            _isOdiaVoiceActive.value = false
            onStatusLog("NOTICE: Odia voice pack not detected. Downloading via TTS Settings recommended.")
        }
    }

    fun setLanguage(languageTag: String) {
        currentLanguageTag = languageTag
        val tts = textToSpeech ?: return
        if (isTtsReady) {
            scanAndConfigureVoices(tts, languageTag)
        }
    }

    fun selectVoiceByName(voiceName: String) {
        val tts = textToSpeech ?: return
        try {
            val voice = tts.voices?.firstOrNull { it.name == voiceName }
            if (voice != null) {
                tts.voice = voice
                _activeVoiceName.value = voice.name
                _isOdiaVoiceActive.value = voice.locale.toLanguageTag().startsWith("or")
                onStatusLog("VOICE SELECTED: ${voice.name}")
            }
        } catch (e: Exception) {
            Log.e("VoiceManager", "Failed to select voice: ${e.message}")
        }
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 1.5f)
        _speechRate.value = clamped
        textToSpeech?.setSpeechRate(clamped)
    }

    fun setSpeechPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 1.5f)
        _speechPitch.value = clamped
        textToSpeech?.setPitch(clamped)
    }

    fun testVoicePhrase() {
        speak(ODIA_SAMPLE_PHRASE)
    }

    fun openTtsSettings() {
        try {
            val intent = Intent("com.android.settings.TTS_SETTINGS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                onErrorLog("CANNOT OPEN SETTINGS: ${e.message}")
            }
        }
    }

    fun interruptSpeakingAndListen() {
        stopTts()
        mainHandler.postDelayed({
            startListening()
        }, 100)
    }

    fun startListening() {
        if (_isListening.value) return
        stopTts()

        mainHandler.post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    onErrorLog("Speech recognition service not available on this device")
                    return@post
                }

                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isListening.value = true
                        }

                        override fun onBeginningOfSpeech() {
                            _isListening.value = true
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                            _audioAmplitude.value = normalized
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _audioAmplitude.value = 0f
                        }

                        override fun onError(error: Int) {
                            _isListening.value = false
                            _audioAmplitude.value = 0f
                            val errorDesc = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "AUDIO RECORDING ERROR"
                                SpeechRecognizer.ERROR_CLIENT -> "CLIENT ERROR"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "MIC PERMISSION REQUIRED"
                                SpeechRecognizer.ERROR_NETWORK -> "NETWORK TIMEOUT"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "NETWORK TIMEOUT"
                                SpeechRecognizer.ERROR_NO_MATCH -> "NO ODIA SPEECH DETECTED"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RECOGNIZER BUSY"
                                SpeechRecognizer.ERROR_SERVER -> "SPEECH SERVER ERROR"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "SPEECH TIMEOUT"
                                else -> "VOICE ERROR $error"
                            }
                            onErrorLog(errorDesc)
                        }

                        override fun onResults(results: Bundle?) {
                            _isListening.value = false
                            _audioAmplitude.value = 0f
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val scores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                            val recognizedText = matches?.firstOrNull()?.trim()

                            if (!recognizedText.isNullOrBlank()) {
                                // Analyze recognition confidence scores for Odia speech input
                                val rawScore = scores?.firstOrNull() ?: -1f
                                val computedConfidence = when {
                                    rawScore in 0.0f..1.0f -> rawScore
                                    rawScore < 0.0f && rawScore >= -10.0f -> kotlin.math.exp(rawScore.toDouble()).toFloat().coerceIn(0.1f, 1.0f)
                                    matches.size > 2 && matches[0] != matches[1] -> 0.42f // Ambiguity among multiple interpretations
                                    matches.size == 2 && matches[0] != matches[1] -> 0.62f
                                    else -> 0.88f // High confidence default
                                }

                                val confidenceObj = SpeechConfidence(score = computedConfidence)
                                _lastConfidence.value = confidenceObj

                                // Provide haptic feedback based on pronunciation clarity
                                triggerHapticFeedback(confidenceObj.isUnclear)

                                if (confidenceObj.isUnclear) {
                                    onStatusLog("PHONETIC ALERT: Odia pronunciation confidence low (${confidenceObj.percentage}%). Context recovery active.")
                                } else {
                                    onStatusLog("PHONETIC ANALYSIS: Clear speech (${confidenceObj.percentage}%)")
                                }

                                onTranscriptionFinal(recognizedText, confidenceObj)
                            } else {
                                onErrorLog("NO TRANSCRIPTION DETECTED")
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partialText = matches?.firstOrNull()?.trim()
                            if (!partialText.isNullOrBlank()) {
                                onTranscriptionPartial(partialText)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguageTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguageTag)
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("or-IN", "en-IN", "hi-IN"))
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {
                Log.e("VoiceManager", "Error starting speech recognition: ${e.message}", e)
                _isListening.value = false
                onErrorLog("MIC ERROR: ${e.message}")
            }
        }
    }

    /**
     * Provides distinct haptic feedback:
     * - Warning double-pulse vibration if pronunciation was unclear to the AI.
     * - Subtle light tap confirmation if pronunciation was clear.
     */
    private fun triggerHapticFeedback(isUnclear: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isUnclear) {
                    // Double warning pulse: buzz-pause-buzz
                    val timings = longArrayOf(0, 80, 60, 140)
                    val amplitudes = intArrayOf(0, 190, 0, 255)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    // Subtle light confirmation click
                    vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } else {
                @Suppress("DEPRECATION")
                if (isUnclear) {
                    vibrator.vibrate(longArrayOf(0, 80, 60, 140), -1)
                } else {
                    vibrator.vibrate(35)
                }
            }
        } catch (e: Exception) {
            Log.w("VoiceManager", "Haptic vibrator unavailable: ${e.message}")
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            _isListening.value = false
            _audioAmplitude.value = 0f
        }
    }

    fun speak(text: String) {
        if (!isTtsReady) {
            Log.w("VoiceManager", "TTS not ready, bypassing audio playback")
            onTtsFinished()
            return
        }

        stopListening()
        mainHandler.post {
            try {
                val utteranceId = "JARVIS_${System.currentTimeMillis()}"
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            } catch (e: Exception) {
                Log.e("VoiceManager", "TTS speech failed: ${e.message}", e)
                _isSpeaking.value = false
                onTtsFinished()
            }
        }
    }

    fun stopTts() {
        mainHandler.post {
            try {
                textToSpeech?.stop()
            } catch (_: Exception) {}
            _isSpeaking.value = false
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}

        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (_: Exception) {}
    }
}
