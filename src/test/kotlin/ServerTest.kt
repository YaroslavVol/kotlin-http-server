import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection
import java.net.URL


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
        assertThat(response).isEqualTo("Hello World!")

        server.close()
    }
}