package astasiak.kremenaros

class SampleException : ApplicationException(427, "Just a sample exception")
class SampleGame : Game {
    override fun handleCommand(team: String, command: String): List<String> {
        if (command.startsWith("E")) {
            throw SampleException()
        }
        return listOf("Thank you!", "Hello, team " + team, "Command: " + command)
    }
}


fun main(args: Array<String>) {
    val sampleGame = SampleGame()
    runGame(sampleGame)
}