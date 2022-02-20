/*
 * LifeDots
 *
 * Copyright (C) 2020 Xilin Jia https://github.com/XilinJia
  ~ Copyright (C) 2017-2018 Raphael Mack http://www.raphael-mack.de
 * Copyright (C) 2018 Bc. Ondrej Janitor
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

package com.mdiqentw.lifedots.db;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.mdiqentw.lifedots.R;
import com.mdiqentw.lifedots.helpers.ActivityHelper;
import com.mdiqentw.lifedots.model.DiaryActivity;

import static android.app.SearchManager.SUGGEST_COLUMN_ICON_1;
import static android.app.SearchManager.SUGGEST_COLUMN_INTENT_ACTION;
import static android.app.SearchManager.SUGGEST_COLUMN_INTENT_DATA;
import static android.app.SearchManager.SUGGEST_COLUMN_QUERY;
import static android.app.SearchManager.SUGGEST_COLUMN_TEXT_1;

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
 * Why a new Content Provider for Diary Activites?
 *
 * According https://developer.android.com/guide/topics/providers/content-provider-creating.html
 * we need it to do searching, synching or widget use of the data -> which in the long we all want to do.
 *
 * Additionally it is used as SearchProvider these days.
 * */
public class LDContentProvider extends ContentProvider {

    private static final int ACTIVITIES = 1;
    private static final int ACTIVITIES_ID = 2;
    private static final int CONDITIONS = 3;
    private static final int CONDITIONS_ID = 4;
    private static final int DIARY = 5;
    private static final int DIARY_ID = 6;
    private static final int DIARY_IMAGE = 7;
    private static final int DIARY_IMAGE_ID = 8;
    private static final int DIARY_LOCATION = 9;
    private static final int DIARY_LOCATION_ID = 10;
    private static final int DIARY_STATS = 11;
    private static final int SEARCH_RECENT_SUGGESTION = 12;
    private static final int SEARCH_SUGGESTION = 13;
    private static final int DIARY_SUGGESTION = 14;

    private static final String TAG = LDContentProvider.class.getName();

    public static final String SEARCH_ACTIVITY = "com.mdiqentw.lifedots.action.SEARCH_ACTIVITY";
    public static final String SEARCH_NOTE = "com.mdiqentw.lifedots.action.SEARCH_NOTE";
    public static final String SEARCH_GLOBAL = "com.mdiqentw.lifedots.action.SEARCH_GLOBAL";
    public static final String SEARCH_DATE = "com.mdiqentw.lifedots.action.SEARCH_DATE";

    // TODO: isn't this already somewhere else?
    public static final Uri SEARCH_URI = Uri.parse("content://" + Contract.AUTHORITY);

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final Pattern plusPattern = Pattern.compile("^/+");

    static {
        sUriMatcher.addURI(Contract.AUTHORITY, plusPattern.matcher(Contract.DiaryActivity.CONTENT_URI.getPath()).replaceAll(""), ACTIVITIES);
        sUriMatcher.addURI(Contract.AUTHORITY, plusPattern.matcher(Contract.DiaryActivity.CONTENT_URI.getPath()).replaceAll("") + "/#", ACTIVITIES_ID);

        sUriMatcher.addURI(Contract.AUTHORITY, plusPattern.matcher(Contract.Diary.CONTENT_URI.getPath()).replaceAll(""), DIARY);
        sUriMatcher.addURI(Contract.AUTHORITY, plusPattern.matcher(Contract.Diary.CONTENT_URI.getPath()).replaceAll("") + "/#", DIARY_ID);

        sUriMatcher.addURI(Contract.AUTHORITY, plusPattern.matcher(Contract.DiaryImage.CONTENT_URI.getPath()).replaceAll(""), DIARY_IMAGE);
        sUriMatcher.addURI(Contract.AUTHORITY, plusPattern.matcher(Contract.DiaryImage.CONTENT_URI.getPath()).replaceAll("") + "/#", DIARY_IMAGE_ID);

        sUriMatcher.addURI(Contract.AUTHORITY, plusPattern.matcher(Contract.DiaryStats.CONTENT_URI.getPath()).replaceAll(""), DIARY_STATS);
        sUriMatcher.addURI(Contract.AUTHORITY, plusPattern.matcher(Contract.DiaryStats.CONTENT_URI.getPath()).replaceAll("") + "/#/#", DIARY_STATS);

        sUriMatcher.addURI(Contract.AUTHORITY, plusPattern.matcher(Contract.DiaryLocation.CONTENT_URI.getPath()).replaceAll(""), DIARY_LOCATION);
        sUriMatcher.addURI(Contract.AUTHORITY, plusPattern.matcher(Contract.DiaryLocation.CONTENT_URI.getPath()).replaceAll("") + "/#", DIARY_LOCATION_ID);

        sUriMatcher.addURI(Contract.AUTHORITY, plusPattern.matcher(Contract.DiaryLocation.CONTENT_URI.getPath()).replaceAll(""), DIARY_LOCATION);
        sUriMatcher.addURI(Contract.AUTHORITY, plusPattern.matcher(Contract.DiaryLocation.CONTENT_URI.getPath()).replaceAll("") + "/#", DIARY_LOCATION_ID);
// TODO:
        sUriMatcher.addURI(Contract.AUTHORITY, "history/" + SearchManager.SUGGEST_URI_PATH_QUERY + "/", SEARCH_RECENT_SUGGESTION);
        sUriMatcher.addURI(Contract.AUTHORITY, "history/" + SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGESTION);
        sUriMatcher.addURI(Contract.AUTHORITY, plusPattern.matcher(Contract.DiarySearchSuggestion.CONTENT_URI.getPath()).replaceAll(""), DIARY_SUGGESTION);

        /* TODO #18 */
        sUriMatcher.addURI(Contract.AUTHORITY, "CONDITIONS", CONDITIONS);
        sUriMatcher.addURI(Contract.AUTHORITY, "CONDITIONS/#", CONDITIONS_ID);

    }

    private LocalDBHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new LocalDBHelper(getContext());
        return true; /* successfully loaded */
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
        boolean useRawQuery = false;
        String grouping = null;
        String sql = "";
        Cursor c;
        int id = 0;
        if(selection == null){
            selection = "";
        }

        MatrixCursor result = new MatrixCursor(new String[]{
                BaseColumns._ID,
                SUGGEST_COLUMN_TEXT_1,
                SUGGEST_COLUMN_ICON_1,
                SUGGEST_COLUMN_INTENT_ACTION,
                SUGGEST_COLUMN_INTENT_DATA,
                SUGGEST_COLUMN_QUERY
        });

        if (sUriMatcher.match(uri) < 1) {
            /* URI is not recognized, return an empty Cursor */
            result.close();
            return null;
        }
        switch (sUriMatcher.match(uri)) {
            case ACTIVITIES_ID:
            case CONDITIONS_ID:
            case DIARY_ID:
            case DIARY_IMAGE_ID:
            case DIARY_LOCATION_ID:
                if (selection != null) {
                    selection = selection + " AND ";
                } else {
                    selection = "";
                }
                selection = selection + "_id=" + uri.getLastPathSegment();
            default:
                /* empty */
        }

        switch (sUriMatcher.match(uri)) {
            case ACTIVITIES_ID: /* intended fall through */
            case ACTIVITIES:
                int n;
                boolean hasDiaryJoin = false;
                String tables = Contract.DiaryActivity.TABLE_NAME;
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = Contract.DiaryActivity.SORT_ORDER_DEFAULT;
                }
                n = 0;
                while(n < projection.length){
                    if(Contract.DiaryActivityJoinableColumns.X_AVG_DURATION.equals(projection[n])){
                        projection[n] = "AVG(" + Contract.DiaryColumns.END + " - "
                                + Contract.DiaryColumns.START + ") AS "
                                + Contract.DiaryActivityJoinableColumns.X_AVG_DURATION;
                        hasDiaryJoin = true;
                    }
                    if(Contract.DiaryActivityJoinableColumns.X_START_OF_LAST.equals(projection[n])){
                        projection[n] = "xx_start AS "
                                + Contract.DiaryActivityJoinableColumns.X_START_OF_LAST;
                        hasDiaryJoin = true;
                    }
                    n++;
                }
                if(hasDiaryJoin){
                    n = 0;
                    while(n < projection.length) {
                        if(Contract.DiaryActivityColumns._ID.equals(projection[n])){
                            projection[n] = Contract.DiaryActivity.TABLE_NAME + "."
                                    + Contract.DiaryActivityColumns._ID;
                        }
                        n++;
                    }
                    selection = selection.replaceAll(" " + Contract.DiaryActivityColumns._ID, " " + Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._ID);
                    selection = selection.replaceAll(Contract.DiaryActivityColumns._DELETED, Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._DELETED);

                    tables = tables + ", " + Contract.Diary.TABLE_NAME;
                    tables = tables + ", (SELECT xx_ref, " + Contract.DiaryColumns.START + " as xx_start FROM " + Contract.Diary.TABLE_NAME + ","
                                    +     "(SELECT " + Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._ID + " AS xx_ref,"
                                                 + " MAX(" + Contract.Diary.TABLE_NAME + "." + Contract.DiaryColumns.END + ") AS xx_ref_end"
                                    +     " FROM " + Contract.DiaryActivity.TABLE_NAME + ", " + Contract.Diary.TABLE_NAME
                                    +     " WHERE " +  Contract.Diary.TABLE_NAME + "." + Contract.DiaryColumns.ACT_ID
                                    +           " = " + Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._ID
                                    +     " GROUP BY " + Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._ID
                                    +     ")"
                                    +    " WHERE " +  Contract.Diary.TABLE_NAME + "." + Contract.DiaryColumns.END + " = xx_ref_end"
                                    +  ")"
                                        ;

                    selection = selection + " AND " + Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._ID + " = " + Contract.Diary.TABLE_NAME + "." + Contract.DiaryColumns.ACT_ID
                                          + " AND " + Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._ID + " = xx_ref";

                    grouping = Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._ID;

                }
                qBuilder.setTables(tables);
                break;

            case DIARY_IMAGE_ID: /* intended fall through */
            case DIARY_IMAGE:
                qBuilder.setTables(Contract.DiaryImage.TABLE_NAME);
                if (TextUtils.isEmpty(sortOrder))
                    sortOrder = Contract.DiaryImage.SORT_ORDER_DEFAULT;
                break;
            case DIARY_LOCATION_ID: /* intended fall through */
            case DIARY_LOCATION:
                qBuilder.setTables(Contract.DiaryLocation.TABLE_NAME);
                if (TextUtils.isEmpty(sortOrder))
                    sortOrder = Contract.DiaryLocation.SORT_ORDER_DEFAULT;
                break;
            case DIARY_ID: /* intended fall through */
            case DIARY:
                /* rewrite projection, to prefix with tables */
                qBuilder.setTables(Contract.Diary.TABLE_NAME + " INNER JOIN " +
                        Contract.DiaryActivity.TABLE_NAME + " ON " +
                        Contract.Diary.TABLE_NAME + "." + Contract.DiaryColumns.ACT_ID + " = " +
                        Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._ID
                );
                if (TextUtils.isEmpty(sortOrder))
                    sortOrder = Contract.Diary.SORT_ORDER_DEFAULT;
                break;
            case DIARY_STATS:
                useRawQuery = true;
                List<String> l = uri.getPathSegments();
                String start;
                String end;

                if(l.size() == 3){
                    // we have a range query with start and end timestamps here
                    start = l.get(1);
                    end = l.get(2);
                }else{
                    start = "0";
                    end = "6156000000000"; // this is roughly 200 year since epoch, congratulations if this lasted so long...
                }

//                System.out.println(start + " " + end);
                String subselect = "SELECT SUM(MIN(IFNULL(" + Contract.DiaryColumns.END + ",strftime('%s','now') * 1000), " + end + ") - "
                        + "MAX(" + Contract.DiaryColumns.START + ", " + start + ")) from " + Contract.Diary.TABLE_NAME
                        + " WHERE ((start >= " + start + " AND start < " + end + ") OR (end > " + start + " AND end <= " + end + ") OR (start < " + start + " AND end > " + end + "))";

                if (selection != null && selection.length() > 0) {
                    subselect += " AND (" + selection + ")";
                }

                sql = "SELECT " + Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityJoinableColumns.NAME + " as " + Contract.DiaryStats.NAME
                        + ", " + Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityJoinableColumns.COLOR + " as " + Contract.DiaryStats.COLOR
                        + ", SUM(MIN(IFNULL(" + Contract.DiaryColumns.END + ",strftime('%s','now') * 1000), " + end + ") - MAX(" + start + ", " + Contract.DiaryColumns.START + ")) as " + Contract.DiaryStats.DURATION
                        + ", (SUM(MIN(IFNULL(" + Contract.DiaryColumns.END + ",strftime('%s','now') * 1000), " + end + ") - MAX(" + start + ", " + Contract.DiaryColumns.START + ")) * 100.0 " +
                        "/ (" + subselect + ")) as " + Contract.DiaryStats.PORTION
                        + " FROM " + Contract.Diary.TABLE_NAME + ", " + Contract.DiaryActivity.TABLE_NAME
                        + " WHERE " + Contract.Diary.TABLE_NAME + "." + Contract.DiaryColumns.ACT_ID + " = " + Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._ID + " AND"
                        + " ((start >= " + start + " AND start < " + end + ") OR (end > " + start + " AND end <= " + end + ") OR (start < " + start + " AND end > " + end + "))"
                ;
                if(selection != null && selection.length() > 0) {
                    sql += " AND (" + selection + ")";
                    String[] newArgs = Arrays.copyOf(selectionArgs, selectionArgs.length * 2);
                    System.arraycopy(selectionArgs, 0, newArgs, selectionArgs.length, selectionArgs.length);
                    selectionArgs = newArgs;
                }
                sql += " GROUP BY " + Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._ID;
                if (sortOrder != null && sortOrder.length() > 0) {
                    sql += " ORDER by " + sortOrder;
                }
                break;

            case SEARCH_RECENT_SUGGESTION:

                sql = "SELECT " + Contract.DiarySearchSuggestion.SUGGESTION + ", " +
                        Contract.DiarySearchSuggestion.ACTION + " FROM " +
                        Contract.DiarySearchSuggestion.TABLE_NAME +
                        " ORDER BY " + Contract.DiarySearchSuggestion._ID + " DESC";

                c = mOpenHelper.getReadableDatabase().rawQuery(sql, selectionArgs);
                if (c != null && c.moveToFirst()) {
                    do {
                        Object icon = null;
                        String action = c.getString(1);
                        String q = c.getString(0); // what do we want to display

                        switch (action) {
                            case SEARCH_ACTIVITY:
                                /* icon stays null */
                                int i = Integer.parseInt(q);
                                q = ActivityHelper.helper.activityWithId(i).getName();
                                break;
                            case SEARCH_NOTE:
                                q = getContext().getResources().getString(R.string.search_notes, q);
                                icon = R.drawable.ic_search;
                                break;
                            case SEARCH_GLOBAL:
                            case Intent.ACTION_SEARCH:
                                q = getContext().getResources().getString(R.string.search_diary, q);
                                icon = R.drawable.ic_search;
                                break;
                            case SEARCH_DATE:
                                q = getContext().getResources().getString(R.string.search_date, q);
                                icon = R.drawable.ic_calendar;
                                break;
                        }

                        result.addRow(new Object[]{id++,
                                q,
                                /* icon */ icon,
                                /* intent action */ action,
                                /* intent data */ Uri.withAppendedPath(SEARCH_URI, c.getString(0)),
                                /* rewrite query */c.getString(0)
                        });
                    } while (c.moveToNext());
                }
                c.close();
                return result;


            case SEARCH_SUGGESTION:
                String query = uri.getLastPathSegment(); //.toLowerCase();

                if (query != null && query.length() > 0) {
                    // ACTIVITIES matching the current search
                    ArrayList<DiaryActivity> filtered = ActivityHelper.sortedActivities(query);

                    // TODO: make the amount of ACTIVITIES shown configurable
                    for (int i = 0; i < 3; i++) {
                        if (i < filtered.size()) {
                            result.addRow(new Object[]{id++,
                                    filtered.get(i).getName(),
                                    /* icon */ null,
                                    /* intent action */ SEARCH_ACTIVITY,
                                    /* intent data */ Uri.withAppendedPath(SEARCH_URI, Integer.toString(filtered.get(i).getId())),
                                    /* rewrite query */filtered.get(i).getName()
                            });
                        }
                    }
                    // Notes
                    result.addRow(new Object[]{id++,
                            getContext().getResources().getString(R.string.search_notes, query),
                            /* icon */ R.drawable.ic_search,
                            /* intent action */ SEARCH_NOTE,
                            /* intent data */ Uri.withAppendedPath(SEARCH_URI, query),
                            /* rewrite query */ query
                    });

                    // Global search
                    result.addRow(new Object[]{id++,
                            getContext().getResources().getString(R.string.search_diary, query),
                            /* icon */ R.drawable.ic_search,
                            /* intent action */ SEARCH_GLOBAL,
                            /* intent data */ Uri.withAppendedPath(SEARCH_URI, query),
                            /* rewrite query */ query
                    });

                    // Date
                    result.addRow(new Object[]{id++,
                            getContext().getResources().getString(R.string.search_date, query),
                            /* icon */ R.drawable.ic_calendar,
                            /* intent action */ SEARCH_DATE,
                            /* intent data */ Uri.withAppendedPath(SEARCH_URI, query),
                            /* rewrite query */ query
                    });

                    // has Pictures
                    // TODO: add picture search

                    // Location (GPS)
                    // TODO: add location search

                }
                return result;

            case CONDITIONS_ID:
                /* intended fall through */
            case CONDITIONS:
//                qBuilder.setTables(Contract.Condition.TABLE_NAME);
                /* TODO #18               if (TextUtils.isEmpty(sortOrder)) sortOrder = Contract.Conditions.SORT_ORDER_DEFAULT; */
            default:
                /* empty */
        }

        if (useRawQuery) {
            c = mOpenHelper.getReadableDatabase().rawQuery(sql, selectionArgs);
        } else {
            c = qBuilder.query(mOpenHelper.getReadableDatabase(),
                    projection,
                    selection,
                    selectionArgs,
                    grouping,
                    null,
                    sortOrder);
        }
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case ACTIVITIES:
                return Contract.DiaryActivity.CONTENT_TYPE;
            case ACTIVITIES_ID:
                return Contract.DiaryActivity.CONTENT_ITEM_TYPE;
            case DIARY:
                return Contract.Diary.CONTENT_TYPE;
            case DIARY_ID:
                return Contract.Diary.CONTENT_ITEM_TYPE;
            case DIARY_LOCATION:
                return Contract.DiaryLocation.CONTENT_TYPE;
            case DIARY_LOCATION_ID:
                return Contract.DiaryLocation.CONTENT_ITEM_TYPE;
            case DIARY_STATS:
                return Contract.DiaryStats.CONTENT_TYPE;
            // TODO #18: add other types
            default:
                Log.e(TAG, "MIME type for " + uri.toString() + " not defined.");
                return "";
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        String table;
        Uri resultUri;

        switch (sUriMatcher.match(uri)) {
            case ACTIVITIES:
                table = Contract.DiaryActivity.TABLE_NAME;
                resultUri = Contract.DiaryActivity.CONTENT_URI;
                break;
            case DIARY:
                table = Contract.Diary.TABLE_NAME;
                resultUri = Contract.Diary.CONTENT_URI;
                break;
            case DIARY_IMAGE:
                table = Contract.DiaryImage.TABLE_NAME;
                resultUri = Contract.DiaryImage.CONTENT_URI;
                break;
            case DIARY_LOCATION:
                table = Contract.DiaryLocation.TABLE_NAME;
                resultUri = Contract.DiaryLocation.CONTENT_URI;
                break;
            case DIARY_SUGGESTION:
                table = Contract.DiarySearchSuggestion.TABLE_NAME;
                resultUri = Contract.DiarySearchSuggestion.CONTENT_URI;
                break;
            case CONDITIONS:
//                table = Contract.Condition.TABLE_NAME;
// TODO #18               resultUri = Contract.Condition.CONTENT_URI;
//                break;
            case DIARY_STATS: /* intended fall-through */
            default:
                throw new IllegalArgumentException(
                        "Unsupported URI for insertion: " + uri);
        }
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        db.beginTransaction();
        long id = db.insertOrThrow(table,
                null,
                values);
        db.setTransactionSuccessful();
        db.endTransaction();

        if(id > 0) {
            resultUri = ContentUris.withAppendedId(resultUri, id);
            getContext().
                    getContentResolver().
                    notifyChange(resultUri, null);

            return resultUri;
        } else {
            throw new SQLException(
                    "Problem while inserting into uri: " + uri + " values " + values.toString());
        }
    }

    /**
     * Implement this to handle requests to delete one or more rows.
     * The implementation should apply the selection clause when performing
     * deletion, allowing the operation to affect multiple rows in a directory.
     * This method can be called from multiple threads, as described in
     * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads">Processes
     * and Threads</a>.
     * <p>
     * <p>The implementation is responsible for parsing out a row ID at the end
     * of the URI, if a specific row is being deleted. That is, the client would
     * pass in <code>content://contacts/people/22</code> and the implementation is
     * responsible for parsing the record number (22) when creating a SQL statement.
     *
     * @param uri           The full URI to query, including a row ID (if a specific record is requested).
     * @param selection     An optional restriction to apply to rows when deleting.
     * @param selectionArgs
     * @return The number of rows affected.
     * @throws SQLException
     */
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        boolean isGlobalDelete = false;
        String table;
        ContentValues values = new ContentValues();
        switch (sUriMatcher.match(uri)) {
            case ACTIVITIES_ID:
                table = Contract.DiaryActivity.TABLE_NAME;
                break;
            case DIARY:
                isGlobalDelete = true;
                /* fall though */
            case DIARY_ID:
                table = Contract.Diary.TABLE_NAME;
                break;
            case DIARY_IMAGE:
                isGlobalDelete = true;
                /* fall though */
            case DIARY_IMAGE_ID:
                table = Contract.DiaryImage.TABLE_NAME;
                break;
            case DIARY_LOCATION:
                isGlobalDelete = true;
                /* fall though */
            case DIARY_LOCATION_ID:
                table = Contract.DiaryLocation.TABLE_NAME;
                break;
            case DIARY_SUGGESTION:
                table = Contract.DiarySearchSuggestion.TABLE_NAME;
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                return db.delete(table, selection, selectionArgs);
            case CONDITIONS_ID:
//                table = Contract.Condition.TABLE_NAME;
//                break;
            case DIARY_STATS: /* intended fall-through */
            default:
                throw new IllegalArgumentException(
                        "Unsupported URI for deletion: " + uri);
        }
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        if (!isGlobalDelete) {
            if (selection != null) {
                selection = selection + " AND ";
            } else {
                selection = "";
            }
            selection = selection + "_id=" + uri.getLastPathSegment();
        }
        values.put(Contract.DiaryActivityColumns._DELETED, "1");

        db.beginTransaction();
        int upds = db.update(table,
                values,
                selection,
                selectionArgs);
        if (upds > 0) {
            getContext().
                    getContentResolver().
                    notifyChange(uri, null);

        } else {
            Log.i(TAG, "Could not delete anything for uri: " + uri + " with selection '" + selection + "'");
        }
        db.setTransactionSuccessful();
        db.endTransaction();

        return upds;
    }

    /**
     * Implement this to handle requests to update one or more rows.
     * The implementation should update all rows matching the selection
     * to set the columns according to the provided values map.
     * This method can be called from multiple threads, as described in
     * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads">Processes
     * and Threads</a>.
     *
     * @param uri           The URI to query. This can potentially have a record ID if this
     *                      is an update request for a specific record.
     * @param values        A set of column_name/value pairs to update in the database.
     *                      This must not be {@code null}.
     * @param selection     An optional filter to match rows to update.
     * @param selectionArgs
     * @return the number of rows affected.
     */
    @Override
    public int update(@NonNull Uri uri, @NonNull ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        String table;
        boolean isID = false;
        switch (sUriMatcher.match(uri)) {
            case ACTIVITIES_ID:
                isID = true;
                table = Contract.DiaryActivity.TABLE_NAME;
                break;
            case DIARY_ID:
                isID = true;
            case DIARY:
                table = Contract.Diary.TABLE_NAME;
                break;
            case DIARY_IMAGE:
                table = Contract.DiaryImage.TABLE_NAME;
                break;
            case DIARY_LOCATION_ID:
                isID = true;
            case DIARY_LOCATION:
                table = Contract.DiaryLocation.TABLE_NAME;
                break;
            case CONDITIONS_ID:
                //                table = Contract.Condition.TABLE_NAME;
//                break;
            case DIARY_STATS: /* intended fall-through */
            default:
                throw new IllegalArgumentException(
                        "Unsupported URI for update: " + uri);
        }
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        if (isID) {
            if (selection != null) {
                selection = selection + " AND ";
            } else {
                selection = "";
            }
            selection = selection + "_id=" + uri.getLastPathSegment();
        }

        int upds = db.update(table,
                values,
                selection,
                selectionArgs);
        if (upds > 0) {
            getContext().
                    getContentResolver().
                    notifyChange(uri, null);

        } else if (isID) {
            throw new SQLException(
                    "Problem while updating uri: " + uri + " with selection '" + selection + "'");
        }
        return upds;
    }

    public void resetDatabase() {
        mOpenHelper.close();
    }


    /**
     * Search for all dates in database which match start/end date or are in range (between start and end date)
     * @param dateInMillis - date is searched
     * @return query (string) with ids that fulfills defined CONDITIONS
     */
    @SuppressLint("Range")
    public String searchDate(Long dateInMillis) {
        // TODO: move this into the method query, for the case DIARY,
        // similar to DIARY_STATS, we can modify selection and selection args there
        // or maybe better, invent a new URI like "DIARY/number" where number is the dateInMillis
        // Alternative: move all this directly into HistoryActivity.onCreateLoader

        String querySelection = " ", id;
        long searchedValue = dateInMillis;
        long searchedValuePlusDay = searchedValue + 86400000; // TODO: replace magic numbers by the formula to calculate them...
        long searchSpecialCase = searchedValue + 86399999;  //used for searching for still running activity

        try (Cursor allRowsStart = mOpenHelper.getReadableDatabase().rawQuery(
                "SELECT " + Contract.DiaryColumns._ID
                        + " FROM " + Contract.Diary.TABLE_NAME
                        + " WHERE " + "(" + searchedValue + " >= " + Contract.DiaryColumns.START + " AND " + searchedValue + " <= " + Contract.DiaryColumns.END + ")" + " OR " +
                        "(" + searchedValuePlusDay + " >= " + Contract.DiaryColumns.START + " AND " + searchedValuePlusDay + " <= " + Contract.DiaryColumns.END + ")" + " OR " +
                        "(" + searchedValue + " < " + Contract.DiaryColumns.START + " AND " + searchedValuePlusDay + " > " + Contract.DiaryColumns.END + ")" + " OR " +
                        "(" + searchSpecialCase + " >= " + Contract.DiaryColumns.START + " AND " + Contract.DiaryColumns.END + " IS NULL" + ")", null)) {
// TODO: -> this query should not be executed outside of the method LDContentProvider.query

            if (allRowsStart.moveToFirst()) {
                do {
                    for (String name : allRowsStart.getColumnNames()) {
                        id = (allRowsStart.getString(allRowsStart.getColumnIndex(name)));
                        querySelection += querySelection.equals(" ") ? Contract.Diary.TABLE_NAME + "." + Contract.DiaryColumns._ID + " =" + id : " OR " + Contract.Diary.TABLE_NAME + "." + Contract.DiaryColumns._ID + " =" + id;
                    }
                } while (allRowsStart.moveToNext());
            }
        } catch (Exception e) {
            // TODO: add proper exception handling. Also "Exception" seems quite generic -> catch all exceptions that can occur directly
        }

        // if there is no matching dates it returns query which links to find nothings
        // otherwise it will return query with IDs of matching dates
        return querySelection.equals(" ") ?  " start=null" : querySelection;
    }
}
