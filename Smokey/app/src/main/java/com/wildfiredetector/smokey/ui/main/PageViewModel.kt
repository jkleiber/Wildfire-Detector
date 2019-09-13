package com.wildfiredetector.smokey.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PageViewModel : ViewModel() {

    val ARG_SECTION_NUMBER = "section_number"

    private val _index = MutableLiveData<Int>()

    fun setIndex(index: Int) {
        _index.value = index
    }
}