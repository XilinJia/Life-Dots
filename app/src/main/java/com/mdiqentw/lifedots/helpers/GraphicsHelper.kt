/*
 * LifeDots
 *
 * Copyright (C) 2017 Raphael Mack http://www.raphael-mack.de
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.mdiqentw.lifedots.helpers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.mdiqentw.lifedots.MVApplication
import com.mdiqentw.lifedots.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ln
import kotlin.math.sqrt

/*
 * LifeDots
 *
 * Copyright (C) 2020 Xilin Jia https://github.com/XilinJia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

object GraphicsHelper {
    const val TAG = "GraphicsHelper"

    /* list if recommended colors for new activites, populated from resources on startup */
    @JvmField
    val activityColorPalette = ArrayList<Int>(19)

    @JvmStatic
    fun imageStorageDirectory(): File {
        val root = File(MVApplication.getAppContext().getExternalFilesDir("/"), "")
        if (!root.exists()) {
            if (!root.mkdirs()) {
                Log.e(TAG, "failed to create directory")
                throw RuntimeException("failed to create directory $root")
            }
        }
        return root
    }

    @JvmStatic
    fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "IMG_" + timeStamp
        val storageDir = imageStorageDirectory()
        return File(storageDir, "$imageFileName.jpg")
    }

    @JvmStatic
    fun compressAndSaveImage(photoPath: String) {
        val b = BitmapFactory.decodeFile(photoPath)
        // original measurements
        val origWidth = b.width
        val origHeight = b.height
        val destWidth = 500 //or the width you need
        if (origWidth > destWidth) {
            // picture is wider than we want it, we calculate its target height
            val destHeight = origHeight / (origWidth / destWidth)
            // we create an scaled bitmap so it reduces the image, not just trim it
            val b2 = Bitmap.createScaledBitmap(b, destWidth, destHeight, false)
            val outStream = ByteArrayOutputStream()
            // compress to the format you want, JPEG, PNG...
            // 70 is the 0-100 quality percentage
            b2.compress(Bitmap.CompressFormat.JPEG, 60, outStream)
            // we save the file, at least until we have made use of it
            val f = File(photoPath)
            f.createNewFile()
            //write the bytes in file
            val fo = FileOutputStream(f)
            fo.write(outStream.toByteArray())
            fo.close()
        }
    }

    /* return the rotation of the image at uri from the exif data
     * do better not call this for a network uri, as this would probably mean to fetch it twice
     * */
    fun getFileExifRotation(uri: Uri?): Int {
        return try {
            val inputStream = MVApplication.getAppContext().contentResolver.openInputStream(uri!!)
            val exifInterface = ExifInterface(inputStream!!)
            when (exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "reading image failed (for exif rotation)", e)
            0
        } catch (e: IOException) {
            Log.e(TAG, "reading image failed (for exif rotation)", e)
            0
        }
    }

    /*
     * Calculate a font color with high contrast to the given background color
     */
    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun textColorOnBackground(color: Int): Int {
        return ContextCompat.getColor(MVApplication.getAppContext(), R.color.activityTextColorLight)
//        return ContextCompat.getColor(MVApplication.getAppContext(), color)
    }

    // function to calculate the color to be set for next newly created activity
    @JvmStatic
    fun prepareColorForNextActivity(): Int {
        var result = activityColorPalette[0]
        val acts = ActivityHelper.helper.activities
        var maxDistance = 0.0

        // check for each color in the palette the average distance to what is already configured
        for (c in activityColorPalette) {
            var dist = 0.0
            for (a in acts) {
                dist += ln(1 + colorDistance(c, a.color).toDouble())
            }
            if (dist > maxDistance) {
                // this one is better than the last
                result = c
                maxDistance = dist
            }
        }
        return result
    }

    /* some function estimating perceptional color difference
     * see https://en.wikipedia.org/wiki/Color_difference for details
     */
    private fun colorDistance(ci1: Int, ci2: Int): Int {
        val r1 = ci1 shr 16 and 0xFF
        val r2 = ci2 shr 16 and 0xFF
        val g1 = ci1 shr 8 and 0xFF
        val g2 = ci2 shr 8 and 0xFF
        val b1 = ci1 and 0xFF
        val b2 = ci2 and 0xFF
        val f = sqrt(2.0 * (r1 - r2) * (r1 - r2) + 4.0 * (g1 - g2) * (g1 - g2) + 3.0 * (b1 - b2) * (b1 - b2) + (r1 + r2) / 2.0 * ((r1 - r2) * (r1 - r2) - (b1 - b2) * (b1 - b2)) / 256.0
        )
        return f.toInt()
    }
}