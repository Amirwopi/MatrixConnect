package com.matrixconnect.network

import com.matrixconnect.data.entities.ServerConfig
import com.matrixconnect.network.crypto.CryptoProvider
import com.matrixconnect.network.proxy.ProxyProtocol
import com.matrixconnect.network.proxy.Socks5Protocol
import kotlinx.coroutines.*
import java.io.Closeable
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import kotlin.coroutines.CoroutineContext

class ProxyConnection(
    private val serverConfig: ServerConfig,
    private val onBytesTransferred: (received: Long, sent: Long) -> Unit,
    private val onError: (Throwable) -> Unit
) : CoroutineScope, Closeable {

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private var selector: Selector? = null
    private var serverChannel: SocketChannel? = null
    private var isRunning = false
    
    val isActive: Boolean
        get() = isRunning && !job.isCancelled

    private val cryptoProvider = CryptoProvider.create(
        type = serverConfig.encryptionType,
        key = serverConfig.encryptionKey
    )

    private val proxyProtocol: ProxyProtocol = when (serverConfig.protocol) {
        "SOCKS5" -> Socks5Protocol()
        // Add other protocols here when implemented
        else -> throw IllegalArgumentException("Unsupported protocol: ${serverConfig.protocol}")
    }

    fun start() {
        if (isRunning) return
        
        launch {
            try {
                initializeConnection()
                startProxyLoop()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private fun initializeConnection() {
        selector = Selector.open()
        serverChannel = SocketChannel.open().apply {
            configureBlocking(false)
            connect(InetSocketAddress(serverConfig.host, serverConfig.port))
            register(selector, SelectionKey.OP_CONNECT)
        }
        isRunning = true
    }

    private suspend fun startProxyLoop() = withContext(Dispatchers.IO) {
        val buffer = ByteBuffer.allocate(8192)
        
        while (isRunning && selector?.isOpen == true) {
            val readyChannels = selector?.select(100) ?: 0
            if (readyChannels == 0) continue

            val selectedKeys = selector?.selectedKeys()?.iterator()
            while (selectedKeys?.hasNext() == true) {
                val key = selectedKeys.next()
                selectedKeys.remove()

                try {
                    when {
                        key.isConnectable -> handleConnect(key)
                        key.isReadable -> handleRead(key, buffer)
                        key.isWritable -> handleWrite(key, buffer)
                    }
                } catch (e: Exception) {
                    key.cancel()
                    throw e
                }
            }
        }
    }

    private fun handleConnect(key: SelectionKey) {
        val channel = key.channel() as SocketChannel
        if (channel.finishConnect()) {
            val initData = proxyProtocol.initialize()
            channel.write(ByteBuffer.wrap(cryptoProvider.encrypt(initData)))
            key.interestOps(SelectionKey.OP_READ)
        }
    }

    private fun handleRead(key: SelectionKey, buffer: ByteBuffer) {
        val channel = key.channel() as SocketChannel
        buffer.clear()
        
        val bytesRead = channel.read(buffer)
        if (bytesRead == -1) {
            throw Exception("Connection closed by server")
        }

        buffer.flip()
        val decryptedData = cryptoProvider.decrypt(buffer.array(), 0, buffer.limit())
        onBytesTransferred(bytesRead.toLong(), 0)

        if (proxyProtocol.processResponse(decryptedData)) {
            if (proxyProtocol.isConnected()) {
                key.interestOps(SelectionKey.OP_READ or SelectionKey.OP_WRITE)
            } else {
                key.interestOps(SelectionKey.OP_WRITE)
            }
        }
    }

    private fun handleWrite(key: SelectionKey, buffer: ByteBuffer) {
        val channel = key.channel() as SocketChannel
        buffer.clear()

        if (!proxyProtocol.isConnected()) {
            val connectData = proxyProtocol.connect(serverConfig.host, serverConfig.port)
            val encryptedData = cryptoProvider.encrypt(connectData)
            buffer.put(encryptedData)
            buffer.flip()
            val bytesSent = channel.write(buffer)
            onBytesTransferred(0, bytesSent.toLong())
        }

        key.interestOps(SelectionKey.OP_READ)
    }

    fun stop() {
        isRunning = false
        serverChannel?.close()
        selector?.close()
        job.cancel()
    }

    override fun close() {
        stop()
    }
}
