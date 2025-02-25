package com.matrixconnect.network.proxy

import java.nio.ByteBuffer

interface ProxyProtocol {
    fun initialize(): ByteArray
    fun connect(host: String, port: Int): ByteArray
    fun processResponse(data: ByteArray): Boolean
    fun isConnected(): Boolean
    val state: ProxyState
}

enum class ProxyState {
    INITIAL,
    AUTHENTICATING,
    CONNECTING,
    CONNECTED,
    ERROR
}

sealed class ProxyException(message: String) : Exception(message) {
    class AuthenticationFailed : ProxyException("Authentication failed")
    class ConnectionFailed : ProxyException("Connection failed")
    class InvalidProtocolData : ProxyException("Invalid protocol data")
    class UnsupportedAddressType : ProxyException("Unsupported address type")
}

abstract class BaseProxyProtocol : ProxyProtocol {
    override var state = ProxyState.INITIAL
    
    override fun isConnected(): Boolean = state == ProxyState.CONNECTED
    
    protected fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
    
    protected fun Int.toByteArray(): ByteArray = ByteBuffer.allocate(4).putInt(this).array()
    
    protected fun String.toByteArray(charset: String = "UTF-8"): ByteArray = toByteArray(charset(charset))
}
