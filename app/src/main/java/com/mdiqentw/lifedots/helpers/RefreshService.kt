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
package com.mdiqentw.lifedots.helpers

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log

class RefreshService : JobService() {
    var isWorking = false
    var jobCancelled = false

    // Called by the Android system when it's time to run the job
    override fun onStartJob(jobParameters: JobParameters): Boolean {
        Log.d(TAG, "Job started... $jobParameters")
        isWorking = true

        /* UI refresh is so fast we can do it directly here */
//        ActivityHelper.helper.updateNotification();

        // We need 'jobParameters' so we can call 'jobFinished'
        startWorkOnNewThread(jobParameters)
        return isWorking
    }

    private fun startWorkOnNewThread(jobParameters: JobParameters) {
        Thread { refresh(jobParameters) }.start()
    }

    private fun refresh(jobParameters: JobParameters) {
        Log.d(TAG, "Job refreshing... $jobParameters")
        if (jobCancelled) return
        isWorking = false
        //        boolean needsReschedule = false;
        LocationHelper.helper.scheduleRefresh()
        LocationHelper.helper.updateLocation(true)
        jobFinished(jobParameters, false)
    }

    // Called if the job was cancelled before being finished
    override fun onStopJob(jobParameters: JobParameters): Boolean {
        Log.d(TAG, "Job stopping... $jobParameters")
        jobCancelled = true
        val needsReschedule = isWorking
        jobFinished(jobParameters, needsReschedule)
        return needsReschedule
    }

    companion object {
        private val TAG = RefreshService::class.java.name
    }
}