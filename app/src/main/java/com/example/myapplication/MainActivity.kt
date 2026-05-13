package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindActionTiles()
        setupBottomNav()
    }

    // ── Tiles de acción rápida ─────────────────────────────────────────────────
    private fun bindActionTiles() {
        findViewById<View>(R.id.tileScan).setOnClickListener {
            startActivity(Intent(this, ar_scan::class.java))
        }
        findViewById<View>(R.id.tileManuals).setOnClickListener {
            startActivity(Intent(this, View_3d::class.java))
        }
        findViewById<View>(R.id.tileHistory).setOnClickListener {
            startActivity(Intent(this, History::class.java))
        }
        findViewById<View>(R.id.tileSettings).setOnClickListener {
            startActivity(Intent(this, Ajustes::class.java))
        }
    }

    // ── Navegación inferior ────────────────────────────────────────────────────
    private fun setupBottomNav() {
        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.selectedItemId = R.id.nav_dashboard
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_scan -> {
                    startActivity(Intent(this, ar_scan::class.java))
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, History::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, Ajustes::class.java))
                    true
                }
                else -> true
            }
        }
    }
}
