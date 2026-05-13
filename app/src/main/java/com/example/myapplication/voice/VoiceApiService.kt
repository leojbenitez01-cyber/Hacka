package com.example.myapplication.voice

/**
 * Contrato que deberá implementar la API de detección de voz externa.
 *
 * TODO: Cuando la API esté lista, crear una clase que implemente esta interfaz
 *       y reemplazar VoiceApiServiceStub en ar_scan.kt y View_3d.kt.
 */
interface VoiceApiService {
    fun startListening()
    fun stopListening()
    fun isListening(): Boolean
    fun setCommandListener(listener: (VoiceCommand) -> Unit)
}

/**
 * Stub temporal — no hace nada real.
 * Sirve para que el resto del código compile y funcione sin la API.
 *
 * Para probar sin la API, llama a [triggerTestCommand] manualmente
 * (por ejemplo desde un botón oculto de debug).
 */
class VoiceApiServiceStub : VoiceApiService {

    private var commandListener: ((VoiceCommand) -> Unit)? = null
    private var listening = false

    override fun startListening() {
        listening = true
        // TODO: iniciar sesión con la API de voz
    }

    override fun stopListening() {
        listening = false
        // TODO: cerrar sesión con la API de voz
    }

    override fun isListening() = listening

    override fun setCommandListener(listener: (VoiceCommand) -> Unit) {
        commandListener = listener
    }

    /** Solo para pruebas: dispara un comando como si viniera de la API. */
    fun triggerTestCommand(command: VoiceCommand) {
        commandListener?.invoke(command)
    }
}
