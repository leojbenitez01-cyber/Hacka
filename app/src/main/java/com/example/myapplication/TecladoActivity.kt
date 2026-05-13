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

    data class KeyboardStep(
        val zone: String,
        val instruction: String,
        val fileIndex: Int
    )

    private val steps = listOf(
        KeyboardStep(
            zone = "INSPECCIÓN GENERAL",
            instruction = "Apague el panel de control y desconecte la alimentación antes de proceder con la inspección del teclado.",
            fileIndex = 1
        ),
        KeyboardStep(
            zone = "TECLAS DE FUNCIÓN F1–F12",
            instruction = "Inspeccione las teclas de función F1 a F12. Verifique que no presenten desgaste excesivo, grietas o teclas atascadas.",
            fileIndex = 2
        ),
        KeyboardStep(
            zone = "FILA NUMÉRICA  1–0",
            instruction = "Revise la fila numérica superior (1, 2, 3… 0, -, =). Limpie residuos con aire comprimido a 30 PSI máximo.",
            fileIndex = 62
        ),
        KeyboardStep(
            zone = "FILA QWERTY",
            instruction = "Examine la fila principal QWERTY. Compruebe que cada tecla regresa a su posición correcta al soltarla.",
            fileIndex = 122
        ),
        KeyboardStep(
            zone = "FILA ASDF",
            instruction = "Revise la fila de inicio ASDF. Verifique el estado del mecanismo de cada tecla y limpie con paño antiestático.",
            fileIndex = 192
        ),
        KeyboardStep(
            zone = "FILA ZXCV — BARRA ESPACIADORA",
            instruction = "Inspeccione la fila inferior ZXCV y la barra espaciadora. La barra es la tecla con mayor desgaste — verifique sus estabilizadores.",
            fileIndex = 252
        ),
        KeyboardStep(
            zone = "TECLAS MODIFICADORAS",
            instruction = "Compruebe CTRL, ALT, SHIFT, WIN y teclas especiales. Aplique lubricante de silicón en los ejes si presentan resistencia.",
            fileIndex = 307
        ),
        KeyboardStep(
            zone = "TECLADO NUMÉRICO LATERAL",
            instruction = "Revise el teclado numérico (NumPad). Pruebe NumLock, /, *, -, + y la tecla Enter lateral. Reconecte el cable de datos al finalizar.",
            fileIndex = 362
        )
    )

    companion object {
        private const val ZOOM_STEP = 0.15f
        private const val ZOOM_MIN  = 0.2f
        private const val ZOOM_MAX  = 3.0f
        private const val MODEL_SCALE = 0.8f
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

        lifecycle.addObserver(sceneView)

        bindButtons()
        setupBottomNav()
        setupVoiceCommands()
        updateStepUI()
        loadCurrentModel()
    }

    // ── Modelo 3D ─────────────────────────────────────────────────────────────

    private fun loadCurrentModel() {
        val step = steps[currentStep]
        val fileName = "teclado/Cube.001_Cube.%03d.glb".format(step.fileIndex)

        loadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            // Remover nodo anterior
            modelNode?.let { sceneView.removeChildNode(it) }
            modelNode = null

            val instance = sceneView.modelLoader.createModelInstance(fileName)

            loadingOverlay.visibility = View.GONE

            if (instance == null) {
                Toast.makeText(
                    this@TecladoActivity,
                    "No se encontró el modelo: $fileName",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val node = ModelNode(
                modelInstance = instance,
                scaleToUnits  = MODEL_SCALE
            ).apply { isEditable = true }

            sceneView.addChildNode(node)
            modelNode = node
            zoomScale = 1.0f
        }
    }

    // ── Pasos ─────────────────────────────────────────────────────────────────

    private fun goToStep(index: Int) {
        if (index !in steps.indices) return
        currentStep = index
        updateStepUI()
        loadCurrentModel()
        speaker.speak(steps[currentStep].instruction)
    }

    private fun updateStepUI() {
        val total = steps.size
        val num   = currentStep + 1
        val pct   = (num * 100) / total
        val step  = steps[currentStep]

        tvStepNum.text     = "PASO ${num.toString().padStart(2, '0')}/$total"
        tvStepPct.text     = "$pct%"
        tvInstruction.text = step.instruction
        tvZone.text        = "ZONA: ${step.zone}"

        stepProgress.post {
            val parent = stepProgress.parent as? View ?: return@post
            val targetW = (parent.width * pct / 100).coerceAtLeast(4)
            stepProgress.layoutParams = stepProgress.layoutParams.apply { width = targetW }
            stepProgress.requestLayout()
        }

        btnPrev.isEnabled = currentStep > 0
        btnNext.isEnabled = currentStep < steps.size - 1
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
                    is VoiceCommand.Repeat   -> speaker.speak(steps[currentStep].instruction)
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
