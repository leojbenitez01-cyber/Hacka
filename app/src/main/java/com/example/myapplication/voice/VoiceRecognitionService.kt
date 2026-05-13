package com.example.myapplication.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class VoiceRecognitionService(private val context: Context) : VoiceApiService {

    private var recognizer: SpeechRecognizer? = null
    private var commandListener: ((VoiceCommand) -> Unit)? = null
    private var active = false

    override fun startListening() {
        if (active) return
        active = true
        startCycle()
    }

    override fun stopListening() {
        active = false
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }

    override fun isListening() = active

    override fun setCommandListener(listener: (VoiceCommand) -> Unit) {
        commandListener = listener
    }

    private fun startCycle() {
        if (!active) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(v: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(r: Bundle?) {}
                override fun onEvent(t: Int, p: Bundle?) {}

                override fun onResults(results: Bundle?) {
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.let { parseAndDispatch(it) }
                    startCycle()
                }

                override fun onError(error: Int) {
                    // reiniciar en cualquier error para escucha continua
                    startCycle()
                }
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        recognizer?.startListening(intent)
    }

    private fun parseAndDispatch(text: String) {
        val t = text.lowercase().trim()
        val cmd = when {
            t.contains("siguiente") || t.contains("avanzar") || t.contains("adelante") ->
                VoiceCommand.Next
            t.contains("anterior") || t.contains("atrás") || t.contains("atras") ||
            t.contains("regresar") || t.contains("volver") ->
                VoiceCommand.Previous
            t.contains("repetir") || t.contains("repite") || t.contains("de nuevo") ->
                VoiceCommand.Repeat
            t.contains("zoom") || t.contains("acercar") ->
                VoiceCommand.Zoom
            t.contains("rotar") || t.contains("girar") ->
                VoiceCommand.Rotate
            t.contains("pausa") || t.contains("pausar") ->
                VoiceCommand.Pause
            t.contains("parar") || t.contains("stop") || t.contains("detener") ->
                VoiceCommand.Stop
            else -> VoiceCommand.Custom(text)
        }
        commandListener?.invoke(cmd)
    }
}
