package astasiak.kremenaros

data class Account(val login: String, val password: String)

class AccountsService {
    private val accounts = listOf(
            Account("abc", "paSSword"),
            Account("def", "paSSword"))

    fun authenticate(login: String, password: String): Boolean {
        val matching = accounts.filter { it.login == login && it.password == password }
        return matching.size == 1
    }
}