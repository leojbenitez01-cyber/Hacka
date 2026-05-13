package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.tts.InstructionSpeaker
import com.example.myapplication.voice.VoiceCommand
import com.example.myapplication.voice.VoiceRecognitionService
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.ar.core.Plane
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.launch

class ar_scan : AppCompatActivity() {

    private lateinit var arSceneView: ARSceneView
    private lateinit var tvInstruction: TextView
    private lateinit var tvStepNum: TextView
    private lateinit var tvStepPct: TextView
    private lateinit var stepProgress: View
    private lateinit var btnNext: Button
    private lateinit var btnPrev: Button
    private lateinit var btnTeclado: View

    private val speaker by lazy { InstructionSpeaker(this) }
    private val voiceService by lazy { VoiceRecognitionService(this) }

    private var modelPlaced = false
    private var currentStep = 0

    private val steps = listOf(
        "Apague la máquina y desconecte la alimentación eléctrica",
        "Retire el panel frontal con llave Allen número 5",
        "Inspeccione el filtro de aire — reemplace si está obstruido más del 50%",
        "Verifique el estado de los rodamientos — escuche ruidos anómalos",
        "Revise el nivel de aceite en el visor lateral y rellene si es necesario"
    )

    companion object {
        const val MODEL_FILE = "models/brazo/brazo.glb"
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) initAR()
        else {
            Toast.makeText(this, "Se requiere permiso de cámara para AR", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar_scan)
        bindViews()
        checkCameraPermission()
    }

    private fun bindViews() {
        arSceneView   = findViewById(R.id.arSceneView)
        tvInstruction = findViewById(R.id.tvInstruction)
        tvStepNum     = findViewById(R.id.tvStepNum)
        tvStepPct     = findViewById(R.id.tvStepPct)
        stepProgress  = findViewById(R.id.stepProgress)
        btnNext       = findViewById(R.id.btnNext)
        btnPrev       = findViewById(R.id.btnPrev)
        btnTeclado    = findViewById(R.id.btnTeclado)

        btnNext.setOnClickListener { goToStep(currentStep + 1) }
        btnPrev.setOnClickListener { goToStep(currentStep - 1) }
        btnTeclado.setOnClickListener { launchKeyboardGuide() }
        findViewById<View>(R.id.btnClose).setOnClickListener { finish() }

        setupBottomNav()
        updateStepUI()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) initAR()
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun initAR() {
        arSceneView.setOnTouchListener { _, event ->
            if (!modelPlaced && event.action == MotionEvent.ACTION_UP) {
                arSceneView.frame?.hitTest(event)
                    ?.firstOrNull { it.trackable is Plane }
                    ?.let { placeModel(it) }
            }
            true
        }
        setupVoiceCommands()
    }

    // ── Modelo 3D ─────────────────────────────────────────────────────────────

    private fun placeModel(hitResult: com.google.ar.core.HitResult) {
        lifecycleScope.launch {
            val instance = arSceneView.modelLoader.createModelInstance(MODEL_FILE)
                ?: run {
                    Toast.makeText(
                        this@ar_scan, "No se encontró $MODEL_FILE en assets/", Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

            val modelNode = ModelNode(modelInstance = instance, scaleToUnits = 0.8f)
            val anchorNode = AnchorNode(
                engine = arSceneView.engine,
                anchor  = hitResult.createAnchor()
            ).apply {
                isEditable = true
                addChildNode(modelNode)
            }

            arSceneView.addChildNode(anchorNode)
            modelPlaced = true
            speaker.speak(steps[currentStep])
        }
    }

    // ── Guía de teclado ───────────────────────────────────────────────────────

    private fun launchKeyboardGuide() {
        speaker.speak("Abriendo guía de mantenimiento del teclado.")
        startActivity(Intent(this, TecladoActivity::class.java))
    }

    // ── Pasos ─────────────────────────────────────────────────────────────────

    private fun goToStep(index: Int) {
        if (index !in steps.indices) return
        currentStep = index
        updateStepUI()
        speaker.speak(steps[currentStep])
    }

    private fun updateStepUI() {
        val total = steps.size
        val num   = currentStep + 1
        val pct   = (num * 100) / total

        tvStepNum.text     = "PASO ${num.toString().padStart(2, '0')}/$total"
        tvStepPct.text     = "$pct%"
        tvInstruction.text = steps[currentStep]

        stepProgress.post {
            val parent = stepProgress.parent as? View ?: return@post
            val targetW = (parent.width * pct / 100).coerceAtLeast(4)
            stepProgress.layoutParams = stepProgress.layoutParams.apply { width = targetW }
            stepProgress.requestLayout()
        }

        btnPrev.isEnabled = currentStep > 0
        btnNext.isEnabled = currentStep < steps.size - 1
    }

    // ── Voz ───────────────────────────────────────────────────────────────────

    private fun setupVoiceCommands() {
        voiceService.setCommandListener { cmd ->
            runOnUiThread {
                when (cmd) {
                    is VoiceCommand.Next     -> goToStep(currentStep + 1)
                    is VoiceCommand.Previous -> goToStep(currentStep - 1)
                    is VoiceCommand.Repeat   -> speaker.speak(steps[currentStep])
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
