/*
 * LifeDots
 *
 * Copyright (C) 2017 Raphael Mack http://www.raphael-mack.de
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
package com.mdiqentw.lifedots

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.graphics.Color
import com.mdiqentw.lifedots.helpers.GraphicsHelper

//import org.acra.*;
//import org.acra.annotation.*;
//import org.acra.data.StringFormat;
class MVApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

        /* now do some init stuff */
        val colors = appContext!!.resources.getStringArray(R.array.activityColorPalette)
        for (color in colors) {
            GraphicsHelper.activityColorPalette.add(Color.parseColor(color))
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

//        ACRA.init(this);
    }

    companion object {
        @JvmField
        @SuppressLint("StaticFieldLeak")
        var appContext: Context? = null
//            private set
    }
}