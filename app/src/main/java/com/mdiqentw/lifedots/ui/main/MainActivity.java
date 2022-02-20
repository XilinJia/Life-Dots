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
package com.mdiqentw.lifedots.ui.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.mdiqentw.lifedots.BuildConfig;
import com.mdiqentw.lifedots.R;
import com.mdiqentw.lifedots.databinding.ActivityMainContentBinding;
import com.mdiqentw.lifedots.db.Contract;
import com.mdiqentw.lifedots.helpers.ActivityHelper;
import com.mdiqentw.lifedots.helpers.DateHelper;
import com.mdiqentw.lifedots.helpers.GraphicsHelper;
import com.mdiqentw.lifedots.helpers.TimeSpanFormatter;
import com.mdiqentw.lifedots.model.DetailViewModel;
import com.mdiqentw.lifedots.model.DiaryActivity;
import com.mdiqentw.lifedots.ui.generic.BaseActivity;
import com.mdiqentw.lifedots.ui.generic.EditActivity;
import com.mdiqentw.lifedots.ui.history.HistoryDetailActivity;
import com.mdiqentw.lifedots.ui.settings.SettingsActivity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
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
public class MainActivity extends BaseActivity
        implements SelectRecyclerViewAdapter.SelectListener,
        ActivityHelper.DataChangedListener,
        NoteEditDialog.NoteEditDialogListener,
        View.OnLongClickListener,
        SearchView.OnQueryTextListener,
        SearchView.OnCloseListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 4711;

    private static final int QUERY_CURRENT_ACTIVITY_STATS = 1;
    private static final int QUERY_CURRENT_ACTIVITY_TOTAL = 2;

    ActivityMainContentBinding binding;

    private DetailViewModel viewModel;

    private String mCurrentPhotoPath;

    private RecyclerView selectRecyclerView;
    FlexboxLayoutManager layoutManager;
    private SelectRecyclerViewAdapter selectAdapter;

    private String filter = "";
    private FloatingActionButton fabAttachPicture;
    private SearchView searchView;
    private View headerView;

    private void setSearchMode(boolean searchMode){
        if (searchMode) {
            headerView.setVisibility(View.GONE);
            fabAttachPicture.hide();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        } else {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            headerView.setVisibility(View.VISIBLE);
            fabAttachPicture.show();
        }
    }

    private MainAsyncQueryHandler mQHandler;

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("currentPhotoPath", mCurrentPhotoPath);

        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_content);
        setContent(binding.getRoot());
//        initNavigation();

        viewModel = new ViewModelProvider(this).get(DetailViewModel.class);

        mQHandler = new MainAsyncQueryHandler(getApplicationContext().getContentResolver(), viewModel);

        // recovering the instance state
        if (savedInstanceState != null) {
            mCurrentPhotoPath = savedInstanceState.getString("currentPhotoPath");
        }

//        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        View contentView = View.inflate(this, R.layout.activity_main_content, null);

//        setContent(binding.getRoot());

        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if(permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                Toast.makeText(this,R.string.perm_write_external_storage_xplain, Toast.LENGTH_LONG).show();
            }
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }

        headerView = binding.headerArea;
        TabLayout tabLayout = binding.tablayout;

        ViewPager viewPager = binding.viewpager;
        setupViewPager(viewPager);
        tabLayout.setupWithViewPager(viewPager);

        selectRecyclerView = binding.selectRecycler;

        View selector = binding.row.background;
        selector.setOnLongClickListener(this);
        selector.setOnClickListener(v -> {
            // TODO: get rid of this setting?
            if(PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext())
                    .getBoolean(SettingsActivity.KEY_PREF_DISABLE_CURRENT, true)){
                ActivityHelper.helper.setCurrentActivity(null);
            } else {
                Intent i = new Intent(MainActivity.this, HistoryDetailActivity.class);
                // no diaryEntryID will edit the last one
                startActivity(i);
            }
        });

        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.listPreferredItemHeightSmall, value, true);

        layoutManager = new FlexboxLayoutManager(this);
        layoutManager.setFlexDirection(FlexDirection.ROW);
        layoutManager.setJustifyContent(JustifyContent.FLEX_START);
        selectRecyclerView.setLayoutManager(layoutManager);

        Objects.requireNonNull(Objects.requireNonNull(getSupportActionBar())).setSubtitle(getResources().getString(R.string.activity_subtitle_main));

        likelyhoodSort();

        fabAttachPicture = binding.fabAttachPicture;

        fabAttachPicture.setOnClickListener(v -> {
            // Handle the click on the FAB
            if(viewModel.currentActivity() != null) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                        Log.i(TAG, "create file for image capture " + photoFile.getAbsolutePath());

                    } catch (IOException ex) {
                        // Error occurred while creating the File
                        Toast.makeText(MainActivity.this, getResources().getString(R.string.camera_error), Toast.LENGTH_LONG).show();
                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        // Save a file: path for use with ACTION_VIEW intents
                        mCurrentPhotoPath = photoFile.getAbsolutePath();

                        Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                                BuildConfig.APPLICATION_ID + ".fileprovider",
                                photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    }

                }
            }else{
                Toast.makeText(MainActivity.this, getResources().getString(R.string.no_active_activity_error), Toast.LENGTH_LONG).show();
            }
        });

        PackageManager pm = getPackageManager();

        if(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            fabAttachPicture.show();
        }else{
            fabAttachPicture.hide();
        }

        // Get the intent, verify the action and get the search query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            filterActivityView(query);
        }
        // TODO: this is crazy to call onActivityChagned here,
        //  as it reloads the statistics and refills the viewModel...
        //  Completely against the idea of the viewmodel :-(
        onActivityChanged(); /* do this at the very end to ensure that no Loader finishes its data loading before */
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_";
        if(viewModel.currentActivity().getValue() != null){
            imageFileName += Objects.requireNonNull(Objects.requireNonNull(viewModel.currentActivity().getValue())).getName();
            imageFileName += "_";
        }

        imageFileName += timeStamp;
        File storageDir = GraphicsHelper.imageStorageDirectory();

        File image = new File(storageDir, imageFileName + ".jpg");
        image.createNewFile();
        return image;

    }

    @Override
    public void onResume() {
        mNavigationView.getMenu().findItem(R.id.nav_main).setChecked(true);
        ActivityHelper.helper.registerDataChangeListener(this);
        onActivityChanged(); /* refresh the current activity data */
        super.onResume();

        selectAdapter.notifyDataSetChanged(); // redraw the complete recyclerview
        ActivityHelper.helper.evaluateAllConditions(); // this is quite heavy and I am not so sure whether it is a good idea to do it unconditionally here...
    }

    @Override
    public void onPause() {
        ActivityHelper.helper.unregisterDataChangeListener(this);

        super.onPause();
    }

    @Override
    public boolean onLongClick(View view) {
        Intent i = new Intent(MainActivity.this, EditActivity.class);
        if(viewModel.currentActivity().getValue() != null) {
            i.putExtra("activityID", Objects.requireNonNull(Objects.requireNonNull(viewModel.currentActivity().getValue())).getId());
        }
        startActivity(i);
        return true;
    }

    @Override
    public boolean onItemLongClick(int adapterPosition){
        Intent i = new Intent(MainActivity.this, EditActivity.class);
        i.putExtra("activityID", selectAdapter.item(adapterPosition).getId());
        startActivity(i);
        return true;
    }

    @Override
    public void onItemClick(int adapterPosition) {

        DiaryActivity newAct = selectAdapter.item(adapterPosition);
        if(newAct != ActivityHelper.helper.getCurrentActivity()) {

            ActivityHelper.helper.setCurrentActivity(newAct);

            searchView.setQuery("", false);
            searchView.setIconified(true);
        }
    }

    public void onActivityChanged(){
        DiaryActivity newAct = ActivityHelper.helper.getCurrentActivity();
        viewModel.mCurrentActivity.setValue(newAct);

        if(newAct != null) {
            queryAllTotals();
        }

        viewModel.setCurrentDiaryUri(ActivityHelper.helper.getCurrentDiaryUri());
        TextView aName = binding.row.name;
        // TODO: move this logic into the DetailViewModel??

//        viewModel.mAvgDuration.setValue("-");
//        viewModel.mStartOfLast.setValue("-");
//        viewModel.mTotalToday.setValue("-");
        /* stats are updated after query finishes in mQHelper */

        if(viewModel.currentActivity().getValue() != null) {
            aName.setText(Objects.requireNonNull(Objects.requireNonNull(viewModel.currentActivity().getValue())).getName());
            binding.row.background.setBackgroundColor(Objects.requireNonNull(Objects.requireNonNull(viewModel.currentActivity().getValue())).getColor());
            aName.setTextColor(GraphicsHelper.textColorOnBackground(Objects.requireNonNull(Objects.requireNonNull(viewModel.currentActivity().getValue())).getColor()));
            viewModel.mNote.setValue(ActivityHelper.helper.getCurrentNote());
        }else{
            int col = ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary);
            aName.setText(getResources().getString(R.string.activity_title_no_selected_act));
            binding.row.background.setBackgroundColor(col);
            aName.setTextColor(GraphicsHelper.textColorOnBackground(col));
            viewModel.mDuration.setValue("-");
            viewModel.mNote.setValue("");
        }
        layoutManager.scrollToPosition(0);
    }

    public void queryAllTotals() {
        // TODO: move this into the DetailStatFragement
        DiaryActivity a = viewModel.mCurrentActivity.getValue();
        if(a != null) {
            int id = a.getId();

//            TODO: need better display format
            long end = System.currentTimeMillis();
            long oneDayAgo = end - DateHelper.DAY_IN_MS ;
            queryTotal(Calendar.DAY_OF_YEAR, oneDayAgo, end, id);
//            queryTotal(DateHelper.FULLDAY, oneDayAgo, end, id);
            long sevenDaysAgo = end - (7 * DateHelper.DAY_IN_MS);
            queryTotal(Calendar.WEEK_OF_YEAR, sevenDaysAgo, end, id);
            long thirtyDaysAgo = end - (30 * DateHelper.DAY_IN_MS);
            queryTotal(Calendar.MONTH, thirtyDaysAgo, end, id);
//            queryTotal(Calendar.WEEK_OF_YEAR, end, id);
//            queryTotal(Calendar.MONTH, end, id);
        }
    }

    private void queryTotal(int field, long start, long end, int actID) {
        Uri u = Contract.DiaryStats.CONTENT_URI;
        u = Uri.withAppendedPath(u, Long.toString(start));
        u = Uri.withAppendedPath(u, Long.toString(end));

        mQHandler.startQuery(QUERY_CURRENT_ACTIVITY_TOTAL, new StatParam(field, end),
                u,
                new String[] {
                        Contract.DiaryStats.DURATION
                },
                Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivity._ID
                        + " = ?",
                new String[] {
                        Integer.toString(actID)
                },
                null);
    }

    /**
     * Called on change of the activity order due to likelyhood.
     */
    @Override
    public void onActivityOrderChanged() {
        /* only do likelihood sort in case we are not in a search */
        if(filter.length() == 0){
            likelyhoodSort();
        }
    }

    /**
     * Called when the data has changed.
     */
    @Override
    public void onActivityDataChanged() {
        selectAdapter.notifyDataSetChanged();
    }

    @Override
    public void onActivityDataChanged(DiaryActivity activity){
        selectAdapter.notifyItemChanged(selectAdapter.positionOf(activity));
    }

    /**
     * Called on addition of an activity.
     *
     */
    @Override
    public void onActivityAdded(DiaryActivity activity) {
        /* no need to add it, as due to the reevaluation of the conditions the order change will happen */
    }

    /**
     * Called on removale of an activity.
     */
    @Override
    public void onActivityRemoved(DiaryActivity activity) {
        selectAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchMenuItem = menu.findItem(R.id.action_filter);
        searchView = (SearchView) searchMenuItem.getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnCloseListener(this);
        searchView.setOnQueryTextListener(this);
        // setOnSuggestionListener -> for selection of a suggestion
        // setSuggestionsAdapter
        searchView.setOnSearchClickListener(v -> setSearchMode(true));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add_activity) {
            startActivity(new Intent(this, EditActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            filterActivityView(query);
        }

        if (intent.hasExtra("SELECT_ACTIVITY_WITH_ID")) {
            int id = intent.getIntExtra("SELECT_ACTIVITY_WITH_ID", -1);
            ActivityHelper.helper.setCurrentActivity(ActivityHelper.helper.activityWithId(id));
        }
    }

    private void filterActivityView(String query){
        this.filter = query;
        if(filter.length() == 0) {
            likelyhoodSort();
        } else {
            ArrayList<DiaryActivity> filtered = ActivityHelper.sortedActivities(query);
//
            selectAdapter = new SelectRecyclerViewAdapter(MainActivity.this, filtered);
            selectRecyclerView.swapAdapter(selectAdapter, false);
            selectRecyclerView.scrollToPosition(0);
        }
    }

    private void likelyhoodSort() {
        if (selectAdapter == null || selectAdapter != selectRecyclerView.getAdapter()) {
            selectAdapter = new SelectRecyclerViewAdapter(MainActivity.this, ActivityHelper.helper.getActivities());
            selectRecyclerView.swapAdapter(selectAdapter, false);
        } else {
            selectAdapter.setActivities(ActivityHelper.helper.getActivities());
        }
    }

    @Override
    public boolean onClose() {
        setSearchMode(false);
        likelyhoodSort();
        return false; /* we wanna clear and close the search */
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        setSearchMode(false);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        filterActivityView(newText);
        return true; /* we handle the search directly, so no suggestions need to be show even if #70 is implemented */
    }

    @Override
    public void onNoteEditPositiveClock(String str, DialogFragment dialog) {
        ContentValues values = new ContentValues();
        values.put(Contract.Diary.NOTE, str);

        mQHandler.startUpdate(0,
                null,
                viewModel.getCurrentDiaryUri(),
                values,
                null, null);

        viewModel.mNote.postValue(str);
        ActivityHelper.helper.setCurrentNote(str);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if(mCurrentPhotoPath != null && viewModel.getCurrentDiaryUri() != null) {
                Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                        BuildConfig.APPLICATION_ID + ".fileprovider",
                        new File(mCurrentPhotoPath));
                ContentValues values = new ContentValues();
                values.put(Contract.DiaryImage.URI, photoURI.toString());
                values.put(Contract.DiaryImage.DIARY_ID, viewModel.getCurrentDiaryUri().getLastPathSegment());

                mQHandler.startInsert(0,
                        null,
                        Contract.DiaryImage.CONTENT_URI,
                        values);

                if(PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext())
                        .getBoolean(SettingsActivity.KEY_PREF_TAG_IMAGES, true)) {
                    try {
                        ExifInterface exifInterface = new ExifInterface(mCurrentPhotoPath);
                        if (viewModel.currentActivity().getValue() != null) {
                            /* TODO: #24: when using hierarchical activities tag them all here, seperated with comma */
                            /* would be great to use IPTC keywords instead of EXIF UserComment, but
                             * at time of writing (2017-11-24) it is hard to find a library able to write IPTC
                             * to JPEG for android.
                             * pixymeta-android or apache/commons-imaging could be interesting for this.
                             * */
                            exifInterface.setAttribute(ExifInterface.TAG_USER_COMMENT,
                                    Objects.requireNonNull(Objects.requireNonNull(viewModel.currentActivity().getValue())).getName());
                            exifInterface.saveAttributes();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "writing exif data to " + mCurrentPhotoPath + " failed", e);
                    }
                }
            }
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new DetailStatFragement(), getResources().getString(R.string.fragment_detail_stats_title));
        adapter.addFragment(new DetailNoteFragment(), getResources().getString(R.string.fragment_detail_note_title));
        adapter.addFragment(new DetailPictureFragement(), getResources().getString(R.string.fragment_detail_pictures_title));
        viewPager.setAdapter(adapter);
    }

    static class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>(50);
        private final List<String> mFragmentTitleList = new ArrayList<>(50);

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    private static class MainAsyncQueryHandler extends AsyncQueryHandler{
        final DetailViewModel viewModel;

        public MainAsyncQueryHandler(ContentResolver cr, DetailViewModel viewModel) {
            super(cr);
            this.viewModel = viewModel;
        }

        @Override
        public void startQuery(int token, Object cookie, Uri uri, String[] projection, String selection, String[] selectionArgs, String orderBy) {
            super.startQuery(token, cookie, uri, projection, selection, selectionArgs, orderBy);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            super.onQueryComplete(token, cookie, cursor);
            if ((cursor != null) && cursor.moveToFirst()) {
                if (token == QUERY_CURRENT_ACTIVITY_STATS) {
//                    long avg = cursor.getLong(cursor.getColumnIndex(Contract.DiaryActivity.X_AVG_DURATION));
//                    viewModel.mAvgDuration.setValue(getResources().
//                            getString(R.string.avg_duration_description, TimeSpanFormatter.format(avg)));

//                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//                    String formatString = sharedPref.getString(SettingsActivity.KEY_PREF_DATETIME_FORMAT,
//                            getResources().getString(R.string.default_datetime_format));
//
//                    long start = cursor.getLong(cursor.getColumnIndex(Contract.DiaryActivity.X_START_OF_LAST));
//
//                    viewModel.mStartOfLast.setValue(getResources().
//                            getString(R.string.last_done_description, DateFormat.format(formatString, start)));

                }
                else if(token == QUERY_CURRENT_ACTIVITY_TOTAL) {
                    StatParam p = (StatParam)cookie;
                    @SuppressLint("Range") long total = cursor.getLong(cursor.getColumnIndex(Contract.DiaryStats.DURATION));

                    String x = DateHelper.dateFormat(p.field).format(p.end);
                    x = x + ": " + TimeSpanFormatter.format(total);
                    switch(p.field){
                        case Calendar.DAY_OF_YEAR:
                            viewModel.mTotalToday.setValue(x);
                            break;
                        case Calendar.WEEK_OF_YEAR:
                            viewModel.mTotalWeek.setValue(x);
                            break;
                        case Calendar.MONTH:
                            viewModel.mTotalMonth.setValue(x);
                            break;
                        default:
                            break;
                    }
                }
            }

            if (cursor != null) cursor.close();
        }
    }

    private static class StatParam {
        public final int field;
        public final long end;
        public StatParam(int field, long end) {
            this.field = field;
            this.end = end;
        }
    }
}
