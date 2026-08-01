package net.prezz.mpr.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import net.prezz.mpr.R
import net.prezz.mpr.databinding.FragmentPlayerPlaylistBinding
import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.PlayerState
import net.prezz.mpr.model.PlayerStatus
import net.prezz.mpr.model.PlaylistEntity
import net.prezz.mpr.model.command.ClearPlaylistCommand
import net.prezz.mpr.model.command.DeleteFromPlaylistCommand
import net.prezz.mpr.model.command.DeleteMultipleFromPlaylistCommand
import net.prezz.mpr.model.command.MoveInPlaylistCommand
import net.prezz.mpr.model.command.PauseCommand
import net.prezz.mpr.model.command.PlayCommand
import net.prezz.mpr.model.command.PrioritizeUriCommand
import net.prezz.mpr.model.command.ShuffleCommand
import net.prezz.mpr.model.command.UnprioritizeCommand
import net.prezz.mpr.model.command.UpdatePrioritiesCommand
import net.prezz.mpr.ui.adapter.PlaylistAdapterEntity
import net.prezz.mpr.ui.adapter.PlaylistArrayAdapter
import net.prezz.mpr.ui.helpers.Boast
import net.prezz.mpr.ui.helpers.UpdatePlayDataHelper
import net.prezz.mpr.ui.helpers.UriFilterHelper
import net.prezz.mpr.ui.library.filtered.FilteredActivity
import net.prezz.mpr.ui.library.filtered.FilteredAlbumAndTitleActivity
import net.prezz.mpr.ui.library.filtered.FilteredTrackAndTitleActivity
import net.prezz.mpr.ui.view.DragListView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent

class PlayerPlaylistFragment : Fragment(), PlayerFragment, OnItemClickListener {

    private var _binding: FragmentPlayerPlaylistBinding? = null
    private val binding get() = _binding!!

    private var playerStatus = PlayerStatus(false)
    private var adapterEntities: Array<PlaylistAdapterEntity>? = null

    private lateinit var uriFilterHelper: UriFilterHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlayerPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createEntityAdapter()

        val listView = findListView()

        listView.onItemClickListener = this

        listView.setDragListener(EntityDragListener())
        listView.setDropListener(EntityDropListener())
        listView.setRemoveListener(EntityRemoveListener())

        listView.setOnItemLongClickListener { _, _, position, _ ->
            showContextMenu(position)
            true
        }

        showUpdatingIndicator()

        uriFilterHelper = UriFilterHelper(requireActivity(), UriFilterHelper.UriFilterChangedListener { })

        (requireActivity() as PlayerActivity).attachFragment(this, FRAGMENT_POSITION)
    }

    override fun onDestroyView() {
        (requireActivity() as PlayerActivity).detachFragment(FRAGMENT_POSITION)
        _binding = null
        super.onDestroyView()
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        val resources = requireActivity().resources
        val enabled = sharedPreferences.getBoolean(resources.getString(R.string.settings_playlist_track_click_behaviour_key), true)

        if (enabled) {
            doPlay(id.toInt())
        }
    }

    private fun showContextMenu(position: Int) {
        val entities = adapterEntities ?: return
        val entity = entities[position]
        val playlistEntity = entity.getEntity()
        val menuItems = resources.getStringArray(R.array.player_playlist_context_menu)
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle(entity.getText())
            .setItems(menuItems) { _, which ->
                when (which) {
                    0 -> doPlay(position)
                    1 -> {
                        if (position != playerStatus.currentSong) {
                            MusicPlayerControl.sendControlCommands(listOf(DeleteFromPlaylistCommand(position), PrioritizeUriCommand(playlistEntity.getUriEntity())))
                        }
                    }
                    2 -> MusicPlayerControl.sendControlCommands(listOf(DeleteFromPlaylistCommand(position), UpdatePrioritiesCommand()))
                    3 -> {
                        val identifiers = ArrayList<Int>()
                        val album = playlistEntity.getAlbum()
                        for (adapterEntity in entities) {
                            val e = adapterEntity.getEntity()
                            if (album == e.getAlbum()) {
                                identifiers.add(e.getId()!!)
                            }
                        }
                        if (identifiers.isNotEmpty()) {
                            MusicPlayerControl.sendControlCommands(listOf(DeleteMultipleFromPlaylistCommand(identifiers.toIntArray()), UpdatePrioritiesCommand()))
                        }
                    }
                    4 -> goTo(playlistEntity)
                }
            }
            .show()
    }

    override fun statusUpdated(status: PlayerStatus) {
        val refresh = playerStatus.playlistVersion == status.playlistVersion &&
            (playerStatus.currentSong != status.currentSong || playerStatus.state != status.state)
        playerStatus = status
        if (refresh) {
            refreshEntities()
        }
    }

    override fun playlistUpdated(playlistEntities: Array<PlaylistEntity>) {
        if (playerStatus.playlistVersion > -1) {
            hideUpdatingIndicator()
            scrollToPlayingSong()
        }
        adapterEntities = createAdapterEntities(playlistEntities)
        refreshEntities()
    }

    override fun onChoiceMenuClick(view: View) {
        val items = resources.getStringArray(R.array.player_playlist_choice_menu)

        MaterialAlertDialogBuilder(requireActivity()).apply {
            setTitle(getString(R.string.player_playlist))
            setItems(items) { _, item ->
                when (item) {
                    0 -> scrollToPlayingSong()
                    1 -> shufflePlaylist()
                    2 -> {
                        val entities = adapterEntities
                        if (entities != null && entities.isNotEmpty()) {
                            MusicPlayerControl.sendControlCommand(UnprioritizeCommand(0, entities.size))
                        }
                    }
                    3 -> MusicPlayerControl.sendControlCommand(ClearPlaylistCommand())
                    4 -> adapterEntities?.let { UpdatePlayDataHelper.updatePlayData(requireActivity(), it) }
                }
            }
        }.create().show()
    }

    override fun forceRefresh() {
        findListView().invalidateViews()
    }

    private fun scrollToPlayingSong() {
        findListView().setSelectionFromTop(maxOf(0, playerStatus.currentSong - 1), 0)
    }

    private fun shufflePlaylist() {
        MusicPlayerControl.sendControlCommands(listOf(UnprioritizeCommand(0, (adapterEntities ?: return).size), ShuffleCommand()))
    }

    private fun createEntityAdapter() {
        val listView = findListView()
        val adapter = PlaylistArrayAdapter(requireActivity(), android.R.layout.simple_list_item_2, ArrayList())
        listView.adapter = adapter
    }

    private fun createAdapterEntities(entities: Array<PlaylistEntity>): Array<PlaylistAdapterEntity> {
        val showPriorities = showPriorities()
        return Array(entities.size) { PlaylistAdapterEntity(entities[it], showPriorities) }
    }

    private fun doPlay(id: Int) {
        if (id == playerStatus.currentSong) {
            if (playerStatus.state == PlayerState.PLAY) {
                MusicPlayerControl.sendControlCommand(PauseCommand(false))
                return
            }

            if (playerStatus.state == PlayerState.PAUSE) {
                MusicPlayerControl.sendControlCommand(PauseCommand(true))
                return
            }
        }
        val entity = adapterEntities?.get(id) ?: return
        val playlistEntity = entity.getEntity()
        MusicPlayerControl.sendControlCommands(listOf(PlayCommand(playlistEntity.getId()), UpdatePrioritiesCommand()))
    }

    private fun findListView(): DragListView {
        return binding.playerListViewPlaylist
    }

    private fun showUpdatingIndicator() {
        binding.playerProgressBarLoad.visibility = View.VISIBLE
    }

    private fun hideUpdatingIndicator() {
        binding.playerProgressBarLoad.visibility = View.GONE
    }

    private fun refreshEntities() {
        refreshEntities(playerStatus.currentSong, playerStatus.state)
    }

    private fun refreshEntities(currentSong: Int, playerState: PlayerState) {
        val adapter = findListView().adapter as PlaylistArrayAdapter
        adapter.setData(adapterEntities, currentSong, playerState)
    }

    private fun setSwipeEnabled(enabled: Boolean) {
        val viewPager = requireActivity().findViewById<ViewPager2>(R.id.player_view_pager_swipe)
        viewPager.isUserInputEnabled = enabled
    }

    private fun showPriorities(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        val resources = requireActivity().resources
        return sharedPreferences.getBoolean(resources.getString(R.string.settings_playlist_show_priority_in_playlist_key), false)
    }

    private fun goTo(playlistEntity: PlaylistEntity) {
        val items = resources.getStringArray(R.array.player_context_goto)

        MaterialAlertDialogBuilder(requireActivity()).apply {
            setTitle(R.string.player_goto_header)
            setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        val artist = playlistEntity.getArtist()
                        if (!artist.isNullOrEmpty()) {
                            val intent = Intent(requireActivity(), FilteredAlbumAndTitleActivity::class.java)
                            val args = Bundle()
                            args.putString(FilteredActivity.TITLE_ARGUMENT_KEY, artist)
                            args.putSerializable(FilteredActivity.ENTITY_ARGUMENT_KEY, LibraryEntity.createBuilder().setArtist(artist).setUriFilter(uriFilterHelper.getUriFilter()).build())
                            intent.putExtras(args)
                            startActivity(intent)
                        } else {
                            Boast.makeText(requireActivity(), R.string.player_not_possible).show()
                        }
                    }
                    1 -> {
                        val album = playlistEntity.getAlbum()
                        if (!album.isNullOrEmpty()) {
                            val intent = Intent(requireActivity(), FilteredTrackAndTitleActivity::class.java)
                            val args = Bundle()
                            args.putString(FilteredActivity.TITLE_ARGUMENT_KEY, album)
                            args.putSerializable(FilteredActivity.ENTITY_ARGUMENT_KEY, LibraryEntity.createBuilder().setAlbum(album).setUriFilter(uriFilterHelper.getUriFilter()).build())
                            intent.putExtras(args)
                            startActivity(intent)
                        } else {
                            Boast.makeText(requireActivity(), R.string.player_not_possible).show()
                        }
                    }
                }
            }
        }.create().show()
    }

    private inner class EntityDragListener : DragListView.DragListener {
        override fun drag(from: Int, to: Int) {
            setSwipeEnabled(false)
        }
    }

    private inner class EntityDropListener : DragListView.DropListener {
        override fun drop(from: Int, to: Int) {
            setSwipeEnabled(true)

            val entities = adapterEntities ?: return
            val movingEntity = entities[from]
            if (from > to) { // moving up in list
                System.arraycopy(entities, to, entities, to + 1, from - to)
                entities[to] = movingEntity
                MusicPlayerControl.sendControlCommands(listOf(MoveInPlaylistCommand(movingEntity.getEntity().getId()!!, to), UpdatePrioritiesCommand()))
                if (from > playerStatus.currentSong && to <= playerStatus.currentSong) {
                    refreshEntities(playerStatus.currentSong + 1, playerStatus.state)
                } else if (from == playerStatus.currentSong) {
                    refreshEntities(to, playerStatus.state)
                } else {
                    refreshEntities()
                }
            } else if (from < to) { // moving down in list
                System.arraycopy(entities, from + 1, entities, from, to - from)
                entities[to] = movingEntity
                MusicPlayerControl.sendControlCommands(listOf(MoveInPlaylistCommand(movingEntity.getEntity().getId()!!, to), UpdatePrioritiesCommand()))
                if (from < playerStatus.currentSong && to >= playerStatus.currentSong) {
                    refreshEntities(playerStatus.currentSong - 1, playerStatus.state)
                } else if (from == playerStatus.currentSong) {
                    refreshEntities(to, playerStatus.state)
                } else {
                    refreshEntities()
                }
            }
        }
    }

    private inner class EntityRemoveListener : DragListView.RemoveListener {
        override fun remove(which: Int) {
            setSwipeEnabled(true)

            val entities = adapterEntities ?: return
            val newEntities = arrayOfNulls<PlaylistAdapterEntity>(entities.size - 1)
            System.arraycopy(entities, 0, newEntities, 0, which)
            System.arraycopy(entities, which + 1, newEntities, which, newEntities.size - which)
            MusicPlayerControl.sendControlCommands(listOf(DeleteFromPlaylistCommand(which), UpdatePrioritiesCommand()))
            @Suppress("UNCHECKED_CAST")
            adapterEntities = newEntities as Array<PlaylistAdapterEntity>

            if (which < playerStatus.currentSong) {
                refreshEntities(playerStatus.currentSong - 1, playerStatus.state)
            } else if (which == playerStatus.currentSong) {
                refreshEntities(-1, playerStatus.state)
            } else {
                refreshEntities()
            }
        }
    }

    companion object {
        const val FRAGMENT_POSITION = 0
    }
}
