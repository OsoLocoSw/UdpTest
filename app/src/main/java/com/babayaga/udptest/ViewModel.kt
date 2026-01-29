package com.babayaga.udptest

import android.content.Context
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.babayaga.lib.Server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class ViewModel(private val context: Context) {
    enum class ListenState {
        INVALID,
        CAN_LISTEN,
        LISTENING
    }

    enum class SendState {
        INVALID,
        CAN_SEND
    }

    private val supervisor = SupervisorJob()

    private val _listenHost = MutableStateFlow("")
    val listenerHost = _listenHost.asStateFlow()
    private val _listenPort = MutableStateFlow("")
    val listenerPort = _listenPort.asStateFlow()
    private val listening = MutableStateFlow(false)

    private val _sendHost = MutableStateFlow("")
    val sendHost = _sendHost.asStateFlow()
    private val _sendPort = MutableStateFlow("")
    val sendPort = _sendPort.asStateFlow()
    private val _sendSourcePort = MutableStateFlow("")
    val sendSourcePort = _sendSourcePort.asStateFlow()
    private val _sendMessage = MutableStateFlow("")
    val sendMessage = _sendMessage.asStateFlow()

    val sendState = combine(sendHost, sendPort, sendMessage) { host, port, message ->
        if (host.isNotBlank() && port.isNotBlank() && message.isNotBlank()) {
            SendState.CAN_SEND
        } else {
            SendState.INVALID
        }
    }.stateIn(
        scope = CoroutineScope(supervisor),
        started = SharingStarted.Eagerly,
        initialValue = SendState.INVALID
    )

    private val server = Server()

    val listenState = combine(listenerHost, listenerPort, listening) { host, port, listening ->
        when {
            listening -> ListenState.LISTENING

            host.isNotBlank() && port.isNotBlank() -> ListenState.CAN_LISTEN

            else -> ListenState.INVALID
        }
    }.stateIn(
        scope = CoroutineScope(supervisor),
        started = SharingStarted.Eagerly,
        initialValue = ListenState.INVALID
    )

    private val _messages = MutableStateFlow(emptyList<String>())
    val messages = _messages.asStateFlow()

    private var listenJob: Job? = null
    private var getAddressesJob: Job? = null
    private val _ipAddresses = MutableStateFlow(emptyList<String>())
    val ipAddresses = _ipAddresses.asStateFlow()

    init {
        getAddressesJob = CoroutineScope(supervisor).launch(Dispatchers.IO) {
            internalGetAddresses()
        }

        CoroutineScope(supervisor).launch(Dispatchers.IO) {
            server.listening.collect { serverListening ->
                listening.value = serverListening
            }
        }
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    fun listen() {
        if (listenState.value == ListenState.CAN_LISTEN) {
            listenerPort.value.toIntOrNull()?.let { port ->
                Log.d("UDP RX", "Started server with listening value ${server.listening.value}")
                listening.value = server.start(listenerHost.value, port, supervisor)

                listenJob = CoroutineScope(supervisor).launch(Dispatchers.IO) {
                    server.lastMessage.collect { messageTriple ->
                        messageTriple?.let { quad ->
                            val message = "(${quad.second}:${quad.third}) ${quad.first}"
                            _messages.value = listOf(message) + messages.value
                        }
                    }
                }
            }
        } else {
            Log.w("UDP RX", "Cannot listen, state is invalid")
        }
    }

    fun stop() {
        if (listenState.value == ListenState.LISTENING) {
            server.stop()
            listenJob?.cancel()
            listenJob = null
            Log.i("UDP RX", "Stopped the server")
        } else {
            Log.w("UDP RX", "Invalid Listen State (${listenState.value}")
        }
    }

    fun listenerHostChanged(value: String) {
        val host = value.trim()
        _listenHost.value = host
    }

    fun listenerPortChanged(value: String) {
        value.toIntOrNull()?.let { _ ->
            _listenPort.value = value.trim()
        } ?: run {
            _listenPort.value = ""
        }
    }

    fun sendHostChanged(value: String) {
        val host = value.trim()
        _sendHost.value = host
    }

    fun sendPortChanged(value: String) {
        value.toIntOrNull()?.let { _ ->
            _sendPort.value = value.trim()
        } ?: run {
            _sendPort.value = ""
        }
    }

    fun sendSourcePortChanged(value: String) {
        value.toIntOrNull()?.let { _ ->
            _sendSourcePort.value = value.trim()
        } ?: run {
            _sendSourcePort.value = ""
        }
    }

    fun sendMessageChanged(value: String) {
        _sendMessage.value = value
    }

    fun send() {
        if (sendState.value == SendState.CAN_SEND) {
            CoroutineScope(supervisor).launch(Dispatchers.IO) {
                try {
                    // Send the udp
                    val toSend = sendMessage.value.trim().toByteArray()
                    val destination = InetAddress.getByName(sendHost.value)
                    sendPort.value.toIntOrNull()?.let { port ->
                        val packet = DatagramPacket(toSend, toSend.size, destination, port)
                        val socket = sendSourcePort.value.toIntOrNull()?.let { srcPort ->
                            DatagramSocket(srcPort)
                        } ?: run {
                            DatagramSocket()
                        }
                        socket.send(packet)
                        socket.close()
                        Log.i("UDP Send", "Send packet to $destination : $port")
                        toast("Sent packet", Toast.LENGTH_SHORT)
                    }
                } catch (e: IOException) {
                    Log.e("UDP Send", "Failed to send packet: $e")
                    toast("Failed to send packet", Toast.LENGTH_LONG)
                }
            }
        }
    }

    fun getAddresses() {
        getAddressesJob?.cancel()

        getAddressesJob = CoroutineScope(supervisor).launch {
            internalGetAddresses()
        }
    }

    private fun internalGetAddresses() {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
            ?.let { connMgr ->
                connMgr.activeNetwork?.let { network ->
                    connMgr.getLinkProperties(network)?.let { properties ->
                        _ipAddresses.value = properties.linkAddresses.filterNot { address ->
                            address.address.isLoopbackAddress
                        }.mapNotNull { address ->
                            address.address.hostAddress
                        }
                    }
                }
            }
    }

    private fun toast(msg: String, length: Int) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, msg, length).show()
        }
    }
}
