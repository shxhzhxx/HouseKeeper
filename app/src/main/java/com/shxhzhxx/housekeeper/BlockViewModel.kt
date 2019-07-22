package com.shxhzhxx.housekeeper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope


class BlockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository =
        BlockRepository(
            AppDatabase.getInstance(application).blockDao(),
            viewModelScope.coroutineContext
        )


    fun newBlock(data: String)=repository.newBlock(data)

    fun list() = repository.list()

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}