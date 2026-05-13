package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.tts.InstructionSpeaker
import com.example.myapplication.voice.VoiceApiService
import com.example.myapplication.voice.VoiceApiServiceStub
import com.example.myapplication.voice.VoiceCommand
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.sceneview.SceneView
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.launch

class TecladoActivity : AppCompatActivity() {

    private lateinit var sceneView: SceneView
    private lateinit var tvInstruction: TextView
    private lateinit var tvStepNum: TextView
    private lateinit var tvStepPct: TextView
    private lateinit var tvZone: TextView
    private lateinit var stepProgress: View
    private lateinit var btnNext: Button
    private lateinit var btnPrev: Button
    private lateinit var loadingOverlay: View

    private val speaker by lazy { InstructionSpeaker(this) }
    private val voiceService: VoiceApiService = VoiceApiServiceStub()

    private var modelNode: ModelNode? = null
    private var zoomScale = 1.0f
    private var currentStep = 0

    companion object {
        private const val TOTAL_PIECES = 383
        private const val ZOOM_STEP  = 0.15f
        private const val ZOOM_MIN   = 0.2f
        private const val ZOOM_MAX   = 3.0f
        private const val MODEL_SCALE = 0.8f

        private fun fileName(step: Int): String =
            if (step == 0) "teclado/tecladocompleto.glb"
            else "teclado/Cube.001_Cube.%03d.glb".format(step)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teclado)

        sceneView      = findViewById(R.id.sceneView)
        tvInstruction  = findViewById(R.id.tvInstruction)
        tvStepNum      = findViewById(R.id.tvStepNum)
        tvStepPct      = findViewById(R.id.tvStepPct)
        tvZone         = findViewById(R.id.tvZone)
        stepProgress   = findViewById(R.id.stepProgress)
        btnNext        = findViewById(R.id.btnNext)
        btnPrev        = findViewById(R.id.btnPrev)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        bindButtons()
        setupBottomNav()
        setupVoiceCommands()
        updateStepUI()
        loadCurrentModel()
    }

    // ── Modelo 3D ─────────────────────────────────────────────────────────────

    private fun loadCurrentModel() {
        val file = fileName(currentStep)
        loadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            modelNode?.let { sceneView.removeChildNode(it) }
            modelNode = null

            val instance = sceneView.modelLoader.createModelInstance(file)
            loadingOverlay.visibility = View.GONE

            if (instance == null) {
                Toast.makeText(this@TecladoActivity, "No se encontró: $file", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val node = ModelNode(modelInstance = instance, scaleToUnits = MODEL_SCALE)
                .apply { isEditable = true }

            sceneView.addChildNode(node)
            modelNode = node
            zoomScale = 1.0f
        }
    }

    // ── Pasos ─────────────────────────────────────────────────────────────────

    private fun goToStep(index: Int) {
        if (index !in 0..TOTAL_PIECES) return
        currentStep = index
        updateStepUI()
        loadCurrentModel()
        val label = if (currentStep == 0) "Teclado completo" else "Pieza $currentStep de $TOTAL_PIECES"
        speaker.speak(label)
    }

    private fun updateStepUI() {
        val total = TOTAL_PIECES + 1          // paso 0 + 383 piezas
        val pct   = (currentStep * 100) / total

        if (currentStep == 0) {
            tvStepNum.text     = "VISTA COMPLETA"
            tvZone.text        = "ZONA: TECLADO COMPLETO"
            tvInstruction.text = "Vista general del teclado de control"
        } else {
            tvStepNum.text     = "PIEZA ${currentStep.toString().padStart(3, '0')}/$TOTAL_PIECES"
            tvZone.text        = "ZONA: TECLADO DE CONTROL"
            tvInstruction.text = "Inspeccione la pieza %03d del teclado".format(currentStep)
        }
        tvStepPct.text = "$pct%"

        stepProgress.post {
            val parent = stepProgress.parent as? View ?: return@post
            val targetW = (parent.width * pct / 100).coerceAtLeast(4)
            stepProgress.layoutParams = stepProgress.layoutParams.apply { width = targetW }
            stepProgress.requestLayout()
        }

        btnPrev.isEnabled = currentStep > 0
        btnNext.isEnabled = currentStep < TOTAL_PIECES
    }

    // ── Controles ─────────────────────────────────────────────────────────────

    private fun bindButtons() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        btnNext.setOnClickListener { goToStep(currentStep + 1) }
        btnPrev.setOnClickListener { goToStep(currentStep - 1) }
        findViewById<View>(R.id.btnRotate).setOnClickListener  { rotateModel() }
        findViewById<View>(R.id.btnZoomIn).setOnClickListener  { applyZoom(+ZOOM_STEP) }
        findViewById<View>(R.id.btnZoomOut).setOnClickListener { applyZoom(-ZOOM_STEP) }
    }

    private fun applyZoom(delta: Float) {
        zoomScale = (zoomScale + delta).coerceIn(ZOOM_MIN, ZOOM_MAX)
        modelNode?.scale = Scale(zoomScale, zoomScale, zoomScale)
    }

    private fun rotateModel() {
        val node = modelNode ?: return
        node.rotation = node.rotation + Rotation(y = 45f)
    }

    // ── Voz ───────────────────────────────────────────────────────────────────

    private fun setupVoiceCommands() {
        voiceService.setCommandListener { cmd ->
            runOnUiThread {
                when (cmd) {
                    is VoiceCommand.Next     -> goToStep(currentStep + 1)
                    is VoiceCommand.Previous -> goToStep(currentStep - 1)
                    is VoiceCommand.Repeat   -> speaker.speak(if (currentStep == 0) "Teclado completo" else "Pieza $currentStep de $TOTAL_PIECES")
                    is VoiceCommand.Zoom     -> applyZoom(+ZOOM_STEP)
                    is VoiceCommand.Rotate   -> rotateModel()
                    is VoiceCommand.Pause,
                    is VoiceCommand.Stop     -> speaker.stop()
                    else                     -> {}
                }
            }
        }
        voiceService.startListening()
    }

    // ── Navegación inferior ────────────────────────────────────────────────────

    private fun setupBottomNav() {
        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.selectedItemId = R.id.nav_scan
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> { finish(); true }
                R.id.nav_history   -> { startActivity(Intent(this, History::class.java)); true }
                R.id.nav_settings  -> { startActivity(Intent(this, Ajustes::class.java)); true }
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
