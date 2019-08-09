package com.shxhzhxx.housekeeper

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlin.coroutines.CoroutineContext


class MainActivity : AppCompatActivity(), CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    private val json = Json(JsonConfiguration.Stable)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = getColor(R.color.colorPrimary)
        setContentView(R.layout.activity_main)

        val adapter = MissionAdapter()
        missions.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        missions.adapter = adapter
        missions.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (adapter.list.isNotEmpty()) missions.smoothScrollToPosition(adapter.list.size - 1)
        }

        val viewModel = ViewModelProviders.of(this).get(BlockViewModel::class.java)
        viewModel.list().observe(this, Observer { list ->
            adapter.list = list.mapNotNull {
                try {
                    json.parse(Mission.serializer(), it.data)
                } catch (e: Throwable) {
                    null
                }
            }.toMutableList()
            if (adapter.itemCount != 0) missions.scrollToPosition(adapter.itemCount - 1)
        })


//        send.setOnClickListener {
//            val msg = input.text?.toString()
//            if (msg.isNullOrEmpty()) return@setOnClickListener
//            viewModel.newBlock(msg)
//            input.text = null
//        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

@Serializable
data class Mission(val date: String, val msg: String)

class MissionHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val date: TextView = itemView.findViewById(R.id.date)
    val msg: TextView = itemView.findViewById(R.id.msg)
    val state: Spinner = itemView.findViewById(R.id.state)
}

class MissionAdapter : RecyclerView.Adapter<MissionHolder>() {
    var list: MutableList<Mission> = mutableListOf()
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        MissionHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_block, parent, false))

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: MissionHolder, position: Int) {
        holder.msg.text = list[position].msg
        holder.date.text = list[position].date
    }
}