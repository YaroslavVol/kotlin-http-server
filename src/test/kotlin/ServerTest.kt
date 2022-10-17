import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.LongAdder


class ServerTest {

    @Test
    fun `server should return OK hello world response`() {
        val server = Server(8080)

        val connection = URL("http://localhost:8080/").openConnection() as HttpURLConnection
        connection.connect()
        val responseCode = connection.responseCode
        val response = connection.inputStream.bufferedReader().readText()
        connection.disconnect()

        assertThat(responseCode).isEqualTo(200)
        assertThat(response).isEqualTo("Hello, World!")

        server.close()
    }

    @Test
    fun `server should refuse to accept more connections that maxIncomingConnections`() {
        val server = Server(port = 8080, maxIncomingConnections = 1)

        val completedRequest = LongAdder()

        val clientsThreadPool = Executors.newFixedThreadPool(3)
        repeat(times = 3) {
            clientsThreadPool.submit {
                try {
                    val connection = URL("http://localhost:8080/").openConnection() as HttpURLConnection
                    connection.connectTimeout = 1000
                    connection.connect()
                    if (connection.responseCode == 200) {
                        completedRequest.increment()
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw e
                }
            }
            Thread.sleep(500)
        }
        clientsThreadPool.awaitTermination(20, TimeUnit.SECONDS)

        assertThat(completedRequest.sum()).isEqualTo(3)

        server.close()
    }
}