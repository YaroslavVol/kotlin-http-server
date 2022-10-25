import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ServerTest {

    private val client = HttpUrlConnectionClient(connectionTimeout = 50, socketTimeout = 100)

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
}