package com.matrixconnect.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.matrixconnect.R
import com.matrixconnect.data.AppDatabase
import com.matrixconnect.data.entities.ServerConfig
import com.matrixconnect.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServerSelectionDialog : DialogFragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var servers: List<ServerConfig>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.select_server)

        lifecycleScope.launch {
            try {
                servers = withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(requireContext()).serverConfigDao().getAllServerConfigs()
                }

                if (servers.isEmpty()) {
                    builder.setMessage(R.string.no_servers_available)
                    builder.setPositiveButton(R.string.ok) { _, _ -> dialog?.cancel() }
                } else {
                    val serverNames = servers.map { it.name }.toTypedArray()
                    builder.setItems(serverNames) { _, which ->
                        val selectedServer = servers[which]
                        viewModel.selectServer(selectedServer)
                    }
                }

                builder.setNegativeButton(R.string.cancel) { _, _ -> dialog?.cancel() }
                builder.setNeutralButton(R.string.add_server) { _, _ -> 
                    showAddServerMessage() 
                }

            } catch (e: Exception) {
                builder.setMessage(R.string.error_loading_servers)
                builder.setPositiveButton(R.string.ok) { _, _ -> dialog?.cancel() }
            }
        }

        return builder.create()
    }

    private fun showAddServerMessage() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_server)
            .setMessage(R.string.add_server_not_implemented)
            .setPositiveButton(R.string.ok, null)
            .show()
    }
}