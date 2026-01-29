package com.babayaga.udpserver

import com.babayaga.lib.Server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class Main {}

fun main(args: Array<String>) {
    when {
        args.size < 2 -> {
            printUsage()
        }

        args[0].isBlank() -> {
            printUsage()
        }

        args[1].toIntOrNull() == null -> {
            printUsage()
        }

        else -> {
            args[1].toIntOrNull()?.let { port ->
                println("Starting UDP Server at ${args[0]}:$port")
                val supervisor = SupervisorJob()
                val server = Server()
                server.start(args[0], port, supervisor)

                val listenerJob = CoroutineScope(supervisor).launch(Dispatchers.IO) {
                    server.lastMessage.collect { lastMessage ->
                        lastMessage?.let { quad ->
                            println("(${quad.second}:${quad.third}) ${quad.first}")
                        }
                    }
                }
                println("Press any key to exit")
                readln()

                listenerJob.cancel()
                server.stop()
                println("Exiting UDP Server")
            }
        }
    }
}

private fun printUsage() {
    println("udpserver <Host> <Port>")
    println("\tHost = Valid socket host name or IP")
    println("\tPort = Valid UDP Port, i.e. Integer > 0 ")
}
