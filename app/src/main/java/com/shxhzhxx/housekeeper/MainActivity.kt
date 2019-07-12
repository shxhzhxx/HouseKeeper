package com.shxhzhxx.housekeeper

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import java.math.BigInteger
import java.security.MessageDigest

const val TAG = "MainActivity"
const val HOST = "39.106.101.63"
const val PORT = 3889
val md: MessageDigest by lazy { MessageDigest.getInstance("MD5") }
@Synchronized
fun String.hash() = BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')

val FOUNDATION_BLOCK by lazy { "block_head_cat_litter".hash() }

class MainActivity : AppCompatActivity() {
    private val client by lazy { PushClient({ Log.d(TAG, it.contentToString()) }, { Log.d(TAG, it.name) }) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or (View.SYSTEM_UI_FLAG_VISIBLE)
        setContentView(R.layout.activity_main)

        client.connect(HOST, PORT)
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        client.close()
    }
}

data class Block(val id: String, val prev: String, val data: String)