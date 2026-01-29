package com.babayaga.udpclient

import com.babayaga.lib.Client

fun main(args: Array<String>) {
    when {
        args.size < 3 -> {
            printUsage()
        }

        args[0].isBlank() || args[1].isBlank() -> {
            printUsage()
        }

        args[2].toIntOrNull() == null -> {
            printUsage()
        }

        else -> {
            val srcPort = if (args.size >= 4) {
                args[3].toIntOrNull()
            } else {
                null
            }

            args[2].toIntOrNull()?.let { port ->

                Client.send(args[0].trim(), args[1].trim(), port, srcPort)
            }
        }
    }
}

private fun printUsage() {
    println("udpclient <Message> <Host> <Port> <[Src Port]>")
    println("\tMessage = The message to send")
    println("\tHost = The host to which to send the message")
    println("\tPort = The port to which to send the message, a valid UDP Port, i.e. Integer > 0 ")
    println("\tSrc Port = The port from which to send the message, random port if not specified or invalid")
}
