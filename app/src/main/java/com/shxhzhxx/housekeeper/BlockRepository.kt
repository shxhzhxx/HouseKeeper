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

    private var offset = 1

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
                                    dao.insert(Block(currentHead, buffer.getString()).also {
                                        currentHead = it.hash;println("insert $it")
                                    })
                                }
                            }

                            val head = buffer.getString()
                            if (head == head()) {
                                offset = 1
                                insertAll(head, buffer.int)
                            } else {
                                val sub = dao.subChain(head)
                                if (sub.isEmpty()) {
                                    offset *= 2
                                    requireSubChain(head, offset)
                                } else {
                                    offset = 1
                                    val size = buffer.int
                                    if (sub.size <= size) {
                                        println("delete $sub")
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
                            val headOffset = if (head == foundationBlockHash) foundationBlockHash else
                                dao.headOffset(head, buffer.int)
                            if (headOffset == null) {
                                offset *= 2
                                requireSubChain(head, offset)
                            } else {
                                offset = 1
                                val data = dao.subChain(headOffset).map { it.data }.toTypedArray()
                                if (data.isNotEmpty())
                                    client.broadcast(blockByteArray(headOffset, *data))
                            }
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
            val head = head()
            dao.insert(Block(head, data))
            client.broadcast(blockByteArray(head, data))
        }
    }

    fun list() = dao.list()

    fun close() {
        dbThread.shutdown()
        client.close()
    }

    private fun requireSubChain(head: String, offset: Int = 0) {
        println("requireSubChain head:$head   offset:$offset")
        val headBytes = head.toByteArray()
        val byteArray = ByteArray(9 + headBytes.size)
        ByteBuffer.wrap(byteArray).put(1).putInt(headBytes.size).put(headBytes).putInt(offset)
        client.broadcast(byteArray)
    }

    private fun head() = dao.head() ?: foundationBlockHash

    private fun blockByteArray(head: String, vararg data: String): ByteArray {
        println("blockByteArray head:$head   data:${data.toList()}")
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