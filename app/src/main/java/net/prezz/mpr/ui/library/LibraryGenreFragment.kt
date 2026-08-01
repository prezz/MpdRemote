package net.prezz.mpr.ui.library

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.MusicPlayerControl
import net.prezz.mpr.model.ResponseReceiver
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.ui.adapter.AdapterEntity
import net.prezz.mpr.ui.adapter.GenreAdapterEntity
import net.prezz.mpr.ui.adapter.LibraryAdapterEntity
import net.prezz.mpr.ui.adapter.LibraryArrayAdapter
import net.prezz.mpr.ui.adapter.SortedAdapterIndexStrategy
import net.prezz.mpr.ui.library.filtered.FilteredActivity
import net.prezz.mpr.ui.library.filtered.FilteredAlbumsActivity

class LibraryGenreFragment : LibraryFragment() {

    override fun onItemClick(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val adapterEntity = getAdapterEntity(position) as LibraryAdapterEntity
        val entity = adapterEntity.getEntity()

        val intent = Intent(activity, FilteredAlbumsActivity::class.java)
        val args = Bundle()
        args.putString(FilteredActivity.TITLE_ARGUMENT_KEY, adapterEntity.getText())
        args.putSerializable(FilteredActivity.ENTITY_ARGUMENT_KEY, entity)
        intent.putExtras(args)
        startActivity(intent)
    }

    override fun getFragmentPosition(): Int {
        return FRAGMENT_POSITION
    }

    override fun getEntities(responseReceiver: ResponseReceiver<Array<LibraryEntity>>): TaskHandle {
        val libraryActivity = requireActivity() as LibraryActivity
        return MusicPlayerControl.getGenresFromLibrary(libraryActivity.getLibraryEntityFilter(), responseReceiver)
    }

    override fun createAdapterEntities(entities: Array<LibraryEntity>): Array<AdapterEntity> {
        return Array(entities.size) { GenreAdapterEntity(entities[it]) }
    }

    override fun createAdapter(adapterEntities: Array<AdapterEntity>): LibraryArrayAdapter {
        return LibraryArrayAdapter(requireActivity(), android.R.layout.simple_list_item_1, adapterEntities, SortedAdapterIndexStrategy, false)
    }

    companion object {
        private const val FRAGMENT_POSITION = 1
    }
}
