import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.LongAdder

class ServerTest {

    private val client = HttpUrlConnectionClient(connectionTimeout = 50, socketTimeout = 100)

    @Test
    fun `server should return OK hello world response`() {
        val body = "Hello, World!"
        val server = Server(port = 8080, handler = {
            Response(status = Status.OK, body = body,
                headers = HttpHeaders("Content-Length" to body.length.toString()))
        })

        val response = client.exchange(
            url = "http://localhost:8080",
            request = Request(path = "/", method = RequestMethod.GET)
        )

        assertThat(response.status).isEqualTo(Status.OK)
        assertThat(response.body).isEqualTo("Hello, World!")

        server.close()
    }

    @Test
    fun `server should refuse to accept more connections that maxIncomingConnections`() {
        val server = Server(port = 8080, maxIncomingConnections = 1, handler = {
            Thread.sleep(100)
            Response(Status.OK)
        })

        val completedRequest = LongAdder()
        val clientsThreadPool = Executors.newFixedThreadPool(3)
        repeat(times = 3) {
            clientsThreadPool.submit {
                try {
                    HttpUrlConnectionClient(connectionTimeout = 50, socketTimeout = 1000)
                        .exchange(url = "http://localhost:8080",
                            request = Request(path = "/",
                            method = RequestMethod.GET))
                    completedRequest.increment()
                } catch (e: Exception) {
                    println(e)
                    throw e
                }
            }
        }
        clientsThreadPool.awaitTermination(500, TimeUnit.MILLISECONDS)

        assertThat(completedRequest.sum()).isEqualTo(3)

        server.close()
    }

    @Test
    fun `server should echo body and headers`() {
        val body = "Echo!"
        val server = Server(port = 8080, maxIncomingConnections = 1, handler = { request ->
            Response(
                status = Status.OK,
                body = request.body,
                headers = request.headers)
        })

        val response = client.exchange(
            url = "http://localhost:8080",
            request = Request(
                path = "/",
                method = RequestMethod.POST,
                headers = HttpHeaders("X-Custom-Header" to "ABC"),
                body = body))

        assertThat(response.status).isEqualTo(Status.OK)
        assertThat(response.body).isEqualTo("Echo!")
        assertThat(response.headers["X-Custom-Header"]).containsExactly("ABC")

        server.close()
    }

    @Test
    fun `server should throw bad request on random data`() {
        val server = Server(port = 8080, maxIncomingConnections = 1, handler = { Response(Status.OK) })

        val socket = Socket("localhost", 8080)
        val input = socket.getOutputStream().writer()
        input.write("NOT VALID HTTP REQUEST\n")
        input.flush()
        val response = socket.getInputStream().bufferedReader().readText()
        socket.close()
        
        assertThat(response).contains("400 Bad Request")

        server.close()
    }
}