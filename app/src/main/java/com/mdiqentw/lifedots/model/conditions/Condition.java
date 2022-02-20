/*
 * LifeDots
 *
 * Copyright (C) 2018 Raphael Mack http://www.raphael-mack.de
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

package com.mdiqentw.lifedots.model.conditions;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

import com.mdiqentw.lifedots.MVApplication;
import com.mdiqentw.lifedots.db.LocalDBHelper;
import com.mdiqentw.lifedots.helpers.ActivityHelper;
import com.mdiqentw.lifedots.model.DiaryActivity;

import static java.lang.Thread.State.NEW;

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
 * Conditions model a specific aspect which influences the likelihood of the activities.
 **/
public abstract class Condition {
    final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MVApplication.getAppContext());

    public static class Likelihood{
        public final DiaryActivity activity;
        public double likelihood;

        public Likelihood(DiaryActivity a, double likelihood){
            activity = a;
            this.likelihood = likelihood;
        }
    }

    /* it seems most conditions will need dedicated database operations, and we don't want
     * to mess up with the ContentProvider, so let's get a new helper here */
    static final LocalDBHelper mOpenHelper = new LocalDBHelper(MVApplication.getAppContext());

    /* storage for the likelyhoods */
    private @NonNull List<Likelihood> result = new ArrayList<>(1);

    /* is the worker thread for the current Condition evaluating */
    private boolean isActive = false;

    protected double weight;

    private final Thread worker = new Thread(() -> {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        while(true){
            if(isActive) {
                doEvaluation();
                isActive = false;
                ActivityHelper.helper.conditionEvaluationFinished();
            }
            try {
                Thread.sleep(1000);
            }catch(InterruptedException e){

            }
        }
    });

    /*
     * return the likelihoods
     * return a list of DiaryActivities which have a non-zero likelihood under this condition
     * all activities not in the result are assumed to have likelyhood zero
     */
    public synchronized @NonNull List<Likelihood> likelihoods(){
        return result;
    }

    /*
     * set the result from the doEvaluation method in subclasses
     */
    protected synchronized void setResult(@NonNull List<Likelihood> likelihoods){
        result = likelihoods;
    }

    /*
     * trigger the likelyhood evaluation
     * callable in any thread, creates thread and evaluates
     */
    public void refresh(){
        // TODO: it seems to be a good idea to put the thread somehow into the WAIT state
        if(worker.getState() == NEW){
            worker.start();
            worker.setName(this.getClass().getSimpleName() + "-" + worker.getName());
        }else {
            worker.interrupt();
        }
        isActive = true;
    }

    /*
     * return TRUE if the evaluation is in progress
     * callable from everywhere
     */
    public synchronized boolean isActive(){
        return isActive;
    }

    /*
     * the likelyhood evaluation, to be executed in Condition thread
     * this shall call Condition.setResult on finish, and NOT modify result directly
     */
    protected abstract void doEvaluation();

    /**
     * Called on change of the activity order due to likelyhood.
     */
    public final void onActivityOrderChanged() {
        // would be very bad to add code here :-)
    }

    public String name() {
        return getClass().getName();
    }
}

