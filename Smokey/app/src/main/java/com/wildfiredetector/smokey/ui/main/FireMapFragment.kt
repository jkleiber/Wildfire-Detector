package com.wildfiredetector.smokey.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.wildfiredetector.smokey.R

class FireMapFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(SectionsPagerAdapter.ARG_SECTION_NUMBER) ?: 1)
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_fire_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

    }

}