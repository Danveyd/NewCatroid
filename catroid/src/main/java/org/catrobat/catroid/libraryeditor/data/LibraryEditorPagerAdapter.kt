package org.catrobat.catroid.libraryeditor.data

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class LibraryEditorPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CodeEditorFragment()
            1 -> FormulasEditorFragment()
            2 -> BricksEditorFragment()
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}