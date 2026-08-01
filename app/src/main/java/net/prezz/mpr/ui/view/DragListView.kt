package net.prezz.mpr.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.ListView
import net.prezz.mpr.R

class DragListView(context: Context, attrs: AttributeSet?) : ListView(context, attrs) {

    private var dragView: ImageView? = null
    private var windowManager: WindowManager? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private var dragPos = 0 // which item is being dragged
    private var srcDragPos = 0 // where was the dragged item originally
    private var dragPointX = 0
    private var dragPointY = 0 // at what offset inside the item did the user grab it
    private var xOffset = 0
    private var yOffset = 0 // the difference between screen coordinates and coordinates in this view
    private var dragListener: DragListener? = null
    private var dropListener: DropListener? = null
    private var removeListener: RemoveListener? = null
    private var upperBound = 0
    private var lowerBound = 0
    private var height = 0
    private val tempRect = Rect()
    private var dragBitmap: Bitmap? = null
    private val touchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop
    private var itemHeightNormal = 0
    private var itemHeightExpanded = 0
    private var itemHeightHalf = 0
    private var draggingEnabled = true
    private var deleting = false

    private fun isDragItem(item: View): Boolean {
        return draggingEnabled && item.findViewById<View?>(R.id.playlist_list_item_drag_image) != null
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (draggingEnabled && dragListener != null || dropListener != null) {
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    deleting = false
                    val x = ev.x.toInt()
                    val y = ev.y.toInt()
                    val itemnum = pointToPosition(x, y)
                    if (itemnum == AdapterView.INVALID_POSITION) {
                        return super.onInterceptTouchEvent(ev)
                    }
                    val item = getChildAt(itemnum - firstVisiblePosition) as ViewGroup
                    itemHeightNormal = item.height
                    itemHeightHalf = itemHeightNormal / 2
                    itemHeightExpanded = itemHeightNormal * 2
                    dragPointX = x - item.left
                    dragPointY = y - item.top
                    xOffset = ev.rawX.toInt() - x
                    yOffset = ev.rawY.toInt() - y
                    val dragger = item.findViewById<View>(R.id.playlist_list_item_drag_image)
                    if (!isDragItem(item)) {
                        return super.onInterceptTouchEvent(ev)
                    }

                    val r = tempRect
                    dragger.getDrawingRect(r)
                    // Drag icon is supposed to be in the left, so if we touch left of the drag icons right side we are dragging.
                    if (x < r.right) {
                        val bitmap = Bitmap.createBitmap(item.width, item.height, Bitmap.Config.ARGB_8888)

                        val canvas = Canvas(bitmap)
                        item.draw(canvas)

                        startDragging(bitmap, x, y)
                        dragPos = itemnum
                        srcDragPos = dragPos
                        height = getHeight()
                        upperBound = minOf(y - touchSlop, height / 3)
                        lowerBound = maxOf(y + touchSlop, height * 2 / 3)
                        return false
                    }
                    stopDragging()
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    /*
     * pointToPosition() doesn't consider invisible views, but we need to, so implement a slightly different version.
     */
    private fun myPointToPosition(x: Int, y: Int): Int {

        if (y < 0) {
            // when dragging off the top of the screen, calculate position
            // by going back from a visible item
            val pos = myPointToPosition(x, y + itemHeightNormal)
            if (pos > 0) {
                return pos - 1
            }
        }

        val frame = tempRect
        val count = childCount
        for (i in count - 1 downTo 0) {
            val child = getChildAt(i)
            child.getHitRect(frame)
            if (frame.contains(x, y)) {
                return firstVisiblePosition + i
            }
        }
        return INVALID_POSITION
    }

    private fun getItemForPosition(y: Int): Int {
        val adjustedy = y - dragPointY - itemHeightHalf
        var pos = myPointToPosition(0, adjustedy)
        if (pos >= 0) {
            if (pos <= srcDragPos) {
                pos += 1
            }
        } else if (adjustedy < 0) {
            // this shouldn't happen anymore now that myPointToPosition deals
            // with this situation
            pos = 0
        }
        return pos
    }

    private fun adjustScrollBounds(y: Int) {
        if (y >= height / 3) {
            upperBound = height / 3
        }
        if (y <= height * 2 / 3) {
            lowerBound = height * 2 / 3
        }
    }

    /*
     * Restore size and visibility for all listitems
     */
    private fun unExpandViews(deletion: Boolean) {
        // find information necessary to adjust list position if the collapsed
        // source position is scrolled out beyond the top. This is to prevent
        // annoying jump of items when dropping.
        val firstVisibleIndex = firstVisiblePosition
        val firstVisibleChild = getChildAt(0)
        val firstVisibleTop = firstVisibleChild?.top ?: 0
        val counterScroll = srcDragPos < firstVisibleIndex

        var i = 0
        while (true) {
            var v = getChildAt(i)
            if (v == null) {
                if (deletion) {
                    // HACK force update of mItemCount
                    val position = firstVisiblePosition
                    val y = getChildAt(0).top
                    val currentAdapter = adapter
                    adapter = currentAdapter
                    setSelectionFromTop(position, y)
                    // end hack
                }
                try {
                    layoutChildren() // force children to be recreated where needed
                    v = getChildAt(i)
                } catch (ex: IllegalStateException) {
                    // layoutChildren throws this sometimes, presumably because we're
                    // in the process of being torn down but are still getting touch
                    // events
                }
                if (v == null) {
                    break
                }
            }
            if (isDragItem(v)) {
                val params = v.layoutParams
                params.height = itemHeightNormal
                v.layoutParams = params
                v.visibility = View.VISIBLE
            }
            i++
        }

        if (counterScroll) {
            setSelectionFromTop(firstVisibleIndex - 1, firstVisibleTop)
        }
    }

    /*
     * Adjust visibility and size to make it appear as though an item is being dragged around and other items are making room for it: If
     * dropping the item would result in it still being in the same place, then make the dragged listitem's size normal, but make the item
     * invisible. Otherwise, if the dragged listitem is still on screen, make it as small as possible and expand the item below the insert
     * point. If the dragged item is not on screen, only expand the item below the current insertpoint.
     */
    private fun doExpansion() {
        var childnum = dragPos - firstVisiblePosition
        if (dragPos > srcDragPos) {
            childnum++
        }
        val numheaders = headerViewsCount

        val first = getChildAt(srcDragPos - firstVisiblePosition)
        var i = 0
        while (true) {
            val vv = getChildAt(i) ?: break

            var height = itemHeightNormal
            var visibility = View.VISIBLE
            if (dragPos < numheaders && i == numheaders) {
                // dragging on top of the header item, so adjust the item below
                // instead
                if (vv == first) {
                    visibility = View.INVISIBLE
                } else {
                    height = itemHeightExpanded
                }
            } else if (vv == first) {
                // processing the item that is being dragged
                if (dragPos == srcDragPos || getPositionForView(vv) == count - 1) {
                    // hovering over the original location
                    visibility = View.INVISIBLE
                } else {
                    // not hovering over it
                    // Ideally the item would be completely gone, but neither
                    // setting its size to 0 nor settings visibility to GONE
                    // has the desired effect.
                    height = 1
                }
            } else if (i == childnum) {
                if (dragPos >= numheaders && dragPos < count - 1) {
                    height = itemHeightExpanded
                }
            }
            val params = vv.layoutParams
            params.height = height
            vv.layoutParams = params
            vv.visibility = visibility
            i++
        }
    }

    private fun collapseDeletionItem() {
        val childnum = srcDragPos - firstVisiblePosition
        val v = getChildAt(childnum) ?: return

        if (isDragItem(v)) {
            val params = v.layoutParams
            params.height = 1
            v.layoutParams = params
            v.visibility = View.INVISIBLE
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if ((dragListener != null || dropListener != null) && dragView != null) {
            val action = ev.action
            when (action) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val r = tempRect
                    dragView!!.getDrawingRect(r)
                    stopDragging()
                    if (deleting) {
                        removeListener?.remove(srcDragPos)
                        unExpandViews(true)
                    } else {
                        if (dropListener != null && dragPos >= 0 && dragPos < count) {
                            dropListener!!.drop(srcDragPos, dragPos)
                        }
                        unExpandViews(false)
                    }
                }

                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    if (!deleting && ev.x > tempRect.right * 3 / 4) {
                        if (dragPos != srcDragPos) {
                            unExpandViews(false)
                        }
                        collapseDeletionItem()
                        deleting = true
                    } else if (deleting && ev.x <= tempRect.right * 3 / 4) {
                        doExpansion()
                        deleting = false
                    }

                    val x = ev.x.toInt()
                    val y = ev.y.toInt()
                    dragView(x, y)
                    val itemnum = getItemForPosition(y)
                    if (itemnum >= 0) {
                        if (action == MotionEvent.ACTION_DOWN || itemnum != dragPos) {
                            dragListener?.drag(dragPos, itemnum)
                            dragPos = itemnum
                            if (!deleting) {
                                doExpansion()
                            }
                        }
                        var speed = 0
                        adjustScrollBounds(y)
                        if (y > lowerBound) {

                            // if dragging an item down past the point where list begins to scroll so its source position
                            // exists the screen, expand the item on scroll the list one position forward.
                            // This is to fix a bug where some items would not get unexpanded again
                            val v = getChildAt(0)
                            val params = v.layoutParams
                            if (params.height == 1) {
                                params.height = itemHeightNormal
                                v.layoutParams = params
                                v.visibility = View.VISIBLE
                                val pos = firstVisiblePosition
                                setSelectionFromTop(pos + 1, 0)
                            }

                            // scroll the list up a bit
                            speed = if (lastVisiblePosition < count - 1) {
                                if (y > (height + lowerBound) / 2) 16 else 4
                            } else {
                                1
                            }
                        } else if (y < upperBound) {
                            // scroll the list down a bit
                            speed = if (y < upperBound / 2) -16 else -4
                            if (firstVisiblePosition == 0 && getChildAt(0).top >= paddingTop) {
                                // if we're already at the top, don't try to scroll,
                                // because
                                // it causes the framework to do some extra drawing
                                // that messes
                                // up our animation
                                speed = 0
                            }
                        }
                        if (speed != 0) {
                            smoothScrollBy(speed, 30)
                        }
                    }
                }
            }
            return true
        }
        return super.onTouchEvent(ev)
    }

    private fun startDragging(bm: Bitmap, x: Int, y: Int) {
        stopDragging()

        val params = WindowManager.LayoutParams()
        params.gravity = Gravity.TOP or Gravity.LEFT
        params.x = x - dragPointX + xOffset
        params.y = y - dragPointY + yOffset

        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        params.format = PixelFormat.TRANSLUCENT
        params.windowAnimations = 0
        windowParams = params

        val context = context
        val v = ImageView(context)
        val backGroundColor = getDragBackgroundColor()
        v.setBackgroundColor(backGroundColor)
        v.setPadding(0, 0, 0, 0)
        v.setImageBitmap(bm)
        dragBitmap = bm

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.addView(v, params)
        windowManager = wm
        dragView = v
    }

    private fun dragView(x: Int, y: Int) {
        val view = dragView!!
        val params = windowParams!!
        var alpha = 1.0f
        val width = view.width
        if (x > width / 2) {
            alpha = (width - x).toFloat() / (width / 2)
        }
        params.alpha = alpha

        params.x = x - dragPointX + xOffset
        params.y = y - dragPointY + yOffset
        windowManager!!.updateViewLayout(view, params)
    }

    private fun stopDragging() {
        dragView?.let { view ->
            view.visibility = GONE
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.removeView(view)
            view.setImageDrawable(null)
            dragView = null
        }
        dragBitmap?.let {
            it.recycle()
            dragBitmap = null
        }
    }

    fun setDraggingEnabled(enable: Boolean) {
        draggingEnabled = enable
    }

    fun setDragListener(l: DragListener?) {
        dragListener = l
    }

    fun setDropListener(l: DropListener?) {
        dropListener = l
    }

    fun setRemoveListener(l: RemoveListener?) {
        removeListener = l
    }

    fun interface DragListener {
        fun drag(from: Int, to: Int)
    }

    fun interface DropListener {
        fun drop(from: Int, to: Int)
    }

    fun interface RemoveListener {
        fun remove(which: Int)
    }

    private fun getDragBackgroundColor(): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.playlistDragColor, typedValue, true)
        return typedValue.data
    }
}
