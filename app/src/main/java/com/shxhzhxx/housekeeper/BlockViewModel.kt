package com.shxhzhxx.housekeeper

import android.app.Application
import androidx.lifecycle.AndroidViewModel


class BlockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository =
        BlockRepository(AppDatabase.getInstance(application).blockDao())

    fun newBlock(data: String) {
        repository.newBlock(data)
    }

    fun list() = repository.list()

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}