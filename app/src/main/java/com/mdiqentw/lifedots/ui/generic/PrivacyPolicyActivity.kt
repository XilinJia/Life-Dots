/*
 * LifeDots
 *
 * Copyright (C) 2018 Raphael Mack http://www.raphael-mack.de
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
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import com.mdiqentw.lifedots.R
import com.mdiqentw.lifedots.databinding.ActivityPrivacyPolicyBinding

class PrivacyPolicyActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding =
            DataBindingUtil.setContentView<ActivityPrivacyPolicyBinding>(this, R.layout.activity_privacy_policy)
        setContent(binding.root)

        // TODO: need to et logo to the image
        val policyText = findViewById<TextView>(R.id.policyTextView)
        var mergedPolicyText = "<h1>" + resources.getString(R.string.activity_title_privacy_policy) + "</h1>"
        mergedPolicyText += "<p>" + resources.getString(R.string.privacy_text) + "</p>"
        mergedPolicyText += "<h2>" + resources.getString(R.string.privacy_intro_title) + "</h2>"
        mergedPolicyText += "<p>" + resources.getString(R.string.privacy_intro_text1) + "</p>"
        mergedPolicyText += "<p>" + resources.getString(R.string.privacy_intro_text2) + "</p>"
        mergedPolicyText += "<p>" + resources.getString(R.string.privacy_intro_text3) + "</p>"
        mergedPolicyText += "<p>" + resources.getString(R.string.privacy_intro_text4) + "</p>"
        mergedPolicyText += "<p>" + resources.getString(R.string.privacy_intro_text5) + "</p>"
        mergedPolicyText += "<h2>" + resources.getString(R.string.privacy_what_title) + "</h2>"
        mergedPolicyText += "<h3>" + resources.getString(R.string.privacy_what_subTitle1) + "</h3>"
        mergedPolicyText += "<p>" + resources.getString(R.string.privacy_what_subText1a) + "</p>"
        mergedPolicyText += "<h3>" + resources.getString(R.string.privacy_what_subTitle2) + "</h3>"
        mergedPolicyText += "<p>" + resources.getString(R.string.privacy_what_subText2a) + "</p>"
        //        mergedPolicyText += "<h3>" + getResources().getString(R.string.privacy_what_subTitle3) + "</h3>";
//        mergedPolicyText += "<p>" + getResources().getString(R.string.privacy_what_subText3a) + "</p>";
        mergedPolicyText += "<h3>" + resources.getString(R.string.privacy_what_subTitle4) + "</h3>"
        mergedPolicyText += "<p>" + resources.getString(R.string.privacy_what_subText4a) + "</p>"
        mergedPolicyText += "<h3>" + resources.getString(R.string.privacy_what_subTitle5) + "</h3>"
        mergedPolicyText += "<p>" + resources.getString(R.string.privacy_what_subText5a) + "</p>"
        //        mergedPolicyText += "<h3>" + getResources().getString(R.string.privacy_what_subTitle6) + "</h3>";
//        mergedPolicyText += "<p>" + getResources().getString(R.string.privacy_what_subText6a) + "</p>";
//        mergedPolicyText += "<p>" + getResources().getString(R.string.privacy_what_subText6b) + "</p>";
//        mergedPolicyText += "<p>" + getResources().getString(R.string.privacy_what_subText6c) + "</p>";
//        mergedPolicyText += "<p>" + getResources().getString(R.string.privacy_what_subText6d) + "</p>";
        mergedPolicyText += "<h2>" + resources.getString(R.string.privacy_why_title) + "</h2>"
        mergedPolicyText += "<p>" + resources.getString(R.string.privacy_why_text1) + "</p>"
        mergedPolicyText += "<p>" + resources.getString(R.string.privacy_why_text2) + "</p>"
        mergedPolicyText += "<p>" + resources.getString(R.string.privacy_why_text3) + "</p>"
        mergedPolicyText += "<h2>" + resources.getString(R.string.privacy_how_title) + "</h2>"
        mergedPolicyText += "<p>" + resources.getString(R.string.privacy_how_text1) + "</p>"
        mergedPolicyText += "<h2>" + resources.getString(R.string.privacy_security_title) + "</h2>"
        mergedPolicyText += "<p>" + resources.getString(R.string.privacy_security_text) + "</p>"
        mergedPolicyText += "<h2>" + resources.getString(R.string.privacy_rights_title) + "</h2>"
        mergedPolicyText += "<p>" + resources.getString(R.string.privacy_rights_text) + "</p>"
        mergedPolicyText += "<h2>" + resources.getString(R.string.privacy_contact_title) + "</h2>"
        mergedPolicyText += "<p>" + resources.getString(R.string.privacy_contact_address) + "</p>"
        mergedPolicyText += "<p>" + resources.getString(R.string.privacy_contact_email) + "</p>"
        if (Build.VERSION.SDK_INT >= 24) {
            policyText.text = Html.fromHtml(mergedPolicyText, Html.FROM_HTML_MODE_LEGACY)
        } else {
            policyText.text = Html.fromHtml(mergedPolicyText)
        }
        policyText.movementMethod = LinkMovementMethod.getInstance()
        mDrawerToggle.isDrawerIndicatorEnabled = false
    }

    public override fun onResume() {
        mNavigationView.menu.findItem(R.id.nav_privacy).isChecked = true
        super.onResume()
    }
}