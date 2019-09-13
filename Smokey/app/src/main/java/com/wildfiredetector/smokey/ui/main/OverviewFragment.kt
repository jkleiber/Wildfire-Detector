package com.wildfiredetector.smokey.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import com.wildfiredetector.smokey.R
import kotlinx.android.synthetic.main.fragment_overview.*

class OverviewFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(SectionsPagerAdapter.ARG_SECTION_NUMBER) ?: 1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Default to returning the overview fragment
        return inflater.inflate(R.layout.fragment_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
            // Listen for clicks on the fire report button
            bDetectWildfire.setOnClickListener { view ->
                Snackbar.make(view, "Fire Reported!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
        }
}