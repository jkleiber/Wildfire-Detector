package com.wildfiredetector.smokey.ui.main

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.wildfiredetector.smokey.R

private val TAB_TITLES = arrayOf(
    R.string.tab_text_1,
    R.string.tab_text_2
)

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(private val context: Context, fm: FragmentManager) :
    FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        // getItem is called to instantiate the fragment for the given page.
        // Return a Fragment (defined as a static inner class below).
        var fragment: Fragment

        when(position)
        {
            1 -> {
                fragment = FireMapFragment()
            }
            else -> {
                fragment = OverviewFragment()
            }
        }

        fragment.arguments = Bundle().apply {
            putInt(ARG_SECTION_NUMBER, position + 1)
        }

        return fragment
        //return MainPageFragment.newInstance(position + 1)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.resources.getString(TAB_TITLES[position])
    }

    override fun getCount(): Int {
        // Show 2 total pages.
        return 2
    }

    companion object {
        const val ARG_SECTION_NUMBER = "section_number"
    }
}