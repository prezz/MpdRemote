package net.prezz.mpr.ui.player

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import net.prezz.mpr.R

class PlayerPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    private val context: Context = fragmentActivity

    fun getTitle(position: Int): String {
        return when (position) {
            0 -> context.getString(R.string.player_playlist)
            1 -> context.getString(R.string.player_remote_control)
            else -> ""
        }
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PlayerPlaylistFragment()
            1 -> PlayerControlFragment()
            else -> throw IllegalStateException("Invalid fragment position: $position")
        }
    }

    override fun getItemCount(): Int {
        return 2
    }
}
