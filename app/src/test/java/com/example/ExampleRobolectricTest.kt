package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ConfidenceLevel
import com.example.data.DeviceAction
import com.example.data.GeminiService
import com.example.data.SpeechConfidence
import com.example.data.VoiceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Jarvis", appName)
    }

    @Test
    fun `test odia high quality voice configuration constants`() {
        assertEquals("or-IN", VoiceManager.ODIA_LANGUAGE_TAG)
        assertEquals("com.google.android.tts", VoiceManager.GOOGLE_TTS_PACKAGE)
        val locale = Locale.forLanguageTag(VoiceManager.ODIA_LANGUAGE_TAG)
        assertEquals("or", locale.language)
        assertEquals("IN", locale.country)
        assertNotNull(VoiceManager.ODIA_SAMPLE_PHRASE)
        assertTrue(VoiceManager.ODIA_SAMPLE_PHRASE.contains("ବସ୍"))
    }

    @Test
    fun `test speech confidence score analysis and unclear threshold`() {
        val highConfidence = SpeechConfidence(score = 0.92f)
        assertEquals(92, highConfidence.percentage)
        assertEquals(ConfidenceLevel.OPTIMAL, highConfidence.level)
        assertFalse(highConfidence.isUnclear)

        val moderateConfidence = SpeechConfidence(score = 0.58f)
        assertEquals(58, moderateConfidence.percentage)
        assertEquals(ConfidenceLevel.FAIR, moderateConfidence.level)
        assertFalse(moderateConfidence.isUnclear)

        val lowConfidence = SpeechConfidence(score = 0.31f)
        assertEquals(31, lowConfidence.percentage)
        assertEquals(ConfidenceLevel.LOW_UNCLEAR, lowConfidence.level)
        assertTrue(lowConfidence.isUnclear)
    }

    @Test
    fun `test odia greeting conversation`() {
        val geminiService = GeminiService()
        val result = geminiService.parseOdiaIntentLocally("ଜାରଭିସ୍, ତୁମେ କେମିତି ଅଛ?")
        assertEquals(DeviceAction.NONE.key, result.action)
        assertTrue(result.reply.contains("ଭଲ ଅଛି ବସ୍"))
    }

    @Test
    fun `test odia stop speaking action`() {
        val geminiService = GeminiService()
        val result = geminiService.parseOdiaIntentLocally("ଜାରଭିସ୍, ଏବେ ଚୁପ୍ ରୁହ")
        assertEquals(DeviceAction.STOP_SPEAKING.key, result.action)
        assertTrue(result.reply.contains("ଚୁପ୍"))
    }

    @Test
    fun `test odia tell time action`() {
        val geminiService = GeminiService()
        val result = geminiService.parseOdiaIntentLocally("ଏବେ ସମୟ କେତେ?")
        assertEquals(DeviceAction.TELL_TIME.key, result.action)
        assertTrue(result.reply.contains("ସମୟ"))
    }

    @Test
    fun `test odia set timer action`() {
        val geminiService = GeminiService()
        val result = geminiService.parseOdiaIntentLocally("ଟାଇମର୍ ସେଟ୍ କର")
        assertEquals(DeviceAction.SET_TIMER.key, result.action)
    }

    @Test
    fun `test odia youtube intent parsing`() {
        val geminiService = GeminiService()
        val result = geminiService.parseOdiaIntentLocally("ୟୁଟ୍ୟୁବ୍ ଖୋଲନ୍ତୁ")
        assertEquals(DeviceAction.OPEN_YOUTUBE.key, result.action)
        assertTrue(result.reply.contains("ୟୁଟ୍ୟୁବ୍"))
    }

    @Test
    fun `test odia camera intent parsing`() {
        val geminiService = GeminiService()
        val result = geminiService.parseOdiaIntentLocally("କ୍ୟାମେରା ଖୋଲନ୍ତୁ")
        assertEquals(DeviceAction.OPEN_CAMERA.key, result.action)
    }

    @Test
    fun `test odia settings intent parsing`() {
        val geminiService = GeminiService()
        val result = geminiService.parseOdiaIntentLocally("ସେଟିଙ୍ଗସ୍ ଖୋଲନ୍ତୁ")
        assertEquals(DeviceAction.OPEN_SETTINGS.key, result.action)
    }
}
