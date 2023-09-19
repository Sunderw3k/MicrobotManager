import io.javalin.Javalin
import io.javalin.websocket.WsContext
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

data class Bot(val name: String, val password: String, val world: Int)

val bots = mutableListOf(
    Bot("username", "password", 302),
    Bot("username2", "password2", 301)
)
val loggedBots = mutableMapOf<WsContext, Bot>()
val clients = mutableListOf<WsContext>()

fun main(args: Array<String>) {
    Javalin.create().start(1337).also { app ->
        app.get("/") {
            it.result("Running clients: \n${loggedBots.entries.joinToString(separator = "\n") { c -> c.key.sessionId }}")
        }

        app.get("/bot/{id}") {
            val bot = getBotFromId(it.pathParam("id"))
            it.result(
                "Name: ${bot.name}\nPassword: ${bot.password}\nWorld: ${bot.world}"
            )
        }

        app.ws("/socket/bot") { ws ->
            ws.onConnect {
                clients.add(it);
            }

            ws.onMessage { ctx ->
                println("Message: ${ctx.message()}")

                if (ctx.message() == "LOGIN") {
                    val bot = loggedBots.getOrPut(ctx) {
                        bots.first { it !in loggedBots.values }
                    }

                    ctx.send("LOGIN:${bot.name}:${encryptPassword(bot.password)}:${bot.world}")
                }
            }

            ws.onClose {
                loggedBots.remove(it)
                clients.remove(it)
            }
        }
    }

    Executors.newSingleThreadScheduledExecutor()
        .scheduleAtFixedRate({ println("Connected clients: ${clients.size}"); clients.forEach { it.sendPing() } }, 0, 5, TimeUnit.SECONDS)
}

fun encryptPassword(password: String): String {
    return Cipher.getInstance("AES").let { cipher ->
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec("microbot12345678".toByteArray(), "AES"))
        val encrypted = cipher.doFinal(password.toByteArray())
        Base64.getEncoder().encodeToString(encrypted)
    }
}

fun getBotFromId(id: String): Bot = loggedBots.entries.first { it.key.sessionId == id }.value