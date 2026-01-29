package com.babayaga.lib

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.BindException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import java.time.Instant

/**
 * Creates a UDP Server.
 */
class Server {
    private val logger = LoggerFactory.getLogger("UDP SERVER RX")
    private val _lastMessage = MutableStateFlow<Quadruple<String, String, Int, Long>?>(null)
    val lastMessage = _lastMessage.asStateFlow()

    private val _listening = MutableStateFlow(false)
    val listening = _listening.asStateFlow()

    private var job: Job? = null
    private var socket: DatagramSocket? = null

    /**
     * Starts the UDP Server.
     *
     * @return True if server started, otherwise false.
     */
    fun start(host: String, port: Int, supervisor: Job): Boolean {
        var started = false

        if (port > 0 && job == null) {
            job = CoroutineScope(supervisor).launch(Dispatchers.IO) {
                logger.info("Started server job")
                try {
                    DatagramSocket(port).let { aSocket ->
                        _listening.value = true
                        socket = aSocket
                        aSocket.soTimeout = 1000

                        val rxBuf = ByteArray(2048)
                        val packet = DatagramPacket(rxBuf, rxBuf.size)

                        try {
                            logger.info("Started listening")
                            do {
                                try {
                                    aSocket.receive(packet)
                                    logger.info("Received packet length ${packet.length}")
                                    val received = String(rxBuf, 0, packet.length)
                                    val senderHost = packet.address.hostAddress ?: ""
                                    val senderPort = packet.port
                                    val time = Instant.now().toEpochMilli()

                                    _lastMessage.value =
                                        Quadruple(received, senderHost, senderPort, time)
                                    logger.info("Received message '$received' from $senderHost:$senderPort")
                                } catch (ste: SocketTimeoutException) {
                                    logger.trace("Socket receive timeout")
                                }
                            } while (listening.value)

                            logger.info("Stopped listening")
                        } catch (e: IOException) {
                            _listening.value = false
                            logger.warn("Exception occurred while listening: $e")
                        } finally {
                            try {
                                aSocket.close()
                                socket = null
                                logger.info("Closed the listening socket")
                            } catch (e: IOException) {
                                logger.warn("Exception occurred while stopping the socket: $e")
                            }
                        }
                    }
                } catch (e: BindException) {
                    logger.error("BindException occurred while trying to start server: $e")
                    _listening.value = false
                }
            }

            started = true
        } else {
            logger.warn("Did not attempt to start the server, invalid port or already started.")
        }

        return started
    }

    /**
     * Stops the UDP Server.
     */
    fun stop() {
        if (listening.value) {
            _listening.value = false
            socket?.close()
            job?.cancel()
            job = null
            logger.info("Stopped the server job")
        } else {
            logger.warn("Did not attempt to stop the server, if is not running")
        }
    }
}