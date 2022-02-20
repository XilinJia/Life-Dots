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
package com.mdiqentw.lifedots.ui.generic;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.mdiqentw.lifedots.R;
import com.mdiqentw.lifedots.databinding.ActivityBaseBinding;
import com.mdiqentw.lifedots.ui.history.HistoryActivity;
import com.mdiqentw.lifedots.ui.history.MapActivity;
import com.mdiqentw.lifedots.ui.main.MainActivity;
import com.mdiqentw.lifedots.ui.settings.SettingsActivity;
import com.mdiqentw.lifedots.ui.history.AnalyticsActivity;

import java.util.Objects;

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

/*
 * MainActivity to show most of the UI, based on switching the fragements
 *
 * */
public class BaseActivity extends AppCompatActivity {
    ActivityBaseBinding baseBinding;

    protected DrawerLayout mDrawerLayout;
    protected ActionBarDrawerToggle mDrawerToggle;
    protected NavigationView mNavigationView;

    protected Toolbar toolbar;

    protected void setupDrawer() {
        mDrawerLayout = baseBinding.drawerLayout;
        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.string.drawer_open,
                R.string.drawer_close
        );
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    protected void setupNavs() {
        mNavigationView = baseBinding.navigationView;
        mNavigationView.setNavigationItemSelectedListener(menuItem -> {
            int mid = menuItem.getItemId();
            if (mid == R.id.nav_main) {
                if(!menuItem.isChecked()) {
                    // start activity only if it is not currently checked
                    Intent intentmain = new Intent(BaseActivity.this, MainActivity.class);
                    intentmain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intentmain);
                }
            } else if (mid == R.id.nav_activity_manager) {
                Intent intentmanage = new Intent(BaseActivity.this, ManageActivity.class);
                startActivity(intentmanage);
            } else if (mid == R.id.nav_diary) {
                Intent intentdiary = new Intent(BaseActivity.this, HistoryActivity.class);
                startActivity(intentdiary);
            } else if (mid == R.id.nav_map) {
                Intent intentmap = new Intent(BaseActivity.this, MapActivity.class);
                startActivity(intentmap);
            } else if (mid == R.id.nav_statistics) {
                Intent intentstats = new Intent(BaseActivity.this, AnalyticsActivity.class);
                startActivity(intentstats);
            } else if (mid == R.id.nav_about) {
                Intent intentabout = new Intent(BaseActivity.this, AboutActivity.class);
                startActivity(intentabout);
            } else if (mid == R.id.nav_privacy) {
                Intent intentpriv = new Intent(BaseActivity.this, PrivacyPolicyActivity.class);
                startActivity(intentpriv);
            } else if (mid == R.id.nav_settings) {
                Intent intentsettings = new Intent(BaseActivity.this, SettingsActivity.class);
                startActivity(intentsettings);
            } else
                Toast.makeText(BaseActivity.this, menuItem.getTitle() + " is not yet implemented :-(", Toast.LENGTH_LONG).show();

            mDrawerLayout.closeDrawers();
            return true;
        });
    }

    protected void initNavigation() {
        toolbar = baseBinding.mainToolbar;
        setSupportActionBar(toolbar);

        setupDrawer();
        setupNavs();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }else if(item.getItemId() == android.R.id.home){
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    protected void setContent(View contentView){
        baseBinding = DataBindingUtil.setContentView(this, R.layout.activity_base);
        FrameLayout content = baseBinding.contentFragment;
        content.removeAllViews();
        content.addView(contentView);

        initNavigation();
    }
}
