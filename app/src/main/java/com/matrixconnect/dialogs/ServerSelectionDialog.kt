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
        return activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(R.string.select_server)

            lifecycleScope.launch {
                servers = withContext(Dispatchers.IO) {
                    AppDatabase.getInstance().serverConfigDao().getAllServerConfigs()
                }

                val serverNames = servers.map { it.name }.toTypedArray()

                builder.setItems(serverNames) { _, which ->
                    val selectedServer = servers[which]
                    viewModel.selectServer(selectedServer)
                }

                builder.setNegativeButton(R.string.cancel) { _, _ ->
                    dialog?.cancel()
                }

                builder.setNeutralButton(R.string.add_server) { _, _ ->
                    // TODO: Implement add server functionality
                    // For now, just show a message
                    showAddServerMessage()
                }
            }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun showAddServerMessage() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.add_server)
            .setMessage(R.string.add_server_not_implemented)
            .setPositiveButton(R.string.ok, null)
            .show()
    }
}
