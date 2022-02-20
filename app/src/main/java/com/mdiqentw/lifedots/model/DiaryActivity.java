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

package com.mdiqentw.lifedots.model;

import androidx.annotation.NonNull;

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

public class DiaryActivity{
    private String mName;
    private int mId;
    private int mColor;

    public DiaryActivity(int id, String name, int color){
        mId = id;
        mName = name;
        mColor = color;
    }

    public String getName(){
        return mName;
    }
    public void setName(String name){ mName = name;}

    public int getColor(){
        return mColor;
    }
    public void setColor(int color){ mColor = color;}

    public int getId() { return mId;}
    public void setId(int id) { mId = id;}

    public boolean equals(Object other){
        return other instanceof DiaryActivity && ((DiaryActivity) other).mName.equals(mName);
    }

    @NonNull
    @Override
    public String toString(){
        return mName + " (" + mId + ")";
    }

    @Override
    public int hashCode(){
        return mId;
    }
}
