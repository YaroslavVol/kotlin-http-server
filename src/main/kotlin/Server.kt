import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Server(val port: Int): AutoCloseable {
    private val logger = LoggerFactory.getLogger(Server::class.java)

    private val serverSocket = ServerSocket(port)
    private val threadPool = Executors.newSingleThreadExecutor()

    init {
        threadPool.submit {
            try {
                handleConnections()
            } catch (e: Exception) {
                logger.error("Problem while handling connections", e)
            }
        }
    }

    private fun handleConnections() {
        while (!serverSocket.isClosed) {
            serverSocket.accept().use {
                try {
                    handleConnection(it)
                } catch (e: Exception) {
                    logger.warn("Problem while handling connection", e)
                }
            }
        }
    }

    private fun handleConnection(socket: Socket) {
        val input = socket.getInputStream().bufferedReader()
        val output = PrintWriter(socket.getOutputStream())

        val requestLine = input.readLine()
        logger.info("Received request: {}", requestLine)

        output.print("""
            HTTP/1.1 200 OK
           
            Hello World!""".trimIndent())
        output.flush()
    }

    override fun close() {
        serverSocket.close()
        threadPool.shutdown()
        threadPool.awaitTermination(1, TimeUnit.SECONDS)
    }
}

fun main() {
    Server(8080)
}