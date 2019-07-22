package com.shxhzhxx.housekeeper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext


const val HOST = "39.106.101.63"
const val PORT = 3889

fun ByteBuffer.getString() = String(ByteArray(int).also { get(it) })

class BlockRepository(private val dao: BlockDao, override val coroutineContext: CoroutineContext) : CoroutineScope {
    private val foundationBlockHash = Block("", "block_head_cat_litter").hash
    private val client = PushClient({ data ->
        launch {
            try {
                ByteBuffer.wrap(data).also { buffer ->
                    when (buffer.get().toInt()) {
                        0 -> {//add
                            val block = Block(buffer.getString(), buffer.getString())
                            if (block.prev == head()) //检验合法性
                                dao.insert(block)
                        }
                        1 -> {//next

                        }
                        2 -> {//prev

                        }
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }, { state ->
        println(state.name)
    }).apply { connect(HOST, PORT) }

    fun newBlock(data: String) = launch {
        client.broadcast(blockByteArray(head().toByteArray(), data.toByteArray()))
    }

    fun list() = dao.list()

    fun close() {
        client.close()
    }


    private suspend fun head() = dao.head() ?: foundationBlockHash

    private fun blockByteArray(prev: ByteArray, data: ByteArray) = ByteArray(prev.size + data.size + 9)
        .also { ByteBuffer.wrap(it).put(0).putInt(prev.size).put(prev).putInt(data.size).put(data) }
}