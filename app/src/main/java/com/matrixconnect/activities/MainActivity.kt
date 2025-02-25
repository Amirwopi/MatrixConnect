package com.matrixconnect.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.matrixconnect.R
import com.matrixconnect.data.entities.ServerConfig
import com.matrixconnect.databinding.ActivityMainBinding
import com.matrixconnect.dialogs.ServerSelectionDialog
import com.matrixconnect.services.ConnectionService
import com.matrixconnect.viewmodels.ConnectionStatus
import com.matrixconnect.viewmodels.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.connectButton.setOnClickListener {
            when (viewModel.connectionStats.value.status) {
                ConnectionStatus.CONNECTED, ConnectionStatus.CONNECTING -> disconnectFromServer()
                else -> connectToServer()
            }
        }

        binding.serverCard.setOnClickListener {
            ServerSelectionDialog().show(supportFragmentManager, "ServerSelectionDialog")
        }

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }


        binding.statsCard.setOnClickListener {
            // TODO: Show detailed statistics dialog
        }
    }

    private fun observeViewModel() {
        // Observe selected server
        viewModel.selectedServer.observe(this) { server ->
            updateServerInfo(server)
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                showError(it)
                viewModel.clearError()
            }
        }

        // Observe connection stats using coroutines
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.connectionStats.collect { stats ->
                    updateConnectionStatus(stats.status)
                    updateConnectionStats(stats)
                }
            }
        }
    }

    private fun updateServerInfo(server: ServerConfig?) {
        server?.let {
            binding.serverName.text = it.name
            binding.serverProtocol.text = it.protocol
            binding.serverAddress.text = "${it.host}:${it.port}"
            binding.noServerSelected.isVisible = false
            binding.serverInfo.isVisible = true
        } ?: run {
            binding.noServerSelected.isVisible = true
            binding.serverInfo.isVisible = false
        }
    }

    private fun updateConnectionStatus(status: ConnectionStatus) {
        binding.connectButton.isEnabled = true
        
        when (status) {
            ConnectionStatus.CONNECTED -> {
                binding.connectButton.setText(R.string.disconnect)
                binding.statusIndicator.setImageResource(R.drawable.ic_connected)
                binding.connectionStatus.setText(R.string.status_connected)
                binding.statsCard.isVisible = true
            }
            ConnectionStatus.CONNECTING -> {
                binding.connectButton.isEnabled = false
                binding.statusIndicator.setImageResource(R.drawable.ic_connecting)
                binding.connectionStatus.setText(R.string.status_connecting)
            }
            ConnectionStatus.DISCONNECTED -> {
                binding.connectButton.setText(R.string.connect)
                binding.statusIndicator.setImageResource(R.drawable.ic_disconnected)
                binding.connectionStatus.setText(R.string.status_disconnected)
                binding.statsCard.isVisible = false
            }
            ConnectionStatus.ERROR -> {
                binding.connectButton.setText(R.string.connect)
                binding.statusIndicator.setImageResource(R.drawable.ic_error)
                binding.connectionStatus.setText(R.string.status_error)
                binding.statsCard.isVisible = false
            }
        }
    }

    private fun updateConnectionStats(stats: com.matrixconnect.viewmodels.ConnectionStats) {
        binding.bytesReceived.text = viewModel.formatBytes(stats.bytesReceived)
        binding.bytesSent.text = viewModel.formatBytes(stats.bytesSent)
        binding.uptime.text = viewModel.formatUptime(stats.uptime)
    }

    private fun connectToServer() {
        viewModel.selectedServer.value?.let { server ->
            val intent = Intent(this, ConnectionService::class.java).apply {
                action = ConnectionService.ACTION_START
                putExtra(ConnectionService.EXTRA_SERVER_ID, server.id)
            }
            startService(intent)
            viewModel.updateConnectionStatus(ConnectionStatus.CONNECTING)
        } ?: showError(getString(R.string.no_server_selected))
    }

    private fun disconnectFromServer() {
        val intent = Intent(this, ConnectionService::class.java).apply {
            action = ConnectionService.ACTION_STOP
        }
        startService(intent)
        viewModel.updateConnectionStatus(ConnectionStatus.DISCONNECTED)
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}
