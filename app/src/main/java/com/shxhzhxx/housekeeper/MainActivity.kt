package com.shxhzhxx.housekeeper

import android.graphics.Color
import android.os.Bundle
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


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or (View.SYSTEM_UI_FLAG_VISIBLE)
        setContentView(R.layout.activity_main)

        val adapter = BlockAdapter()
        blocks.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        blocks.adapter = adapter

        val viewModel = ViewModelProviders.of(this).get(BlockViewModel::class.java)

        viewModel.observableList().observe(this, Observer { list ->
            adapter.list = list.reversed()
        })

        var cnt = 0
        add.setOnClickListener {
            viewModel.newBlock("魏舒婷${cnt++}")
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }
}

class BlockAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var list: List<Block> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = object :
        RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_block, parent, false)) {}

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder.itemView as? TextView)?.text = list[position].data
    }
}