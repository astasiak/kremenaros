package astasiak.kremenaros

import io.vertx.core.Vertx
import io.vertx.core.net.NetSocket

fun main(args: Array<String>) {
    val accountsService = AccountsService()
    Vertx.vertx().createNetServer()
            .connectHandler { connection ->
                MainHandler(connection, accountsService)
            }
            .listen(8866)
    println("Listening!")
}

class MainHandler(val connection: NetSocket, val accountsService: AccountsService) {

    var state: ConnectionState = ConnectionState.LOGIN
    var login: String? = null

    // TODO: separate net operations from logic
    init {
        connection.handler { buffer ->
            // TODO: read line by line
            val input = buffer.getString(0, buffer.length()).trim()
            println("Got: " + input)
            val handler = selectHandler()
            try {
                handler(input)
            } catch (e: ApplicationException) {
                send("ERR ${e.code} ${e.desc}")
                if (e.terminal) {
                    connection.close()
                }
            }
        }
        send("LOGIN")
        println("Connected")
    }

    private fun selectHandler(): (String) -> Unit {
        return when (state) {
            ConnectionState.LOGIN -> this::handleLogin
            ConnectionState.PASS -> this::handlePassword
            ConnectionState.AUTHENTICATED -> this::handleCommand
        }
    }
    private fun handleLogin(login: String) {
        this.login = login
        this.state = ConnectionState.PASS
        send("PASS")
    }
    private fun handlePassword(password: String) {
        val login = this.login
        if (login != null && accountsService.authenticate(login, password)) {
            this.state = ConnectionState.AUTHENTICATED
            send("OK")
        } else {
            throw LoginException()
        }
    }
    private fun handleCommand(command: String) {
        send("Thank you for your command \"$command\"")
    }
    private fun send(message: String) {
        connection.write(message + "\n")
    }
}
enum class ConnectionState {
    LOGIN, PASS, AUTHENTICATED
}

abstract class ApplicationException(val code: Int, val desc: String, val terminal: Boolean = false)
    : RuntimeException()
class LoginException :ApplicationException(501, "Login failed", true)
