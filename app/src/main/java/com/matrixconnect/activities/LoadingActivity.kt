package com.matrixconnect.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.matrixconnect.R

class LoadingActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var currentScreen = 0
    private val screens = arrayOf(
        R.layout.activity_loading_start,
        R.layout.activity_loading_step1
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLoadingScreen()
    }
    
    private fun showLoadingScreen() {
        if (currentScreen < screens.size) {
            setContentView(screens[currentScreen])
            
            // For loading_start, auto-advance after delay
            if (currentScreen == 0) {
                handler.postDelayed({
                    currentScreen++
                    showLoadingScreen()
                }, LOADING_START_DELAY)
            } else {
                // Set up button click listeners for step screens
                findViewById<View>(R.id.btn_next)?.setOnClickListener {
                    currentScreen++
                    showLoadingScreen()
                }
                
                findViewById<View>(R.id.btn_skip)?.setOnClickListener {
                    navigateToMain()
                }
            }
        } else {
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
    
    companion object {
        private const val LOADING_START_DELAY = 2000L // 2 second delay for start screen
    }
}
