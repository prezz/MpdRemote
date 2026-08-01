package net.prezz.mpr.ui.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import net.prezz.mpr.R
import net.prezz.mpr.databinding.ViewListItemPartitionBinding

class PartitionArrayAdapter(
    context: Context,
    textViewResourceId: Int,
    objects: List<PartitionAdapterEntity>,
) : ArrayAdapter<PartitionAdapterEntity>(context, textViewResourceId, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding: ViewListItemPartitionBinding
        val tag: ViewTag

        if (convertView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            binding = ViewListItemPartitionBinding.inflate(inflater, parent, false)
            tag = ViewTag(false, binding.partitionListItemText1.textColors, binding)
            binding.root.tag = tag
        } else {
            tag = convertView.tag as ViewTag
            binding = tag.binding
        }

        val entity = getItem(position)!!

        binding.partitionListItemText1.text = entity.getText()
        binding.partitionListItemText2.text = entity.getSubText()

        if (entity.getEntity().clientPartition != tag.selected) {
            if (entity.getEntity().clientPartition) {
                val color = getSelectionColor()
                binding.partitionListItemText1.setTextColor(color)
                binding.partitionListItemText2.setTextColor(color)
            } else {
                binding.partitionListItemText1.setTextColor(tag.standardColors)
                binding.partitionListItemText2.setTextColor(tag.standardColors)
            }
            tag.selected = entity.getEntity().clientPartition
        }

        return binding.root
    }

    fun setData(data: Array<PartitionAdapterEntity>?) {
        setNotifyOnChange(false)
        clear()
        if (data != null) {
            addAll(*data)
        }
        notifyDataSetChanged()
    }

    private fun getSelectionColor(): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.redFocusColor, typedValue, true)
        return typedValue.data
    }

    private class ViewTag(
        var selected: Boolean,
        val standardColors: ColorStateList,
        val binding: ViewListItemPartitionBinding,
    )
}
