package com.matrixconnect.network.proxy

import java.net.InetAddress
import java.nio.ByteBuffer

class Socks5Protocol : BaseProxyProtocol() {
    companion object {
        private const val SOCKS_VERSION = 0x05
        private const val NO_AUTHENTICATION = 0x00
        private const val CONNECT_COMMAND = 0x01
        private const val IPV4_ADDRESS = 0x01
        private const val DOMAIN_NAME = 0x03
        private const val IPV6_ADDRESS = 0x04
        private const val RESERVED = 0x00
    }

    override fun initialize(): ByteArray {
        state = ProxyState.AUTHENTICATING
        // Version 5, 1 authentication method (no authentication)
        return byteArrayOf(
            SOCKS_VERSION.toByte(),
            1.toByte(), // Number of authentication methods
            NO_AUTHENTICATION.toByte() // No authentication required
        )
    }

    override fun connect(host: String, port: Int): ByteArray {
        if (state != ProxyState.CONNECTED && state != ProxyState.CONNECTING) {
            throw ProxyException.InvalidProtocolData()
        }

        val buffer = ByteBuffer.allocate(1024)
        buffer.put(SOCKS_VERSION.toByte()) // SOCKS version
        buffer.put(CONNECT_COMMAND.toByte()) // CONNECT command
        buffer.put(RESERVED.toByte()) // Reserved byte

        try {
            // Try to parse as IP address
            val ipAddress = InetAddress.getByName(host)
            when (ipAddress.address.size) {
                4 -> { // IPv4
                    buffer.put(IPV4_ADDRESS.toByte())
                    buffer.put(ipAddress.address)
                }
                16 -> { // IPv6
                    buffer.put(IPV6_ADDRESS.toByte())
                    buffer.put(ipAddress.address)
                }
                else -> throw ProxyException.UnsupportedAddressType()
            }
        } catch (e: Exception) {
            // Handle as domain name
            val domainBytes = host.toByteArray()
            if (domainBytes.size > 255) {
                throw ProxyException.UnsupportedAddressType()
            }
            buffer.put(DOMAIN_NAME.toByte())
            buffer.put(domainBytes.size.toByte())
            buffer.put(domainBytes)
        }

        // Add port in network byte order (big-endian)
        buffer.put((port shr 8).toByte())
        buffer.put((port and 0xFF).toByte())

        state = ProxyState.CONNECTING
        
        buffer.flip()
        val result = ByteArray(buffer.remaining())
        buffer.get(result)
        return result
    }

    override fun processResponse(data: ByteArray): Boolean {
        when (state) {
            ProxyState.AUTHENTICATING -> {
                if (data.size < 2 || data[0] != SOCKS_VERSION.toByte() || data[1] != NO_AUTHENTICATION.toByte()) {
                    state = ProxyState.ERROR
                    throw ProxyException.AuthenticationFailed()
                }
                state = ProxyState.CONNECTING
                return true
            }
            
            ProxyState.CONNECTING -> {
                if (data.size < 4 || data[0] != SOCKS_VERSION.toByte()) {
                    state = ProxyState.ERROR
                    throw ProxyException.InvalidProtocolData()
                }

                when (data[1].toInt()) {
                    0x00 -> { // Success
                        state = ProxyState.CONNECTED
                        return true
                    }
                    else -> {
                        state = ProxyState.ERROR
                        throw ProxyException.ConnectionFailed()
                    }
                }
            }
            
            ProxyState.CONNECTED -> return true
            
            else -> {
                state = ProxyState.ERROR
                throw ProxyException.InvalidProtocolData()
            }
        }
    }
}
