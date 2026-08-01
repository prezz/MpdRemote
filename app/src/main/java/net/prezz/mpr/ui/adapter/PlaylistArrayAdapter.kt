package net.prezz.mpr.ui.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import net.prezz.mpr.R
import net.prezz.mpr.databinding.ViewListItemPlaylistBinding
import net.prezz.mpr.model.PlayerState

class PlaylistArrayAdapter(
    context: Context,
    textViewResourceId: Int,
    objects: List<PlaylistAdapterEntity>,
) : ArrayAdapter<PlaylistAdapterEntity>(context, textViewResourceId, objects) {

    private var currentSong = -1
    private var playerState = PlayerState.STOP

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding: ViewListItemPlaylistBinding
        val tag: ViewTag

        if (convertView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            binding = ViewListItemPlaylistBinding.inflate(inflater, parent, false)
            tag = ViewTag(R.drawable.ic_drag, false, binding.playlistListItemText1.textColors, binding)
            binding.root.tag = tag
        } else {
            tag = convertView.tag as ViewTag
            binding = tag.binding
        }

        var drawableId = R.drawable.ic_drag
        if (position == currentSong) {
            drawableId = when (playerState) {
                PlayerState.PLAY -> R.drawable.ic_play
                PlayerState.STOP -> R.drawable.ic_stop
                PlayerState.PAUSE -> R.drawable.ic_pause
            }
        }

        if (tag.drawable != drawableId) {
            binding.playlistListItemDragImage.setImageResource(drawableId)
            tag.drawable = drawableId
        }

        val entity = getItem(position)!!
        val priority = entity.getPriority()

        binding.playlistListItemText1.text = entity.getSubText()
        binding.playlistListItemText2.text = entity.getText()
        binding.playlistListItemTime.text = entity.getTime()
        binding.playlistListItemPriority.text = priority ?: ""

        if (entity.prioritized() != tag.prioritized) {
            if (entity.prioritized()) {
                val color = getPrioritizedColor()
                binding.playlistListItemText1.setTextColor(color)
                binding.playlistListItemText2.setTextColor(color)
            } else {
                binding.playlistListItemText1.setTextColor(tag.standardColors)
                binding.playlistListItemText2.setTextColor(tag.standardColors)
            }
            tag.prioritized = entity.prioritized()
        }

        return binding.root
    }

    fun setData(data: Array<PlaylistAdapterEntity>?, currentSong: Int, playerState: PlayerState) {
        this.currentSong = currentSong
        this.playerState = playerState

        setNotifyOnChange(false)
        clear()
        if (data != null) {
            addAll(*data)
        }
        notifyDataSetChanged()
    }

    private fun getPrioritizedColor(): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.redFocusColor, typedValue, true)
        return typedValue.data
    }

    private class ViewTag(
        var drawable: Int,
        var prioritized: Boolean,
        val standardColors: ColorStateList,
        val binding: ViewListItemPlaylistBinding,
    )
}
