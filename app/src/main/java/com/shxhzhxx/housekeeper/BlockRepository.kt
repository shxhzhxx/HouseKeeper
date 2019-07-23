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
        dbThread.execute {
            val head = head().toByteArray()
            val byteArray = ByteArray(5 + head.size)
            ByteBuffer.wrap(byteArray).put(1).putInt(head.size).put(head)
            client.broadcast(byteArray)
        }
    }

    private fun handle(data: ByteArray) {
        dbThread.execute {
            try {
                ByteBuffer.wrap(data).also { buffer ->
                    when (buffer.get().toInt()) {
                        0 -> {//add
//                            val prev = buffer.getString()
                            val block = Block(buffer.getString(), buffer.getString())
                            if (block.prev == head()) //检验合法性
                                dao.insert(block)
                        }
                        1 -> {//next
                            for (sub in dao.subChain(buffer.getString())) {
                                client.broadcast(blockByteArray(sub.prev, sub.data))
                            }
                        }
                        2 -> {//info
                            dao.get(buffer.getString())
                                ?.also { info -> client.broadcast(blockByteArray(info.prev, info.data)) }
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


    private fun head() = dao.head() ?: foundationBlockHash

    private fun blockByteArray(prev: String, data: String): ByteArray {
        val prevArr = prev.toByteArray()
        val dataArr = data.toByteArray()
        val byteArray = ByteArray(prevArr.size + dataArr.size + 9)
        ByteBuffer.wrap(byteArray).put(0).putInt(prevArr.size).put(prevArr).putInt(dataArr.size).put(dataArr)
        return byteArray
    }
}