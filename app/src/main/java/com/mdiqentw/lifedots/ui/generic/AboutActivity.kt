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

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import androidx.databinding.DataBindingUtil
import com.mdiqentw.lifedots.BuildConfig
import com.mdiqentw.lifedots.R
import com.mdiqentw.lifedots.databinding.ActivityAboutBinding

class AboutActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityAboutBinding>(this, R.layout.activity_about)
        setContent(binding.root)
        val aboutText = binding.aboutTextView
        val appName = resources.getString(R.string.app_name)
        val contributors = resources.getString(R.string.contributors)
        val libraries = resources.getString(R.string.libraries)
        val versionName = BuildConfig.VERSION_NAME
        var mergedAboutText = "<h1>$appName</h1>"
        mergedAboutText += resources.getString(R.string.about_text_version, versionName)
        mergedAboutText += "<p>" + resources.getString(R.string.about_text_intro) + "</p>"
        mergedAboutText += "<p>" + resources.getString(R.string.about_fork) + "</p>"
        mergedAboutText += "<h1>" + resources.getString(R.string.about_text_licence_h) + "</h1>"
        mergedAboutText += "<p>" + resources.getString(R.string.about_text_licence, appName) + "</p>"
        mergedAboutText += "<p>" + resources.getString(R.string.about_text_license_2) + "</p>"
        mergedAboutText += "<h1>" + resources.getString(R.string.about_text_privacy_h) + "</h1>"
        mergedAboutText += "<p>" + resources.getString(R.string.about_text_privacy, appName) + "</p>"
        mergedAboutText += "<h1>" + resources.getString(R.string.about_text_contact_h) + "</h1>"
        mergedAboutText += "<p>" + resources.getString(R.string.about_text_contact) + "</p>"
        mergedAboutText += "<p>" + resources.getString(R.string.about_text_contact_2) + "</p>"
        mergedAboutText += "<h1>" + resources.getString(R.string.about_text_support_h) + "</h1>"
        mergedAboutText += "<p>" + resources.getString(R.string.about_text_support, appName) + "</p>"
        mergedAboutText += "<h1>" + resources.getString(R.string.about_text_cont_h) + "</h1>"
        mergedAboutText += "<p>$contributors</p>"
        mergedAboutText += "<p>" + resources.getString(R.string.about_text_cont_2) + "</p>"
        mergedAboutText += "<h1>" + resources.getString(R.string.about_text_lib_h) + "</h1>"
        mergedAboutText += "<p>" + resources.getString(R.string.about_text_lib) + "</p>"
        mergedAboutText += "<p>$libraries</p>"
        if (Build.VERSION.SDK_INT >= 24) {
            aboutText.text = Html.fromHtml(mergedAboutText, Html.FROM_HTML_MODE_LEGACY)
        } else {
            aboutText.text = Html.fromHtml(mergedAboutText)
        }
        aboutText.movementMethod = LinkMovementMethod.getInstance()
        mDrawerToggle.isDrawerIndicatorEnabled = false
    }

    public override fun onResume() {
        mNavigationView.menu.findItem(R.id.nav_about).isChecked = true
        super.onResume()
    }
}