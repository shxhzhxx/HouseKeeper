package com.shxhzhxx.housekeeper

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity(), CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or (View.SYSTEM_UI_FLAG_VISIBLE)
        setContentView(R.layout.activity_main)

        val adapter = BlockAdapter()
        blocks.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        blocks.adapter = adapter
        blocks.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (adapter.list.isNotEmpty()) blocks.smoothScrollToPosition(adapter.list.size - 1)
        }

        val viewModel = ViewModelProviders.of(this).get(BlockViewModel::class.java)
        viewModel.list().observe(this, Observer { list ->
            adapter.list = list.toMutableList()
            if (list.isNotEmpty()) blocks.scrollToPosition(list.size - 1)
        })

        input.addTextChangedListener(object : TextWatcher {
            var aim: Float? = null
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    if (aim != 0f) {
                        aim = 0f
                        send.animate().apply { cancel() }.translationX(send.width.toFloat()).alpha(0f)
                    }
                } else {
                    if (aim != 1f) {
                        aim = 1f
                        send.animate().apply { cancel() }.translationX(0f).alpha(1f)
                    }
                }
            }
        })

        send.setOnClickListener {
            val msg = input.text?.toString()
            if (msg.isNullOrEmpty()) return@setOnClickListener
            viewModel.newBlock(msg)
            input.text = null
        }

        launch {
            send.waitForLayout()
            send.translationX = send.width.toFloat()
            send.alpha = 0f
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

class BlockAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var list: MutableList<Block> = mutableListOf()
        set(value) {
            val intersect = field.intersect(value)
            val removeIndexes = field.map { !intersect.contains(it) }
            field = field.filter { intersect.contains(it) }.toMutableList()

            removeIndexes.indices.reversed().forEach { index ->
                if (removeIndexes[index]) {
                    notifyItemRemoved(index)
                }
            }
            val correctOrder = value.filter { intersect.contains(it) }
            for (i in field.indices) {
                if (field[i] != correctOrder[i]) {
                    val n = correctOrder.indexOf(field[i])
                    field.add(i, field.removeAt(n))
                    notifyItemMoved(n, i)
                }
            }

            val insertIndexes = value.map { !intersect.contains(it) }
            field = value
            insertIndexes.forEachIndexed { index, b ->
                if (b) {
                    notifyItemInserted(index)
                }
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = object :
        RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_block, parent, false)) {}

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder.itemView as? TextView)?.text = list[position].data
    }
}

suspend fun View.waitForLayout() {
    while (!isLaidOut) yield()
}