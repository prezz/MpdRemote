package net.prezz.mpr.ui.library

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import net.prezz.mpr.R

class LibraryPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    private val context: Context = fragmentActivity

    fun getTitle(position: Int): String {
        return when (position) {
            0 -> context.getString(R.string.library_albums)
            1 -> context.getString(R.string.library_genres)
            2 -> context.getString(R.string.library_musicians)
            3 -> context.getString(R.string.library_files)
            else -> ""
        }
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> LibraryAlbumFragment()
            1 -> LibraryGenreFragment()
            2 -> LibraryArtistFragment()
            3 -> LibraryUriFragment()
            else -> throw IllegalStateException("Invalid fragment position: $position")
        }
    }

    override fun getItemCount(): Int {
        return FRAGMENT_COUNT
    }

    companion object {
        const val FRAGMENT_COUNT = 4
    }
}
