package com.example.data

data class JarvisResponse(
    val reply: String,
    val action: String,
    val rawJson: String? = null,
    val isFallback: Boolean = false
)

enum class PreferredLanguage(val tag: String, val displayName: String, val speechPrompt: String) {
    ODIA("or-IN", "ଓଡ଼ିଆ (Odia)", "The user's preferred language is Odia."),
    ENGLISH("en-IN", "English (India)", "The user's preferred language is English."),
    HINDI("hi-IN", "हिन्दी (Hindi)", "The user's preferred language is Hindi.");

    companion object {
        fun fromTag(tag: String?): PreferredLanguage {
            return entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: ODIA
        }
    }
}

enum class DeviceAction(val key: String, val title: String, val iconDescription: String) {
    OPEN_YOUTUBE("OPEN_YOUTUBE", "YouTube", "Launch YouTube application"),
    OPEN_SETTINGS("OPEN_SETTINGS", "Settings", "Open Android System Settings"),
    OPEN_CAMERA("OPEN_CAMERA", "Camera", "Launch Camera application"),
    OPEN_DIALER("OPEN_DIALER", "Phone Dialer", "Open Phone dialpad"),
    OPEN_BROWSER("OPEN_BROWSER", "Web Browser", "Open Web Browser"),
    OPEN_MAPS("OPEN_MAPS", "Maps", "Open Navigation Maps"),
    OPEN_CALCULATOR("OPEN_CALCULATOR", "Calculator", "Launch Calculator app"),
    SET_TIMER("SET_TIMER", "Timer", "Set a timer or alarm"),
    TELL_TIME("TELL_TIME", "Current Time", "Announce current system time"),
    STOP_SPEAKING("STOP_SPEAKING", "Stop Speaking", "Stop assistant speech output immediately"),
    GO_HOME("GO_HOME", "Home Screen", "Return to device home screen"),
    NONE("NONE", "None", "Spoken conversation only");

    companion object {
        fun fromString(actionStr: String?): DeviceAction {
            if (actionStr.isNullOrBlank()) return NONE
            val clean = actionStr.trim().uppercase()
            return entries.firstOrNull { it.key == clean } ?: NONE
        }
    }
}

enum class JarvisState(val label: String) {
    IDLE("READY"),
    LISTENING("LISTENING"),
    PROCESSING("THINKING"),
    SPEAKING("SPEAKING"),
    EXECUTING_ACTION("ACTION"),
    ERROR("ERROR")
}

data class ConversationMessage(
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ConfidenceLevel(val label: String, val thresholdDesc: String) {
    OPTIMAL("OPTIMAL", "Crystal Clear"),
    FAIR("FAIR", "Moderate"),
    LOW_UNCLEAR("UNCLEAR", "Low Confidence / Phonetic Alert")
}

data class SpeechConfidence(
    val score: Float,
    val percentage: Int = (score.coerceIn(0f, 1f) * 100).toInt(),
    val level: ConfidenceLevel = when {
        score >= 0.70f -> ConfidenceLevel.OPTIMAL
        score >= 0.45f -> ConfidenceLevel.FAIR
        else -> ConfidenceLevel.LOW_UNCLEAR
    },
    val isUnclear: Boolean = score < 0.45f
)

data class TerminalLog(
    val id: Long = System.currentTimeMillis(),
    val timestamp: String,
    val type: LogType,
    val message: String,
    val action: DeviceAction? = null
)

enum class LogType {
    SYSTEM,
    USER,
    JARVIS,
    ACTION,
    ERROR
}
