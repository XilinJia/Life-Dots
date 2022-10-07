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
package com.mdiqentw.lifedots.model.conditions

import android.os.Process
import androidx.preference.PreferenceManager
import com.mdiqentw.lifedots.MVApplication
import com.mdiqentw.lifedots.db.LocalDBHelper
import com.mdiqentw.lifedots.helpers.ActivityHelper
import com.mdiqentw.lifedots.model.DiaryActivity

/*
 * Conditions model a specific aspect which influences the likelihood of the activities.
 **/
abstract class Condition {
    @JvmField
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MVApplication.appContext!!)

    class Likelihood(val activity: DiaryActivity, var likelihood: Double)

    /* storage for the likelyhoods */
    private var result: List<Likelihood> = ArrayList(1)

    /*
     * return TRUE if the evaluation is in progress
     * callable from everywhere
     */
    /* is the worker thread for the current Condition evaluating */
    @get:Synchronized
    var isActive = false
        private set
    @JvmField
    protected var weight = 0.0
    private val worker = Thread {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        while (true) {
            if (isActive) {
                doEvaluation()
                isActive = false
                ActivityHelper.helper.conditionEvaluationFinished()
            }
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
            }
        }
    }

    /*
     * return the likelihoods
     * return a list of DiaryActivities which have a non-zero likelihood under this condition
     * all activities not in the result are assumed to have likelyhood zero
     */
    @Synchronized
    fun likelihoods(): List<Likelihood> {
        return result
    }

    /*
     * set the result from the doEvaluation method in subclasses
     */
    @Synchronized
    protected fun setResult(likelihoods: List<Likelihood>) {
        result = likelihoods
    }

    /*
     * trigger the likelyhood evaluation
     * callable in any thread, creates thread and evaluates
     */
    fun refresh() {
        // TODO: it seems to be a good idea to put the thread somehow into the WAIT state
        if (worker.state == Thread.State.NEW) {
            worker.start()
            worker.name = this.javaClass.simpleName + "-" + worker.name
        } else {
            worker.interrupt()
        }
        isActive = true
    }

    /*
     * the likelyhood evaluation, to be executed in Condition thread
     * this shall call Condition.setResult on finish, and NOT modify result directly
     */
    protected abstract fun doEvaluation()

    /**
     * Called on change of the activity order due to likelyhood.
     */
    fun onActivityOrderChanged() {
        // would be very bad to add code here :-)
    }

    fun name(): String {
        return javaClass.name
    }

    companion object {
        /* it seems most conditions will need dedicated database operations, and we don't want
     * to mess up with the ContentProvider, so let's get a new helper here */
        @JvmField
        val mOpenHelper = LocalDBHelper(MVApplication.appContext!!)
    }
}