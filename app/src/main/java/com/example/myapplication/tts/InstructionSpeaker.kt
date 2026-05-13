package com.example.myapplication.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Envuelve Android TextToSpeech para leer las instrucciones de mantenimiento en voz alta.
 *
 * Uso:
 *   val speaker = InstructionSpeaker(context)
 *   speaker.speak("Retire el panel frontal")
 *   speaker.stop()
 *   speaker.release()   // llamar en onDestroy
 */
class InstructionSpeaker(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "MX"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback a español genérico
                tts?.setLanguage(Locale("es"))
            }
            isReady = true
        } else {
            Log.e("InstructionSpeaker", "Error al inicializar TTS, status=$status")
        }
    }

    /** Lee el texto en voz alta. Interrumpe cualquier lectura en curso. */
    fun speak(text: String) {
        if (!isReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "step_${System.currentTimeMillis()}")
    }

    /** Detiene la lectura actual. */
    fun stop() {
        tts?.stop()
    }

    /** Libera recursos. Llamar en onDestroy de la Activity. */
    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
