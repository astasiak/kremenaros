package astasiak.kremenaros

import io.vertx.core.Vertx
import io.vertx.core.net.NetSocket

fun runGame(game: Game) {
    val accountsService = AccountsService()
    Vertx.vertx().createNetServer()
            .connectHandler { connection ->
                MainHandler(game, connection, accountsService)
            }
            .listen(8866)
    println("Listening!")
}

class MainHandler(
        val game: Game,
        val connection: NetSocket,
        val accountsService: AccountsService) {

    enum class ConnectionState {
        LOGIN, PASS, AUTHENTICATED
    }

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
        if (accountsService.authenticate(takeLogin(), password)) {
            this.state = ConnectionState.AUTHENTICATED
            send("OK")
        } else {
            throw LoginException()
        }
    }
    private fun handleCommand(command: String) {
        val output = game.handleCommand(takeLogin(), command)
        send("OK")
        for (line in output) {
            send(line)
        }
    }
    private fun send(message: String) {
        connection.write(message + "\n")
    }
    private fun takeLogin(): String {
        return login ?: throw InternalException()
    }
}

abstract class ApplicationException(val code: Int, val desc: String, val terminal: Boolean = false)
    : RuntimeException()
class LoginException : ApplicationException(401, "Login failed", true)
class InternalException : ApplicationException(501, "Internal exception", true)

interface Game {
    fun handleCommand(team: String, command: String): List<String>
}
