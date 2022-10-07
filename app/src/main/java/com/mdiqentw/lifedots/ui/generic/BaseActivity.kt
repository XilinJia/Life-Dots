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
package com.mdiqentw.lifedots.ui.generic

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.mdiqentw.lifedots.R
import com.mdiqentw.lifedots.databinding.ActivityBaseBinding
import com.mdiqentw.lifedots.ui.history.AnalyticsActivity
import com.mdiqentw.lifedots.ui.history.HistoryActivity
import com.mdiqentw.lifedots.ui.history.MapActivity
import com.mdiqentw.lifedots.ui.main.MainActivity
import com.mdiqentw.lifedots.ui.settings.SettingsActivity
import java.util.*

/*
 * MainActivity to show most of the UI, based on switching the fragements
 *
 * */
open class BaseActivity : AppCompatActivity() {
    lateinit var baseBinding: ActivityBaseBinding

    protected lateinit var mDrawerLayout: DrawerLayout
//    @JvmField
    protected lateinit var mDrawerToggle: ActionBarDrawerToggle
//    @JvmField
    protected lateinit var mNavigationView: NavigationView
    protected lateinit var toolbar: Toolbar

    protected fun setupDrawer() {
        mDrawerLayout = baseBinding.drawerLayout
        mDrawerToggle = ActionBarDrawerToggle(
            this,
            mDrawerLayout,
            R.string.drawer_open,
            R.string.drawer_close
        )
        mDrawerLayout.addDrawerListener(mDrawerToggle)
        Objects.requireNonNull(supportActionBar!!).setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)
    }

    protected fun setupNavs() {
        mNavigationView = baseBinding.navigationView
        mNavigationView.setNavigationItemSelectedListener { menuItem: MenuItem ->
            val mid = menuItem.itemId
            if (mid == R.id.nav_main) {
                if (!menuItem.isChecked) {
                    // start activity only if it is not currently checked
                    val intentmain = Intent(this@BaseActivity, MainActivity::class.java)
                    intentmain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intentmain)
                }
            } else if (mid == R.id.nav_activity_manager) {
                val intentmanage = Intent(this@BaseActivity, ManageActivity::class.java)
                startActivity(intentmanage)
            } else if (mid == R.id.nav_diary) {
                val intentdiary = Intent(this@BaseActivity, HistoryActivity::class.java)
                startActivity(intentdiary)
            } else if (mid == R.id.nav_map) {
                val intentmap = Intent(this@BaseActivity, MapActivity::class.java)
                startActivity(intentmap)
            } else if (mid == R.id.nav_statistics) {
                val intentstats = Intent(this@BaseActivity, AnalyticsActivity::class.java)
                startActivity(intentstats)
            } else if (mid == R.id.nav_about) {
                val intentabout = Intent(this@BaseActivity, AboutActivity::class.java)
                startActivity(intentabout)
            } else if (mid == R.id.nav_privacy) {
                val intentpriv = Intent(this@BaseActivity, PrivacyPolicyActivity::class.java)
                startActivity(intentpriv)
            } else if (mid == R.id.nav_settings) {
                val intentsettings = Intent(this@BaseActivity, SettingsActivity::class.java)
                startActivity(intentsettings)
            } else Toast.makeText(
                this@BaseActivity,
                menuItem.title.toString() + " is not yet implemented :-(",
                Toast.LENGTH_LONG
            ).show()
            mDrawerLayout.closeDrawers()
            true
        }
    }

    protected fun initNavigation() {
        toolbar = baseBinding.mainToolbar
        setSupportActionBar(toolbar)
        setupDrawer()
        setupNavs()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mDrawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true
        } else if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    protected fun setContent(contentView: View?) {
        baseBinding = DataBindingUtil.setContentView(this, R.layout.activity_base)
        val content = baseBinding.contentFragment
        content.removeAllViews()
        content.addView(contentView)
        initNavigation()
    }
}