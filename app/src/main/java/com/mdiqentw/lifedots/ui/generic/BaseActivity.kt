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
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

open class BaseActivity : AppCompatActivity() {
    lateinit var baseBinding: ActivityBaseBinding

    protected lateinit var mDrawerLayout: DrawerLayout
//    @JvmField
    protected lateinit var mDrawerToggle: ActionBarDrawerToggle
//    @JvmField
    protected lateinit var mNavigationView: NavigationView
    protected lateinit var toolbar: Toolbar
    private lateinit var backPressedCallback: OnBackPressedCallback
    protected open val shouldApplyDefaultInsets: Boolean = true

    protected fun setupDrawer() {
        mDrawerLayout = baseBinding.drawerLayout
        mDrawerToggle = ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close)
        mDrawerLayout.addDrawerListener(mDrawerToggle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)
    }

    protected fun setupNavs() {
        mNavigationView = baseBinding.navigationView
        mNavigationView.setNavigationItemSelectedListener { menuItem: MenuItem ->
            val mid = menuItem.itemId
            when (mid) {
                R.id.nav_main -> {
                    if (!menuItem.isChecked) {
                        val intentmain = Intent(this@BaseActivity, MainActivity::class.java)
                        intentmain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intentmain)
                    }
                }
                R.id.nav_activity_manager -> startActivity(Intent(this@BaseActivity, ManageActivity::class.java))
                R.id.nav_diary -> startActivity(Intent(this@BaseActivity, HistoryActivity::class.java))
                R.id.nav_map -> startActivity(Intent(this@BaseActivity, MapActivity::class.java))
                R.id.nav_statistics -> startActivity(Intent(this@BaseActivity, AnalyticsActivity::class.java))
                R.id.nav_about -> startActivity(Intent(this@BaseActivity, AboutActivity::class.java))
                R.id.nav_privacy -> startActivity(Intent(this@BaseActivity, PrivacyPolicyActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this@BaseActivity, SettingsActivity::class.java))
                else -> Toast.makeText(this@BaseActivity, menuItem.title.toString() + " is not yet implemented :-(", Toast.LENGTH_LONG).show()
            }
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

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        setupSystemBarInsets()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        setupSystemBarInsets()
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        setupSystemBarInsets()
    }

    protected open fun setupSystemBarInsets() {
        if (!shouldApplyDefaultInsets) return
        val contentView = findViewById<View>(android.R.id.content) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(contentView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBaseBackPressed()
            }
        }
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    open fun onBaseBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START)
        } else {
            backPressedCallback.isEnabled = false
            onBackPressedDispatcher.onBackPressed()
        }
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

    protected fun setContent(contentView: View?) {
        baseBinding = DataBindingUtil.setContentView(this, R.layout.activity_base)
        val content = baseBinding.contentFragment
        content.removeAllViews()
        content.addView(contentView)
        initNavigation()
    }
}