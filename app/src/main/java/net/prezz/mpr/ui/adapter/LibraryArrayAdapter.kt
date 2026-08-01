package net.prezz.mpr.ui.adapter

import java.util.ArrayList

import net.prezz.mpr.model.LibraryEntity
import net.prezz.mpr.model.LibraryEntity.Tag
import net.prezz.mpr.model.TaskHandle
import net.prezz.mpr.model.UriEntity.FileType
import net.prezz.mpr.model.external.CoverReceiver
import net.prezz.mpr.model.external.ExternalInformationService
import net.prezz.mpr.R
import net.prezz.mpr.databinding.ViewListItemLibraryBinding
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.SectionIndexer
import android.widget.TextView

class LibraryArrayAdapter : ArrayAdapter<AdapterEntity>, SectionIndexer {

    private val sectionsList = ArrayList<String>()
    private val positionForSection = ArrayList<Int>()
    private val sectionForPosition = ArrayList<Int>()
    private var showCover = false
    private var coverSize: Int? = null

    constructor(context: Context, textViewResourceId: Int) : super(context, textViewResourceId) {
        throw UnsupportedOperationException()
    }

    constructor(context: Context, resource: Int, textViewResourceId: Int) : super(context, resource, textViewResourceId) {
        throw UnsupportedOperationException()
    }

    constructor(context: Context, textViewResourceId: Int, objects: Array<AdapterEntity>) : super(context, textViewResourceId, objects) {
        throw UnsupportedOperationException()
    }

    constructor(context: Context, resource: Int, textViewResourceId: Int, objects: Array<AdapterEntity>) : super(context, resource, textViewResourceId, objects) {
        throw UnsupportedOperationException()
    }

    constructor(context: Context, textViewResourceId: Int, objects: List<AdapterEntity>) : super(context, textViewResourceId, objects) {
        throw UnsupportedOperationException()
    }

    constructor(context: Context, resource: Int, textViewResourceId: Int, objects: List<AdapterEntity>) : super(context, resource, textViewResourceId, objects) {
        throw UnsupportedOperationException()
    }

    constructor(context: Context, textViewResourceId: Int, objects: Array<AdapterEntity>, sectionIndexStrategy: AdapterIndexStrategy, showCover: Boolean) : super(context, textViewResourceId, objects) {

        sectionIndexStrategy.createSectionIndexes(objects, sectionsList, positionForSection, sectionForPosition)

        this.showCover = showCover
        if (showCover) {
            coverSize = context.resources.getDimensionPixelSize(R.dimen.library_list_view_cover_size)
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val entity = getItem(position)

        if (view == null && entity is LibraryAdapterEntity) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.view_list_item_library, parent, false)
            view.tag = Wrapper(view, false, showCover, coverSize)
        } else if (view == null && entity is UriAdapterEntity) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.view_list_item_library, parent, false)
            view.tag = Wrapper(view, false, false, null)
        } else if (view == null && entity is SectionAdapterEntity) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(android.R.layout.preference_category, parent, false)
            view.tag = Wrapper(view, true, false, null)
        }

        val wrapper = view!!.tag as Wrapper
        wrapper.setAdapterEntity(entity)

        return view
    }

    override fun getViewTypeCount(): Int {
        return 3
    }

    override fun getItemViewType(position: Int): Int {
        val entity = getItem(position)
        return if (entity is LibraryAdapterEntity) {
            0
        } else if (entity is UriAdapterEntity) {
            1
        } else {
            2
        }
    }

    override fun getSections(): Array<Any> {
        @Suppress("UNCHECKED_CAST")
        return sectionsList.toArray() as Array<Any>
    }

    override fun getPositionForSection(section: Int): Int {
        var section = section
        if (section >= positionForSection.size) {
            section = positionForSection.size - 1
        }
        return positionForSection.get(section)
    }

    override fun getSectionForPosition(position: Int): Int {
        var position = position
        if (position >= sectionForPosition.size) {
            position = sectionForPosition.size - 1
        }
        return sectionForPosition.get(position)
    }

    private class Wrapper(view: View, isSection: Boolean, showCover: Boolean, coverSize: Int?) {

        private val view: View = view
        private var binding: ViewListItemLibraryBinding? = null
        private var textView1: TextView? = null
        private var textViewData: TextView? = null
        private var textView2: TextView? = null
        private var textViewTime: TextView? = null
        private var coverView: ImageView? = null
        private var showCover = false
        private var coverSize: Int? = null
        private var loaderTask: TaskHandle = TaskHandle.NULL_HANDLE
        private var currentAdapterEntity: AdapterEntity? = null
        private var standardColors1: ColorStateList? = null
        private var standardColors2: ColorStateList? = null

        init {
            if (!isSection) {
                this.showCover = showCover
                this.coverSize = coverSize
                val binding = ViewListItemLibraryBinding.bind(view)
                this.binding = binding
                this.textView1 = binding.libraryListItemText1
                this.textViewData = binding.libraryListItemData
                this.textView2 = binding.libraryListItemText2
                this.textViewTime = binding.libraryListItemTime
                this.coverView = binding.libraryListItemCoverImage
                this.standardColors1 = textView1!!.textColors
                this.standardColors2 = textView2!!.textColors
            }
        }

        fun setAdapterEntity(entity: AdapterEntity?) {
            if (currentAdapterEntity === entity) {
                return
            }
            currentAdapterEntity = entity
            if (entity is LibraryAdapterEntity) {
                textView1!!.text = entity.getSubText()
                textViewData!!.text = entity.getData()
                textView2!!.text = entity.getText()
                textViewTime!!.text = entity.getTime()
                if (showCover) {
                    loadCover(entity)
                }
            } else if (entity is UriAdapterEntity) {
                textView1!!.text = entity.getSubText()
                textView2!!.text = entity.getText()
                if (entity.getEntity().fileType == FileType.PLAYLIST) {
                    textView1!!.setTextColor(standardColors1!!.withAlpha(128))
                    textView2!!.setTextColor(standardColors2!!.withAlpha(128))
                } else {
                    textView1!!.setTextColor(standardColors1)
                    textView2!!.setTextColor(standardColors2)
                }
            } else if (entity is SectionAdapterEntity) {
                (view.findViewById<TextView>(android.R.id.title)).text = entity.getText()
            }
        }

        private fun loadCover(adapterEntity: LibraryAdapterEntity) {
            loaderTask.cancelTask()

            coverView!!.setImageBitmap(null)
            val entity = adapterEntity.getEntity()
            if (entity.getTag() == Tag.ALBUM) {
                coverView!!.visibility = View.VISIBLE
                val compilation = entity.getMetaCompilation()
                val artist = if (compilation == java.lang.Boolean.TRUE) null else entity.getLookupArtist()
                val album = entity.getLookupAlbum()
                loaderTask = ExternalInformationService.getCover(artist, album, coverSize, CoverReceiver { bitmap ->
                    coverView!!.setImageBitmap(bitmap)
                })
            } else {
                coverView!!.visibility = View.GONE
            }
        }
    }
}
