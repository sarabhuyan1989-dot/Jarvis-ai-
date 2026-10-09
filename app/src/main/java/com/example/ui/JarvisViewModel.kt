package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ConversationMessage
import com.example.data.DetectedVoice
import com.example.data.DeviceAction
import com.example.data.DeviceActionController
import com.example.data.GeminiService
import com.example.data.JarvisResponse
import com.example.data.JarvisState
import com.example.data.LogType
import com.example.data.PreferredLanguage
import com.example.data.TerminalLog
import com.example.data.VoiceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    private val geminiService = GeminiService()
    private val actionController = DeviceActionController(application.applicationContext)

    private val _state = MutableStateFlow(JarvisState.IDLE)
    val state: StateFlow<JarvisState> = _state.asStateFlow()

    private val _currentTranscript = MutableStateFlow("")
    val currentTranscript: StateFlow<String> = _currentTranscript.asStateFlow()

    private val _lastResponse = MutableStateFlow<JarvisResponse?>(null)
    val lastResponse: StateFlow<JarvisResponse?> = _lastResponse.asStateFlow()

    private val _pendingAction = MutableStateFlow<DeviceAction?>(null)
    val pendingAction: StateFlow<DeviceAction?> = _pendingAction.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<TerminalLog>>(emptyList())
    val terminalLogs: StateFlow<List<TerminalLog>> = _terminalLogs.asStateFlow()

    // Multi-turn conversational session memory
    private val _conversationHistory = MutableStateFlow<List<ConversationMessage>>(emptyList())
    val conversationHistory: StateFlow<List<ConversationMessage>> = _conversationHistory.asStateFlow()

    // Hands-Free Continuous Voice Mode
    private val _isContinuousMode = MutableStateFlow(prefs.getBoolean("continuous_mode", false))
    val isContinuousMode: StateFlow<Boolean> = _isContinuousMode.asStateFlow()

    // Preferred Language
    private val _preferredLanguage = MutableStateFlow(
        PreferredLanguage.fromTag(prefs.getString("pref_lang", "or-IN"))
    )
    val preferredLanguage: StateFlow<PreferredLanguage> = _preferredLanguage.asStateFlow()

    private val _customApiKey = MutableStateFlow(prefs.getString("custom_api_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _autoExecuteAfterTts = MutableStateFlow(prefs.getBoolean("auto_execute", true))
    val autoExecuteAfterTts: StateFlow<Boolean> = _autoExecuteAfterTts.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    // High-quality TTS State
    private val _ttsEngineName = MutableStateFlow("Google Speech Services")
    val ttsEngineName: StateFlow<String> = _ttsEngineName.asStateFlow()

    private val _activeVoiceName = MutableStateFlow("Odia Neutral Voice")
    val activeVoiceName: StateFlow<String> = _activeVoiceName.asStateFlow()

    private val _isOdiaVoiceActive = MutableStateFlow(true)
    val isOdiaVoiceActive: StateFlow<Boolean> = _isOdiaVoiceActive.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<DetectedVoice>>(emptyList())
    val availableVoices: StateFlow<List<DetectedVoice>> = _availableVoices.asStateFlow()

    private val _speechRate = MutableStateFlow(prefs.getFloat("speech_rate", 0.95f))
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(prefs.getFloat("speech_pitch", 1.0f))
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    // Phonetic Speech Recognition Confidence Analysis
    private val _speechConfidence = MutableStateFlow<com.example.data.SpeechConfidence?>(null)
    val speechConfidence: StateFlow<com.example.data.SpeechConfidence?> = _speechConfidence.asStateFlow()

    private var voiceManager: VoiceManager? = null
    private var processJob: Job? = null

    init {
        initVoiceManager()
        addLog(LogType.SYSTEM, "JARVIS MK-IV ONLINE // VOICE PROTOCOL ACTIVE")
        addLog(LogType.SYSTEM, "CORE LANGUAGE: ${_preferredLanguage.value.displayName}")
    }

    private fun initVoiceManager() {
        voiceManager = VoiceManager(
            context = getApplication(),
            onTranscriptionFinal = { text, confidence ->
                viewModelScope.launch {
                    _currentTranscript.value = text
                    _speechConfidence.value = confidence
                    addLog(LogType.USER, text)
                    if (confidence.isUnclear) {
                        addLog(LogType.SYSTEM, "⚠ PHONETIC ALERT: Pronunciation clarity low (${confidence.percentage}%). Context recovery engaged.")
                    } else {
                        addLog(LogType.SYSTEM, "✓ PHONETIC CLARITY: ${confidence.percentage}% [${confidence.level.label}]")
                    }
                    processSpokenOdiaInput(text, confidence)
                }
            },
            onTranscriptionPartial = { partial ->
                _currentTranscript.value = partial
            },
            onTtsFinished = {
                viewModelScope.launch {
                    // Check if action execution is requested
                    if (_autoExecuteAfterTts.value && _pendingAction.value != null && _pendingAction.value != DeviceAction.NONE) {
                        executePendingAction()
                    }

                    // If Continuous Hands-Free Voice Mode is active, resume listening!
                    if (_isContinuousMode.value) {
                        _state.value = JarvisState.IDLE
                        // Pause briefly for echo cancellation and ambient silence
                        delay(500)
                        if (_isContinuousMode.value && _state.value == JarvisState.IDLE) {
                            addLog(LogType.SYSTEM, "CONTINUOUS MODE: RESUMING MIC...")
                            _state.value = JarvisState.LISTENING
                            voiceManager?.startListening()
                        }
                    } else {
                        _state.value = JarvisState.IDLE
                    }
                }
            },
            onErrorLog = { errorMsg ->
                viewModelScope.launch {
                    _state.value = JarvisState.IDLE
                    addLog(LogType.ERROR, errorMsg)
                    // If in continuous mode and error was just silence, retry after brief delay
                    if (_isContinuousMode.value && errorMsg.contains("NO ODIA SPEECH")) {
                        delay(1200)
                        if (_isContinuousMode.value && _state.value == JarvisState.IDLE) {
                            voiceManager?.startListening()
                        }
                    }
                }
            },
            onStatusLog = { statusMsg ->
                viewModelScope.launch {
                    addLog(LogType.SYSTEM, statusMsg)
                }
            }
        ).apply {
            setLanguage(_preferredLanguage.value.tag)
            setSpeechRate(_speechRate.value)
            setSpeechPitch(_speechPitch.value)
        }

        viewModelScope.launch {
            voiceManager?.audioAmplitude?.collect { amp ->
                _audioAmplitude.value = amp
            }
        }

        viewModelScope.launch {
            voiceManager?.engineName?.collect { name ->
                _ttsEngineName.value = name
            }
        }

        viewModelScope.launch {
            voiceManager?.activeVoiceName?.collect { name ->
                _activeVoiceName.value = name
            }
        }

        viewModelScope.launch {
            voiceManager?.isOdiaVoiceActive?.collect { active ->
                _isOdiaVoiceActive.value = active
            }
        }

        viewModelScope.launch {
            voiceManager?.availableVoices?.collect { list ->
                _availableVoices.value = list
            }
        }

        viewModelScope.launch {
            voiceManager?.isListening?.collect { listening ->
                if (listening) {
                    _state.value = JarvisState.LISTENING
                } else if (_state.value == JarvisState.LISTENING) {
                    _state.value = JarvisState.IDLE
                }
            }
        }

        viewModelScope.launch {
            voiceManager?.isSpeaking?.collect { speaking ->
                if (speaking) {
                    _state.value = JarvisState.SPEAKING
                } else if (_state.value == JarvisState.SPEAKING) {
                    _state.value = JarvisState.IDLE
                }
            }
        }
    }

    /**
     * Primary mic trigger button:
     * - If currently speaking: interrupts speech immediately and starts listening.
     * - If currently listening: stops listening.
     * - If idle/ready: begins listening in Odia.
     */
    fun onMicButtonClicked() {
        when (_state.value) {
            JarvisState.SPEAKING -> {
                // User interruption
                interruptSpeakingAndListen()
            }
            JarvisState.LISTENING -> {
                voiceManager?.stopListening()
                _state.value = JarvisState.IDLE
                addLog(LogType.SYSTEM, "VOICE STREAM STOPPED")
            }
            else -> {
                _currentTranscript.value = ""
                _state.value = JarvisState.LISTENING
                addLog(LogType.SYSTEM, "LISTENING [${_preferredLanguage.value.displayName}]...")
                voiceManager?.startListening()
            }
        }
    }

    fun interruptSpeakingAndListen() {
        addLog(LogType.SYSTEM, "USER INTERRUPTED JARVIS")
        voiceManager?.interruptSpeakingAndListen()
    }

    fun stopSpeaking() {
        voiceManager?.stopTts()
        _state.value = JarvisState.IDLE
        addLog(LogType.SYSTEM, "SPEECH OUTPUT TERMINATED")
    }

    fun toggleContinuousMode() {
        val next = !_isContinuousMode.value
        _isContinuousMode.value = next
        prefs.edit().putBoolean("continuous_mode", next).apply()
        addLog(LogType.SYSTEM, if (next) "HANDS-FREE CONTINUOUS MODE: ENABLED" else "HANDS-FREE CONTINUOUS MODE: DISABLED")

        if (next && _state.value == JarvisState.IDLE) {
            onMicButtonClicked()
        }
    }

    fun setPreferredLanguage(language: PreferredLanguage) {
        _preferredLanguage.value = language
        prefs.edit().putString("pref_lang", language.tag).apply()
        voiceManager?.setLanguage(language.tag)
        addLog(LogType.SYSTEM, "PREFERRED LANGUAGE UPDATED: ${language.displayName}")
    }

    fun selectVoice(voiceId: String) {
        voiceManager?.selectVoiceByName(voiceId)
    }

    fun processSpokenOdiaInput(input: String, confidence: com.example.data.SpeechConfidence? = null) {
        if (input.isBlank()) return
        processJob?.cancel()

        if (confidence != null) {
            _speechConfidence.value = confidence
        }

        // Append to session conversation history
        _conversationHistory.value = _conversationHistory.value + ConversationMessage("user", input)

        processJob = viewModelScope.launch {
            _state.value = JarvisState.PROCESSING
            _currentTranscript.value = input
            addLog(LogType.SYSTEM, "NEURAL PROCESSING VIA GEMINI...")

            val response = geminiService.analyzeOdiaInput(
                userInput = input,
                conversationHistory = _conversationHistory.value,
                preferredLanguage = _preferredLanguage.value,
                customApiKey = _customApiKey.value
            )

            _lastResponse.value = response
            val parsedAction = DeviceAction.fromString(response.action)
            _pendingAction.value = parsedAction

            // Append assistant reply to conversational memory
            _conversationHistory.value = _conversationHistory.value + ConversationMessage("model", response.reply)

            addLog(LogType.JARVIS, response.reply)
            if (parsedAction != DeviceAction.NONE) {
                addLog(LogType.ACTION, "INTENT: ${parsedAction.key}", parsedAction)
            }

            // If action is STOP_SPEAKING, immediately obey
            if (parsedAction == DeviceAction.STOP_SPEAKING) {
                stopSpeaking()
                return@launch
            }

            // Speak out answer aloud in natural Odia
            _state.value = JarvisState.SPEAKING
            voiceManager?.speak(response.reply)

            // Safety timeout in case TTS engine doesn't fire onDone
            delay(6000)
            if (_state.value == JarvisState.SPEAKING && _autoExecuteAfterTts.value) {
                executePendingAction()
            }
        }
    }

    fun executePendingAction() {
        val action = _pendingAction.value ?: return
        if (action == DeviceAction.NONE) {
            _state.value = JarvisState.IDLE
            return
        }

        _state.value = JarvisState.EXECUTING_ACTION
        addLog(LogType.SYSTEM, "EXECUTING DISPATCH: ${action.key}...")

        val result = actionController.executeAction(action)
        if (result.success) {
            addLog(LogType.ACTION, "✓ ${result.message}", action)
        } else {
            addLog(LogType.ERROR, "✗ ${result.message}", action)
        }

        _pendingAction.value = null
        _state.value = JarvisState.IDLE
    }

    fun speakLastResponse() {
        val reply = _lastResponse.value?.reply ?: return
        _state.value = JarvisState.SPEAKING
        voiceManager?.speak(reply)
    }

    fun testOdiaVoiceSample() {
        _state.value = JarvisState.SPEAKING
        addLog(LogType.SYSTEM, "TESTING ODIA VOICE OUTPUT...")
        voiceManager?.testVoicePhrase()
    }

    fun openSystemTtsSettings() {
        voiceManager?.openTtsSettings()
    }

    fun updateSpeechRate(rate: Float) {
        _speechRate.value = rate
        prefs.edit().putFloat("speech_rate", rate).apply()
        voiceManager?.setSpeechRate(rate)
    }

    fun updateSpeechPitch(pitch: Float) {
        _speechPitch.value = pitch
        prefs.edit().putFloat("speech_pitch", pitch).apply()
        voiceManager?.setSpeechPitch(pitch)
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
        prefs.edit().putString("custom_api_key", key).apply()
        addLog(LogType.SYSTEM, if (key.isNotBlank()) "CUSTOM GEMINI KEY SAVED" else "DEFAULT KEY RESTORED")
    }

    fun setAutoExecute(enabled: Boolean) {
        _autoExecuteAfterTts.value = enabled
        prefs.edit().putBoolean("auto_execute", enabled).apply()
        addLog(LogType.SYSTEM, "AUTO EXECUTE SET TO: $enabled")
    }

    fun clearConversation() {
        _conversationHistory.value = emptyList()
        _terminalLogs.value = emptyList()
        _currentTranscript.value = ""
        _lastResponse.value = null
        _pendingAction.value = null
        _speechConfidence.value = null
        addLog(LogType.SYSTEM, "CONVERSATION MEMORY & TERMINAL RESET")
    }

    private fun addLog(type: LogType, message: String, action: DeviceAction? = null) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val time = timeFormat.format(Date())
        val newLog = TerminalLog(
            timestamp = time,
            type = type,
            message = message,
            action = action
        )
        _terminalLogs.value = (_terminalLogs.value + newLog).takeLast(60)
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager?.destroy()
    }
}
