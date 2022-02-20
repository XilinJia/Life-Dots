/*
 * LifeDots
 *
 * Copyright (C) 2020 Xilin Jia https://github.com/XilinJia
  ~ Copyright (C) 2017-2018 Raphael Mack http://www.raphael-mack.de
 * Copyright (C) 2018 Sam Partee
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

import android.annotation.SuppressLint;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.TooltipCompat;
import androidx.databinding.DataBindingUtil;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.mdiqentw.lifedots.MVApplication;
import com.mdiqentw.lifedots.R;
import com.mdiqentw.lifedots.databinding.ActivityEditContentBinding;
import com.mdiqentw.lifedots.db.Contract;
import com.mdiqentw.lifedots.helpers.ActivityHelper;
import com.mdiqentw.lifedots.helpers.GraphicsHelper;
import com.mdiqentw.lifedots.model.DiaryActivity;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
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
 * EditActivity to add and modify activities
 *
 * */
public class EditActivity extends BaseActivity implements ActivityHelper.DataChangedListener {
    @Nullable
    private DiaryActivity currentActivity; /* null is for creating a new object */

    private static final int QUERY_NAMES = 1;
    private static final int RENAME_DELETED_ACTIVITY = 2;
    private static final int TEST_DELETED_NAME = 3;
    private static final int SIMILAR_ACTIVITY = 4;

    private static final String COLOR_KEY = "COLOR";
    private static final String NAME_KEY = "NAME";

    private static final int CHECK_STATE_CHECKING = 0;
    private static final int CHECK_STATE_OK = 1;
    private static final int CHECK_STATE_WARNING = 2;
    private static final int CHECK_STATE_ERROR = 3;
    private static final String[] NAME_TEST_PROJ = new String[]{Contract.DiaryActivity.NAME};

    private int checkState = CHECK_STATE_CHECKING;

    private ImageView mActivityColorImg;
    private int mActivityColor;

    private EditText mActivityName;
    private TextInputLayout mActivityNameTIL;
    private ImageButton mQuickFixBtn1;
    private ImageButton mBtnRenameDeleted;

    private int getCheckState() {
        return checkState;
    }

    private void setCheckState(int checkState) {
        this.checkState = checkState;
        if(checkState == CHECK_STATE_CHECKING && mActivityNameTIL != null){
            mActivityNameTIL.setError("...");
        }
    }

    public void doTokenSimilar(Cursor cursor) {
        if (cursor.moveToFirst()) {
            LinkedList<String> similarNames = new LinkedList<>();
            String sims = android.text.TextUtils.join(", ", similarNames);
            mActivityNameTIL.setError(getResources().getString(R.string.error_name_similar, sims));
            setCheckState(CHECK_STATE_WARNING);
        }
    }

    public void doTokenQueryName(Cursor cursor, AsyncQueryHandler handler) {
        if(cursor.moveToFirst()) {
            mQuickFixBtn1.setVisibility(View.VISIBLE);
            @SuppressLint("Range") boolean deleted = (cursor.getLong(cursor.getColumnIndex(Contract.DiaryActivity._DELETED)) != 0);
            @SuppressLint("Range") int actId = cursor.getInt(cursor.getColumnIndex(Contract.DiaryActivity._ID));
            @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(Contract.DiaryActivity.NAME));
            setCheckState(CHECK_STATE_ERROR);

            if(deleted) {
                CharSequence str = getResources().getString(R.string.error_name_already_used_in_deleted, cursor.getString(0));
                mBtnRenameDeleted.setVisibility(View.VISIBLE);
                setBtnTooltip(mBtnRenameDeleted, getResources().getString(R.string.tooltip_quickfix_btn_rename_deleted));
                mBtnRenameDeleted.setContentDescription(getResources().getString(R.string.contentDesc_renameDeletedActivity));

                mActivityNameTIL.setError(str);
                mQuickFixBtn1.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_undelete));
                setBtnTooltip(mQuickFixBtn1, getResources().getString(R.string.tooltip_quickfix_btn_undelete_existing));
                mQuickFixBtn1.setContentDescription(getResources().getString(R.string.contentDesc_undeleteActivity));


                mQuickFixBtn1.setOnClickListener(v -> {
                    currentActivity = ActivityHelper.helper.undeleteActivity(actId, name);
                    assert currentActivity != null;
                    Toast.makeText(this,
                            getResources().getString(R.string.recover_activity_toast, currentActivity.getName()),
                            Toast.LENGTH_LONG).show();

                    refreshElements();
                    setCheckState(CHECK_STATE_OK);
                });
                mBtnRenameDeleted.setOnClickListener(v -> {
                    setCheckState(CHECK_STATE_CHECKING);
                    Toast.makeText(this,
                            getResources().getString(R.string.renamed_deleted_activity_toast, name),
                            Toast.LENGTH_LONG).show();

                    ContentValues values = new ContentValues();
                    String newName = name + "_deleted";

                    values.put(Contract.DiaryActivity.NAME, newName);
                    values.put(Contract.DiaryActivity._ID, Long.valueOf(actId));
                    handler.startQuery(TEST_DELETED_NAME,
                            values,
                            Contract.DiaryActivity.CONTENT_URI,
                            NAME_TEST_PROJ,
                            Contract.DiaryActivity.NAME + " = ?",
                            new String[]{newName},
                            null
                    );
                    setCheckState(CHECK_STATE_OK);
                });
            } else {
                mActivityNameTIL.setError(getResources().getString(R.string.error_name_already_used, cursor.getString(0)));
                mBtnRenameDeleted.setVisibility(View.GONE);
                mBtnRenameDeleted.setOnClickListener(null);
                mQuickFixBtn1.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_edit));
                setBtnTooltip(mQuickFixBtn1, getResources().getString(R.string.tooltip_quickfix_btn_edit_existing));
                mQuickFixBtn1.setContentDescription(getResources().getString(R.string.contentDesc_rejectAndOpenActivity));
                mQuickFixBtn1.setOnClickListener(v -> {
                    currentActivity = ActivityHelper.helper.activityWithId(actId);
                    assert currentActivity != null;
                    Toast.makeText(this,
                            getResources().getString(R.string.edit_existing_activity_toast, currentActivity.getName()),
                            Toast.LENGTH_LONG).show();

                    refreshElements();
                    setCheckState(CHECK_STATE_OK);
                });
            }
        }
        else {
            mActivityNameTIL.setError("");
            mQuickFixBtn1.setVisibility(View.GONE);
            mQuickFixBtn1.setOnClickListener(null);
            mBtnRenameDeleted.setVisibility(View.GONE);
            mBtnRenameDeleted.setOnClickListener(null);
            setCheckState(CHECK_STATE_OK);
        }
    }

    private static class QHandler extends AsyncQueryHandler {
        final EditActivity act;

        /* Access only allowed via ActivityHelper.helper singleton */
        private QHandler(EditActivity act){
            super(MVApplication.getAppContext().getContentResolver());
            this.act = new WeakReference<>(act).get();
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if ((cursor != null)) {
                if(token == SIMILAR_ACTIVITY)
                    act.doTokenSimilar(cursor);
                else if(token == QUERY_NAMES)
                    act.doTokenQueryName(cursor, this);
                else if(token == TEST_DELETED_NAME) {
                    ContentValues values = (ContentValues)cookie;
                    if(cursor.moveToFirst()){
                        // name already exists, choose another one
                        String triedName = (String)values.get(Contract.Diary.NAME);
                        String newName = triedName.replaceFirst("-\\d+$", "");
                        String idx;
                        if(triedName.length() == newName.length()) {
                            // no "-x" at the end so far
                            idx = "-2";
                        }else{
                            String x = triedName.substring(newName.length() + 1);
                            idx = "-" + (Integer.parseInt(x) + 1);
                        }
                        newName += idx;
                        values.put(Contract.DiaryActivity.NAME, newName);
                        startQuery(TEST_DELETED_NAME, values,
                                Contract.DiaryActivity.CONTENT_URI,
                                NAME_TEST_PROJ,
                                Contract.DiaryActivity.NAME + " = ?",
                                new String[]{newName},
                                null
                        );

                    }

                    else {
                        // name not found, use it for the deleted one
                        Long actId = (Long)values.get(Contract.Diary._ID);
                        values.remove(Contract.Diary._ID);
                        startUpdate(RENAME_DELETED_ACTIVITY, null,
                                ContentUris.withAppendedId(Contract.DiaryActivity.CONTENT_URI, actId),
                                values, Contract.Diary._ID + " = " + actId, null);
                    }
                }
                cursor.close();
            }
            else {
                System.out.println("cursor was null");
            }
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            super.onUpdateComplete(token, cookie, result);
            if (token == RENAME_DELETED_ACTIVITY) {
                act.checkConstraints();
            } else {
                act.setCheckState(CHECK_STATE_OK);
            }
        }
    }

    private static void setBtnTooltip(View view, @Nullable CharSequence tooltipText) {
        if (Build.VERSION.SDK_INT < 26) {
            TooltipCompat.setTooltipText(view, tooltipText);
        }else{
            view.setTooltipText(tooltipText);
        }
    }

    /* refresh all view elements depending on currentActivity */
    private void refreshElements() {
        if (currentActivity != null) {
            mActivityName.setText(currentActivity.getName());
            Objects.requireNonNull(getSupportActionBar()).setTitle(currentActivity.getName());
            mActivityColor = currentActivity.getColor();
        } else {
            mActivityColor = GraphicsHelper.prepareColorForNextActivity();
        }
        mActivityColorImg.setBackgroundColor(mActivityColor);
    }

    private final QHandler mQHandler = new QHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        linkCol = ContextCompat.getColor(MVApplication.getAppContext(), R.color.colorAccent);
        setCheckState(CHECK_STATE_CHECKING);

//        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Intent i = getIntent();
        int actId = i.getIntExtra("activityID", -1);
        if(actId == -1) {
            currentActivity = null;
        }else {
            currentActivity = ActivityHelper.helper.activityWithId(actId);
        }

        ActivityEditContentBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_content);
        setContent(binding.getRoot());

        mActivityName = binding.editActivityName;
        mActivityName.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                checkConstraints();
                checkSimilarNames();
            }
        });
        mActivityNameTIL = binding.editActivityNameTil;
        mQuickFixBtn1 = binding.quickFixButton1;
        mBtnRenameDeleted = binding.quickFixButtonRename;

        mActivityColorImg = binding.editActivityColor;
        mActivityColorImg.setOnClickListener(v -> ColorPickerDialogBuilder
                .with(EditActivity.this)
                .setTitle("Choose color")
                .initialColor(R.color.activityTextColorLight)
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setOnColorSelectedListener(selectedColor -> {
//                                toast("onColorSelected: 0x" + Integer.toHexString(selectedColor));
                })
                .setPositiveButton("ok", (dialog, selectedColor, allColors) -> {
                    mActivityColor = selectedColor;
                    mActivityColorImg.setBackgroundColor(mActivityColor);
//                                changeBackgroundColor(selectedColor);
                })
                .setNegativeButton("cancel", (dialog, which) -> {
                })
                .build()
                .show());

        if(savedInstanceState != null) {
            String name = savedInstanceState.getString(NAME_KEY);
            mActivityColor = savedInstanceState.getInt(COLOR_KEY);
            mActivityName.setText(name);
            Objects.requireNonNull(getSupportActionBar()).setTitle(name);
            checkConstraints();
        }else{
            refreshElements();
        }
        mDrawerToggle.setDrawerIndicatorEnabled(false);
        Objects.requireNonNull(getSupportActionBar()).setHomeAsUpIndicator(R.drawable.ic_close_cancel);
        checkConstraints();
    }

    @Override
    public void onResume(){
        if(currentActivity == null) {
//            mNavigationView.getMenu().findItem(R.id.nav_add_activity).setChecked(true);
        }
        ActivityHelper.helper.registerDataChangeListener(this);

        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        ActivityHelper.helper.unregisterDataChangeListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(NAME_KEY, mActivityName.getText().toString());
        outState.putInt(COLOR_KEY, mActivityColor);
        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int mid = item.getItemId();
        if (mid == R.id.action_edit_delete) {
            if(currentActivity != null){
                ActivityHelper.helper.deleteActivity(currentActivity);
            }
            finish();
        } else if (mid == R.id.action_edit_done) {
            if(checkState != CHECK_STATE_CHECKING) {
                CharSequence error = mActivityNameTIL.getError();
                if (checkState == CHECK_STATE_ERROR) {
                    Toast.makeText(EditActivity.this,
                            error,
                            Toast.LENGTH_LONG
                    ).show();
                } else {
                    if (currentActivity == null) {
                        ActivityHelper.helper.insertActivity(new DiaryActivity(-1, mActivityName.getText().toString(), mActivityColor));
                    } else {
                        currentActivity.setName(mActivityName.getText().toString());
                        currentActivity.setColor(mActivityColor);
                        ActivityHelper.helper.updateActivity(currentActivity);
                    }
                    finish();
                }
            }
        } else if (mid == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void checkConstraints() {
        setCheckState(CHECK_STATE_CHECKING);

        if (currentActivity == null) {
            mQHandler.startQuery(QUERY_NAMES,
                    null,
                    Contract.DiaryActivity.CONTENT_URI,
                    new String[]{Contract.DiaryActivity.NAME, Contract.DiaryActivity._DELETED, Contract.DiaryActivity._ID},
                    Contract.DiaryActivity.NAME + "=?",
                    new String[]{mActivityName.getText().toString()}, null);
        } else {
            mQHandler.startQuery(QUERY_NAMES,
                    null,
                    Contract.DiaryActivity.CONTENT_URI,
                    new String[]{Contract.DiaryActivity.NAME, Contract.DiaryActivity._DELETED, Contract.DiaryActivity._ID},
                    Contract.DiaryActivity.NAME + "=? AND " +
                            Contract.DiaryActivity._ID + " != ?",
                    new String[]{mActivityName.getText().toString(), Long.toString(currentActivity.getId())},
                    null);
        }
    }

    private void checkSimilarNames() {
        setCheckState(CHECK_STATE_CHECKING);

        mQHandler.startQuery(SIMILAR_ACTIVITY,
                null,
                Contract.DiaryActivity.CONTENT_URI,
                new String[]{Contract.DiaryActivity.NAME, Contract.DiaryActivity._DELETED, Contract.DiaryActivity._ID},
                null, null,null);
    }

    /**
     * Called when the data has changed and no further specification is possible.
     * => everything needs to be refreshed!
     */
    @Override
    public void onActivityDataChanged() {
        refreshElements();
    }

    /**
     * Called when the data of one activity was changed.
     *
     * @param activity
     */
    @Override
    public void onActivityDataChanged(DiaryActivity activity) {
        if(activity == currentActivity){
            refreshElements();
        }
    }

    /**
     * Called on addition of an activity.
     *
     * @param activity
     */
    @Override
    public void onActivityAdded(DiaryActivity activity) {
        if(activity == currentActivity){
            refreshElements();
        }
    }

    /**
     * Called on removale of an activity.
     *
     * @param activity
     */
    @Override
    public void onActivityRemoved(DiaryActivity activity) {
        if(activity == currentActivity){
            refreshElements();
            // TODO: handle deletion of the activity while in editing it...
        }
    }

    /**
     * Called on change of the current activity.
     */
    @Override
    public void onActivityChanged() {

    }

    /**
     * Called on change of the activity order due to likelyhood.
     */
    @Override
    public void onActivityOrderChanged() {

    }
}
