package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Text-To-Speech Manager for Foundry Voice Conversations.
 */
class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false
    private var pendingText: String? = null

    var onStateChanged: ((isSpeaking: Boolean) -> Unit)? = null

    init {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onStateChanged?.invoke(true)
            }

            override fun onDone(utteranceId: String?) {
                onStateChanged?.invoke(false)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onStateChanged?.invoke(false)
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            isInitialized = true
            pendingText?.let { text ->
                speak(text)
                pendingText = null
            }
        } else {
            Log.e("TtsManager", "TextToSpeech initialization failed with status $status")
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (!isInitialized) {
            pendingText = text
            return
        }
        val cleanText = text
            .replace(Regex("""[*_#`\[\]]"""), "") // Remove markdown symbols for natural reading
            .take(1000) // Keep speech concise for optimal audio UX

        tts?.stop()
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "FoundryUtterance")
    }

    fun stop() {
        pendingText = null
        tts?.stop()
        onStateChanged?.invoke(false)
    }

    fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
