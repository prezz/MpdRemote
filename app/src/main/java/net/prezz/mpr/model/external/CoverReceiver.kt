package net.prezz.mpr.model.external

import android.graphics.Bitmap

fun interface CoverReceiver {

    fun receiveCover(bitmap: Bitmap?)
}
