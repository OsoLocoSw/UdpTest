package com.marples.lib

import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class Client {
    companion object {
        fun send(
            message: String,
            destHost: String,
            destPort: Int,
            srcPort: Int?,
        ): Boolean {
            val logger = LoggerFactory.getLogger("UDP SERVER TX")

            return try {
                // Send the udp
                val toSend = message.trim().toByteArray()
                val destination = InetAddress.getByName(destHost)
                val packet = DatagramPacket(toSend, toSend.size, destination, destPort)
                val socket = srcPort?.let { port ->
                    DatagramSocket(port)
                } ?: run {
                    DatagramSocket()
                }
                socket.send(packet)
                socket.close()
                logger.info("Send packet to $destination : $destPort")
                true
            } catch (e: IOException) {
                logger.error("Failed to send packet: $e")
                false
            }
        }
    }
}