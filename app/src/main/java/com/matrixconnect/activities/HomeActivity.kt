package com.matrixconnect.activities

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.matrixconnect.R

class HomeActivity : AppCompatActivity() {
    private var isConnected = false
    private lateinit var connectButton: View
    private lateinit var outerRipple: View
    private lateinit var innerRipple: View
    private lateinit var powerIcon: ImageView
    private var outerRippleAnimator: ObjectAnimator? = null
    private var innerRippleAnimator: ObjectAnimator? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize views first
        initializeViews()

        // Set up click listeners
        setupClickListeners()

        // Post animation start to next frame to avoid frame drops
        connectButton.post {
            startRippleAnimation()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up animations
        outerRippleAnimator?.cancel()
        innerRippleAnimator?.cancel()
    }

    private fun initializeViews() {
        try {
            connectButton = findViewById(R.id.btn_connect) ?: throw Exception("Connect button not found")
            outerRipple = findViewById(R.id.outer_ripple) ?: throw Exception("Outer ripple not found")
            innerRipple = findViewById(R.id.inner_ripple) ?: throw Exception("Inner ripple not found")
            powerIcon = findViewById(R.id.power_icon) ?: throw Exception("Power icon not found")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupClickListeners() {
        // Menu button click handler
        findViewById<ImageButton>(R.id.btn_menu)?.setOnClickListener {
            // TODO: Show menu drawer
        }

        // Notification button click handler
        findViewById<ImageButton>(R.id.btn_notification)?.setOnClickListener {
            // TODO: Show notifications
        }

        // Change location button click handler
        findViewById<View>(R.id.btn_change_location)?.setOnClickListener {
            // TODO: Show location selection dialog
        }

        // Connect button click handler
        connectButton.setOnClickListener {
            toggleConnection()
        }
    }

    private fun toggleConnection() {
        isConnected = !isConnected
        
        // Animate power icon
        ObjectAnimator.ofFloat(powerIcon, View.ROTATION, if (isConnected) 360f else 0f).apply {
            duration = 500
            start()
        }

        // TODO: Start/stop VPN connection
        // TODO: Update UI to show connected/disconnected state
    }

    private fun startRippleAnimation() {
        try {
            // Cancel any existing animations
            outerRippleAnimator?.cancel()
            innerRippleAnimator?.cancel()

            // Set initial scale
            outerRipple.scaleX = 1f
            outerRipple.scaleY = 1f
            innerRipple.scaleX = 1f
            innerRipple.scaleY = 1f

            // Outer ripple animation
            val outerScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.2f)
            val outerScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.2f)
            val outerAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.2f, 0f)
            
            outerRippleAnimator = ObjectAnimator.ofPropertyValuesHolder(outerRipple, outerScaleX, outerScaleY, outerAlpha).apply {
                duration = 1500
                repeatCount = ObjectAnimator.INFINITE
                start()
            }

            // Inner ripple animation with delay
            val innerScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.2f)
            val innerScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.2f)
            val innerAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.3f, 0f)
            
            innerRippleAnimator = ObjectAnimator.ofPropertyValuesHolder(innerRipple, innerScaleX, innerScaleY, innerAlpha).apply {
                duration = 1500
                startDelay = 750
                repeatCount = ObjectAnimator.INFINITE
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
