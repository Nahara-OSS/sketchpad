package io.github.naharaoss.skpd.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class CommandQueue : AutoCloseable {
    private val queue = Channel<() -> Unit>(Channel.UNLIMITED)
    private val queueMutex = Mutex()
    private var closing = false

    suspend fun process() {
        for (task in queue) task()
        if (queueMutex.isLocked) queueMutex.unlock()
    }

    fun createThread(): Thread = Thread {
        runBlocking {
            process()
        }
    }

    fun <T> queue(block: () -> T): Command<T> {
        if (closing) throw Exception("Cannot queue new command while the queue is closing")

        val command = object : Command<T> {
            private var _stage = Stage.Waiting
            private var _canceled = false
            private var _result: Result<T>? = null
            private val _lock = ReentrantLock()
            private val _condition = _lock.newCondition()

            override val stage: Stage get() = _stage
            override val canceled: Boolean get() = _canceled
            override val result: Result<T> get() {
                _lock.withLock { _condition.await() }
                return _result ?: throw Exception("BUG! Internal error")
            }

            fun run() {
                if (_stage != Stage.Finished) {
                    _stage = Stage.Running
                    _result = try { Result.success(block()) } catch (e: Throwable) { Result.failure(e) }
                }

                _stage = Stage.Finished
                _lock.withLock { _condition.signalAll() }
            }

            override fun cancel() {
                if (_stage != Stage.Finished) {
                    _stage = Stage.Finished
                    _canceled = true
                    _result = Result.failure(CancellationException("Command is canceled"))
                    _lock.withLock { _condition.signalAll() }
                }
            }
        }

        queue.trySend({ command.run() })
        return command
    }

    /**
     * Wait for all commands to finish and then close the queue.
     */
    override fun close() {
        if (closing) return
        closing = true
        queue.close()
        queueMutex.tryLock()
    }

    interface Command<T> {
        val stage: Stage
        val result: Result<T>
        val canceled: Boolean
        fun cancel()
    }

    enum class Stage {
        Waiting,
        Running,
        Finished
    }
}