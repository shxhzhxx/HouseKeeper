package com.shxhzhxx.housekeeper

import java.nio.ByteBuffer
import java.util.concurrent.Executors


const val HOST = "39.106.101.63"
const val PORT = 3889

fun ByteBuffer.getString() = String(ByteArray(int).also { get(it) })

class BlockRepository(private val dao: BlockDao) {
    private val foundationBlockHash = Block("", "block_head_cat_litter").hash
    private val dbThread = Executors.newSingleThreadExecutor()
    private val client = PushClient({ data ->
        handle(data)
    }, { state ->
        println(state.name)
    }).apply { connect(HOST, PORT) }

    init {
        dbThread.execute { requireSubChain(head()) }
    }

    private fun handle(bytes: ByteArray) {
        dbThread.execute {
            try {
                ByteBuffer.wrap(bytes).also { buffer ->
                    when (buffer.get().toInt()) {
                        0 -> {
                            //add
                            fun insertAll(head: String, size: Int) {
                                var currentHead = head
                                repeat(size) {
                                    dao.insert(Block(currentHead, buffer.getString()).also { currentHead = it.hash })
                                }
                            }

                            val head = buffer.getString()
                            if (head == head()) {
                                insertAll(head, buffer.int)
                            } else {
                                val sub = dao.subChain(head)
                                if (sub.isEmpty()) {
                                    requireSubChain(head)
                                } else {
                                    val size = buffer.int
                                    if (sub.size <= size) {
                                        dao.delete(*sub.toTypedArray())
                                        insertAll(head, size)
                                    } else {
                                        client.broadcast(blockByteArray(head, *sub.map { it.data }.toTypedArray()))
                                    }
                                }
                            }
                        }
                        1 -> {//sub chain after head
                            val head = buffer.getString()
                            val data = dao.subChain(head).map { it.data }.toTypedArray()
                            if (data.isNotEmpty()) client.broadcast(blockByteArray(head, *data))
                        }
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun newBlock(data: String) {
        dbThread.execute {
            client.broadcast(blockByteArray(head(), data))
        }
    }

    fun observableList() = dao.observableList()

    fun close() {
        dbThread.shutdown()
        client.close()
    }

    private fun requireSubChain(head: String) {
        val headBytes = head.toByteArray()
        val byteArray = ByteArray(5 + headBytes.size)
        ByteBuffer.wrap(byteArray).put(1).putInt(headBytes.size).put(headBytes)
        client.broadcast(byteArray)
    }

    private fun head() = dao.head() ?: foundationBlockHash

    private fun blockByteArray(head: String, vararg data: String): ByteArray {
        val headBytes = head.toByteArray()
        val dataBytesList = data.map { it.toByteArray() }
        val byteArray = ByteArray(headBytes.size + dataBytesList.sumBy { it.size + 4 } + 9)
        ByteBuffer.wrap(byteArray).put(0).putInt(headBytes.size).put(headBytes).putInt(dataBytesList.size).apply {
            for (dataBytes in dataBytesList) {
                putInt(dataBytes.size).put(dataBytes)
            }
        }
        return byteArray
    }
}