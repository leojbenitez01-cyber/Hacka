package com.example.myapplication.voice

/**
 * Comandos que la API de voz puede devolver.
 * Cuando la API externa esté lista, emitirá estos sealed classes.
 */
sealed class VoiceCommand {
    object Next     : VoiceCommand()   // "Siguiente"
    object Previous : VoiceCommand()   // "Anterior"
    object Pause    : VoiceCommand()   // "Pausa"
    object Repeat   : VoiceCommand()   // "Repetir"
    object Zoom     : VoiceCommand()   // "Zoom"
    object Rotate   : VoiceCommand()   // "Rotar"
    object Stop     : VoiceCommand()   // "Parar" / "Stop"
    data class Custom(val rawText: String) : VoiceCommand()
}
