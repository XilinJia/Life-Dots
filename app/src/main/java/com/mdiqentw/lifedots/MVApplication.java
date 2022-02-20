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

package com.mdiqentw.lifedots;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.graphics.Color;

//import org.acra.*;
//import org.acra.annotation.*;
//import org.acra.data.StringFormat;

import com.mdiqentw.lifedots.helpers.GraphicsHelper;

/*
 * LifeDots
 *
 * Copyright (C) 2020 Xilin Jia https://github.com/XilinJia
  ~ Copyright (C) 2017-2018 Raphael Mack http://www.raphael-mack.de
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

public class MVApplication extends Application {

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    public void onCreate() {
        super.onCreate();
        MVApplication.context = getApplicationContext();

        /* now do some init stuff */
        String[] colors = context.getResources().getStringArray(R.array.activityColorPalette);

        for (String color : colors) {
            GraphicsHelper.activityColorPalette.add(Color.parseColor(color));
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

//        ACRA.init(this);
    }

    public static Context getAppContext() {
        return MVApplication.context;
    }
}
