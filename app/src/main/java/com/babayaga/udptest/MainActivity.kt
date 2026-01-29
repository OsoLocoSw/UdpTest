package com.babayaga.udptest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.babayaga.udptest.ui.theme.UdpTestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UdpTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    UdpElements(innerPadding)
                }
            }
        }
    }
}

@Composable
fun UdpElements(innerPadding: PaddingValues) {
    val viewModel = ViewModel(LocalContext.current)
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .verticalScroll(scrollState)
    ) {
        Text("UDP Tester")
        SendElements(viewModel)
        HorizontalDivider(thickness = 3.dp)
        ReceiveElements(viewModel)
        HorizontalDivider(thickness = 3.dp)
        IpElements(viewModel)
    }
}

@Composable
fun SendElements(viewModel: ViewModel) {
    val sendState = viewModel.sendState.collectAsState().value

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = viewModel.sendHost.collectAsState().value,
            onValueChange = { value -> viewModel.sendHostChanged(value) },
            label = { Text("Host") }
        )
        OutlinedTextField(
            value = viewModel.sendPort.collectAsState().value,
            onValueChange = { value -> viewModel.sendPortChanged(value) },
            label = { Text("Destination Port") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )
        OutlinedTextField(
            value = viewModel.sendSourcePort.collectAsState().value,
            onValueChange = { value -> viewModel.sendSourcePortChanged(value) },
            label = { Text("Source Port") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )
    }

    OutlinedTextField(
        value = viewModel.sendMessage.collectAsState().value,
        onValueChange = { value -> viewModel.sendMessageChanged(value) },
        label = { Text("Message") }
    )

    Button(
        enabled = sendState == ViewModel.SendState.CAN_SEND,
        onClick = {
            viewModel.send()
        }
    ) {
        Text("Send")
    }
}

@Composable
fun ReceiveElements(viewModel: ViewModel) {
    val listenState = viewModel.listenState.collectAsState().value

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = viewModel.listenerHost.collectAsState().value,
            onValueChange = { value -> viewModel.listenerHostChanged(value) },
            label = { Text("Host") },
            enabled = listenState != ViewModel.ListenState.LISTENING
        )
        OutlinedTextField(
            value = viewModel.listenerPort.collectAsState().value,
            onValueChange = { value -> viewModel.listenerPortChanged(value) },
            label = { Text("Port") },
            enabled = listenState != ViewModel.ListenState.LISTENING,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            enabled = listenState == ViewModel.ListenState.CAN_LISTEN,
            onClick = {
                viewModel.listen()
            }
        ) {
            Text("Listen")
        }
        Button(
            enabled = listenState == ViewModel.ListenState.LISTENING,
            onClick = {
                viewModel.stop()
            }
        ) {
            Text("Stop")
        }
    }

    val messages = viewModel.messages.collectAsState().value
    val title = StringBuilder("Received Messages")
    if (messages.isNotEmpty()) {
        title.append("(")
        title.append(messages.size)
        title.append(")")
    }
    title.append(":")
    Text(title.toString())
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column {
            if (messages.isEmpty()) {
                Text("No messages received")
            } else {
                messages.forEach { message ->
                    Text(message)
                }
            }
        }
        Button(
            enabled = messages.isNotEmpty(),
            onClick = {
                viewModel.clearMessages()
            }
        ) {
            Text("Clear Messages")
        }
    }
}

@Composable
fun IpElements(viewModel: ViewModel) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("IP Addresses of the device:")
        Button(
            onClick = {
                viewModel.getAddresses()
            }
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Refresh IP Addresses",
            )
        }
    }

    val ipAddresses: List<String> = viewModel.ipAddresses.collectAsState().value
    val inset = Modifier.padding(start = 16.dp)
    if (ipAddresses.isEmpty()) {
        Text("No Addresses", modifier = inset)
    } else {
        ipAddresses.forEach { address ->
            Text(address, modifier = inset)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UdpPreview() {
    UdpTestTheme {
        UdpElements(PaddingValues(8.dp))
    }
}

