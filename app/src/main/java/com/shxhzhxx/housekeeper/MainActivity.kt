package com.shxhzhxx.housekeeper

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.math.BigInteger
import java.security.MessageDigest
import kotlin.coroutines.CoroutineContext

const val HOST = "39.106.101.63"
const val PORT = 3889
val md: MessageDigest by lazy { MessageDigest.getInstance("MD5") }
@Synchronized
fun String.hash() = BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')

val FOUNDATION_BLOCK by lazy { Block("block_head_cat_litter") }

class MainActivity : AppCompatActivity(), CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    private val json by lazy { Json(JsonConfiguration.Stable) }
    //    private val dao by lazy { AppDatabase.getInstance(this).blockDao() }
    private val client by lazy {
        PushClient({ data ->
            val msg = try {
                json.parse(Message.serializer(), String(data))
            } catch (e: Throwable) {
                return@PushClient
            }
            runOnUiThread {
                when (msg.cmd) {
                    Cmd.ADD -> {
//                        dao.insert(msg.block)
                    }
                    Cmd.NEXT -> {
                    }
                    Cmd.PREV -> {
                    }
                }
                println(msg)
            }
        }, { state ->
            println(state.name)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or (View.SYSTEM_UI_FLAG_VISIBLE)
        setContentView(R.layout.activity_main)

        client.connect(HOST, PORT)
        launch {
            //            repeat(10) {
//                delay(1000)
//                client.broadcast(
//                    json.stringify(
//                        Message.serializer(),
//                        Message(Cmd.ADD, Block("a", "b", "c"))
//                    ).also { println(it) }.toByteArray()
//                )
//            }


//            Telephony.Sms.Intents.getMessagesFromIntent(
//                receiveCoroutine(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
//            ).mapNotNull { println(it.displayMessageBody) }
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        client.close()
        job.cancel()
    }
}

//@Entity(
//    tableName = "block",
//    primaryKeys = ["hash"],
//    indices = [Index(value = ["prev"])]
//)
@Serializable
data class Block(
    val data: String,
    val prev: String = "",
    val hash: String = "${prev.hash()}$data".hash()
)

@Serializable
data class Message(val cmd: Cmd, val block: Block)

enum class Cmd {
    ADD, NEXT, PREV
}

//@Dao
//interface BlockDao {
//    @Insert(onConflict = OnConflictStrategy.ABORT)
//    fun insertAll(words: List<Block>)
//
//    @Insert(onConflict = OnConflictStrategy.ABORT)
//    fun insert(vararg words: Block)
//
//    @Query("DELETE FROM block")
//    fun clear()
//}

//fun FragmentActivity.receive(
//    action: String,
//    lifecycle: Lifecycle = this.lifecycle,
//    onReceive: (Intent) -> Unit
//) {
//    val receiver by lazy {
//        object : BroadcastReceiver() {
//            override fun onReceive(context: Context, intent: Intent) {
//                if (intent.action == action)
//                    onReceive(intent)
//            }
//        }
//    }
//    lifecycle.addObserver(object : LifecycleObserver {
//        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
//        fun onResume() {
//            registerReceiver(receiver, IntentFilter(action))
//        }
//
//        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
//        fun onPause() {
//            unregisterReceiver(receiver)
//        }
//    })
//}
//
//suspend fun FragmentActivity.receiveCoroutine(
//    action: String,
//    lifecycle: Lifecycle = this.lifecycle
//) = suspendCancellableCoroutine<Intent> { continuation ->
//    receive(action, lifecycle) { continuation.resume(it) }
//}