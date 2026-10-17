/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import androidx.annotation.Keep
import fr.sygix.sygixos.IPrivilegedOps
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.system.exitProcess

data class CommandOutcome(val code: Int, val output: String)

fun interface CommandRunner {
    fun run(command: String): CommandOutcome
}

object PrivilegedReply {
    const val Timeout = -1
    const val Refused = -2

    fun handle(command: String, runner: CommandRunner): String =
        encode(if (PrivilegedCommands.allowed(command)) runner.run(command) else CommandOutcome(Refused, "Command refused"))

    fun encode(outcome: CommandOutcome): String = "${outcome.code}\n${outcome.output}"

    fun decode(reply: String?): CommandOutcome? {
        val code = reply?.substringBefore('\n')?.toIntOrNull() ?: return null
        return CommandOutcome(code, reply.substringAfter('\n', ""))
    }
}

class ProcessCommandRunner(private val timeoutMs: Long = 10_000) : CommandRunner {
    override fun run(command: String): CommandOutcome {
        val process = try {
            ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start()
        } catch (failure: Exception) {
            return CommandOutcome(127, failure.message.orEmpty())
        }
        runCatching { process.outputStream.close() }
        val output = ByteArrayOutputStream()
        val reader = thread(isDaemon = true) { runCatching { process.inputStream.use { it.copyTo(output) } } }
        if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            reader.join(1_000)
            return CommandOutcome(PrivilegedReply.Timeout, output.toString(Charsets.UTF_8))
        }
        reader.join(1_000)
        return CommandOutcome(process.exitValue(), output.toString(Charsets.UTF_8))
    }
}

@Keep
class PrivilegedOpsService internal constructor(private val runner: CommandRunner) : IPrivilegedOps.Stub() {
    @Keep
    constructor() : this(ProcessCommandRunner())

    override fun run(command: String?): String = PrivilegedReply.handle(command.orEmpty(), runner)

    override fun destroy() {
        exitProcess(0)
    }
}
