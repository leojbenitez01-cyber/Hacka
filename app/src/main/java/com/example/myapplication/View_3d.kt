package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.tts.InstructionSpeaker
import com.example.myapplication.voice.VoiceApiService
import com.example.myapplication.voice.VoiceApiServiceStub
import com.example.myapplication.voice.VoiceCommand
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.launch

class View_3d : AppCompatActivity() {

    // ── Vistas ────────────────────────────────────────────────────────────────
    private lateinit var sceneView: SceneView

    // ── Servicios ─────────────────────────────────────────────────────────────
    private val speaker by lazy { InstructionSpeaker(this) }

    // ── TODO: reemplazar stub cuando la API de voz esté lista ─────────────────
    private val voiceService: VoiceApiService = VoiceApiServiceStub()
    // ─────────────────────────────────────────────────────────────────────────

    // ── Estado ────────────────────────────────────────────────────────────────
    private var modelNode: ModelNode? = null
    private var zoomScale = 1.0f

    companion object {
        const val MODEL_FILE = "models/machine.glb"
        private const val ZOOM_STEP = 0.15f
        private const val ZOOM_MIN  = 0.3f
        private const val ZOOM_MAX  = 3.0f
    }

    // ═══════════════════════════════════════════════════════════════════════════
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view3d)

        sceneView = findViewById(R.id.sceneView)
        lifecycle.addObserver(sceneView)

        bindButtons()
        setupBottomNav()
        setupVoiceCommands()
        loadModel()
    }

    // ── Modelo 3D ─────────────────────────────────────────────────────────────
    private fun loadModel() {
        lifecycleScope.launch {
            val instance = sceneView.modelLoader
                .createModelInstance(MODEL_FILE)
                ?: run {
                    Toast.makeText(
                        this@View_3d,
                        "No se encontró $MODEL_FILE en assets/",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

            val node = ModelNode(
                modelInstance = instance,
                scaleToUnits  = 1.2f
            ).apply { isEditable = true }

            sceneView.addChildNode(node)
            modelNode = node
        }
    }

    // ── Botones de control ────────────────────────────────────────────────────
    private fun bindButtons() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<View>(R.id.btnZoomIn).setOnClickListener  { applyZoom(+ZOOM_STEP) }
        findViewById<View>(R.id.btnZoomOut).setOnClickListener { applyZoom(-ZOOM_STEP) }

        findViewById<View>(R.id.btnRotate).setOnClickListener {
            modelNode?.transform(rotate = io.github.sceneview.math.Rotation(y = 45f))
        }

        findViewById<View>(R.id.btnExpand).setOnClickListener {
            startActivity(android.content.Intent(this, ar_scan::class.java))
        }

        // Botones de voz táctiles (demuestran comandos sin API real)
        findViewById<View>(R.id.vcZoom).setOnClickListener   { applyZoom(+ZOOM_STEP) }
        findViewById<View>(R.id.vcRotate).setOnClickListener {
            modelNode?.transform(rotate = io.github.sceneview.math.Rotation(y = 45f))
        }
        findViewById<View>(R.id.vcNext).setOnClickListener   { finish() }
    }

    private fun applyZoom(delta: Float) {
        zoomScale = (zoomScale + delta).coerceIn(ZOOM_MIN, ZOOM_MAX)
        modelNode?.scale = io.github.sceneview.math.Scale(zoomScale)
    }

    // ── Integración con API de voz ─────────────────────────────────────────────
    // TODO: Cuando la API esté lista, reemplazar VoiceApiServiceStub por la
    //       implementación real.
    private fun setupVoiceCommands() {
        voiceService.setCommandListener { cmd ->
            runOnUiThread {
                when (cmd) {
                    is VoiceCommand.Zoom    -> applyZoom(+ZOOM_STEP)
                    is VoiceCommand.Rotate  -> modelNode?.transform(
                        rotate = io.github.sceneview.math.Rotation(y = 45f)
                    )
                    is VoiceCommand.Pause,
                    is VoiceCommand.Stop    -> speaker.stop()
                    else                    -> { /* ignorar */ }
                }
            }
        }
        voiceService.startListening()
    }
    // ──────────────────────────────────────────────────────────────────────────

    // ── Navegación inferior ────────────────────────────────────────────────────
    private fun setupBottomNav() {
        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.selectedItemId = R.id.nav_scan
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> { finish(); true }
                R.id.nav_history   -> {
                    startActivity(android.content.Intent(this, History::class.java))
                    true
                }
                R.id.nav_settings  -> {
                    startActivity(android.content.Intent(this, Ajustes::class.java))
                    true
                }
                else -> true
            }
        }
    }

    // ── Ciclo de vida ──────────────────────────────────────────────────────────
    override fun onResume() {
        super.onResume()
        voiceService.startListening()
    }

    override fun onPause() {
        super.onPause()
        voiceService.stopListening()
        speaker.stop()
    }

    override fun onDestroy() {
        speaker.release()
        super.onDestroy()
    }
}
