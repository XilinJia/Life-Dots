/*
 * LifeDots
 *
 * Copyright (C) 2017-2018 Raphael Mack http://www.raphael-mack.de
 * Copyright (C) 2018 Bc. Ondrej Janitor
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
package com.mdiqentw.lifedots.db

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.*
import android.database.Cursor
import android.database.MatrixCursor
import android.database.SQLException
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.provider.BaseColumns
import android.text.TextUtils
import android.util.Log
import com.mdiqentw.lifedots.R
import com.mdiqentw.lifedots.helpers.ActivityHelper
import com.mdiqentw.lifedots.helpers.ActivityHelper.Companion.sortedActivities
import java.util.*
import java.util.regex.Pattern

/*
 * Why a new Content Provider for Diary Activites?
 *
 * According https://developer.android.com/guide/topics/providers/content-provider-creating.html
 * we need it to do searching, synching or widget use of the data -> which in the long we all want to do.
 *
 * Additionally it is used as SearchProvider these days.
 * */
class LDContentProvider : ContentProvider() {
    private var mOpenHelper: LocalDBHelper? = null
    override fun onCreate(): Boolean {
        mOpenHelper = LocalDBHelper(context)
        return true /* successfully loaded */
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        var selection = selection
        var selectionArgs = selectionArgs
        var sortOrder = sortOrder
        val qBuilder = SQLiteQueryBuilder()
        var useRawQuery = false
        var grouping: String? = null
        var sql = ""
        val c: Cursor?
        var id = 0
        if (selection == null) {
            selection = ""
        }
        val result = MatrixCursor(
            arrayOf(
                BaseColumns._ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_ICON_1,
                SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA,
                SearchManager.SUGGEST_COLUMN_QUERY
            )
        )
        if (sUriMatcher.match(uri) < 1) {
            /* URI is not recognized, return an empty Cursor */
            result.close()
            return null
        }
        when (sUriMatcher.match(uri)) {
            ACTIVITIES_ID, CONDITIONS_ID, DIARY_ID, DIARY_IMAGE_ID, DIARY_LOCATION_ID -> {
                selection = "$selection AND "
                selection = selection + "_id=" + uri.lastPathSegment
            }

            else -> {}
        }
        when (sUriMatcher.match(uri)) {
            ACTIVITIES_ID, ACTIVITIES -> {
                var n: Int
                var hasDiaryJoin = false
                var tables = Contract.DiaryActivity.TABLE_NAME
                if (TextUtils.isEmpty(sortOrder)) {
                    sortOrder = Contract.DiaryActivity.SORT_ORDER_DEFAULT
                }
                n = 0
                while (n < projection!!.size) {
                    if (Contract.DiaryActivityJoinableColumns.X_AVG_DURATION == projection[n]) {
                        projection[n] = ("AVG(" + Contract.DiaryColumns.END + " - "
                                + Contract.DiaryColumns.START + ") AS "
                                + Contract.DiaryActivityJoinableColumns.X_AVG_DURATION)
                        hasDiaryJoin = true
                    }
                    if (Contract.DiaryActivityJoinableColumns.X_START_OF_LAST == projection[n]) {
                        projection[n] = ("xx_start AS "
                                + Contract.DiaryActivityJoinableColumns.X_START_OF_LAST)
                        hasDiaryJoin = true
                    }
                    n++
                }
                if (hasDiaryJoin) {
                    n = 0
                    while (n < projection.size) {
                        if (Contract.DiaryActivityColumns._ID == projection[n]) {
                            projection[n] = (Contract.DiaryActivity.TABLE_NAME + "."
                                    + Contract.DiaryActivityColumns._ID)
                        }
                        n++
                    }
                    selection = selection.replace(
                        (" " + Contract.DiaryActivityColumns._ID).toRegex(),
                        " " + Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._ID
                    )
                    selection = selection.replace(
                        Contract.DiaryActivityColumns._DELETED.toRegex(),
                        Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._DELETED
                    )
                    tables = tables + ", " + Contract.Diary.TABLE_NAME
                    tables =
                        (tables + ", (SELECT xx_ref, " + Contract.DiaryColumns.START + " as xx_start FROM " + Contract.Diary.TABLE_NAME + ","
                                + "(SELECT " + Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._ID + " AS xx_ref,"
                                + " MAX(" + Contract.Diary.TABLE_NAME + "." + Contract.DiaryColumns.END + ") AS xx_ref_end"
                                + " FROM " + Contract.DiaryActivity.TABLE_NAME + ", " + Contract.Diary.TABLE_NAME
                                + " WHERE " + Contract.Diary.TABLE_NAME + "." + Contract.DiaryColumns.ACT_ID
                                + " = " + Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._ID
                                + " GROUP BY " + Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._ID
                                + ")"
                                + " WHERE " + Contract.Diary.TABLE_NAME + "." + Contract.DiaryColumns.END + " = xx_ref_end"
                                + ")")
                    selection =
                        (selection + " AND " + Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._ID + " = " + Contract.Diary.TABLE_NAME + "." + Contract.DiaryColumns.ACT_ID
                                + " AND " + Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._ID + " = xx_ref")
                    grouping = Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._ID
                }
                qBuilder.tables = tables
            }

            DIARY_IMAGE_ID, DIARY_IMAGE -> {
                qBuilder.tables = Contract.DiaryImage.TABLE_NAME
                if (TextUtils.isEmpty(sortOrder)) sortOrder = Contract.DiaryImage.SORT_ORDER_DEFAULT
            }

            DIARY_LOCATION_ID, DIARY_LOCATION -> {
                qBuilder.tables = Contract.DiaryLocation.TABLE_NAME
                if (TextUtils.isEmpty(sortOrder)) sortOrder = Contract.DiaryLocation.SORT_ORDER_DEFAULT
            }

            DIARY_ID, DIARY -> {
                /* rewrite projection, to prefix with tables */qBuilder.tables =
                    Contract.Diary.TABLE_NAME + " INNER JOIN " +
                            Contract.DiaryActivity.TABLE_NAME + " ON " +
                            Contract.Diary.TABLE_NAME + "." + Contract.DiaryColumns.ACT_ID + " = " +
                            Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._ID
                if (TextUtils.isEmpty(sortOrder)) sortOrder = Contract.Diary.SORT_ORDER_DEFAULT
            }

            DIARY_STATS -> {
                useRawQuery = true
                val l = uri.pathSegments
                val start: String
                val end: String
                if (l.size == 3) {
                    // we have a range query with start and end timestamps here
                    start = l[1]
                    end = l[2]
                } else {
                    start = "0"
                    end =
                        "6156000000000" // this is roughly 200 year since epoch, congratulations if this lasted so long...
                }

//                System.out.println(start + " " + end);
                var subselect =
                    ("SELECT SUM(MIN(IFNULL(" + Contract.DiaryColumns.END + ",strftime('%s','now') * 1000), " + end + ") - "
                            + "MAX(" + Contract.DiaryColumns.START + ", " + start + ")) from " + Contract.Diary.TABLE_NAME
                            + " WHERE ((start >= " + start + " AND start < " + end + ") OR (end > " + start + " AND end <= " + end + ") OR (start < " + start + " AND end > " + end + "))")
                if (selection.isNotEmpty()) {
                    subselect += " AND ($selection)"
                }
                sql =
                    ("SELECT " + Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityJoinableColumns.NAME + " as " + Contract.DiaryStats.NAME
                            + ", " + Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityJoinableColumns.COLOR + " as " + Contract.DiaryStats.COLOR
                            + ", SUM(MIN(IFNULL(" + Contract.DiaryColumns.END + ",strftime('%s','now') * 1000), " + end + ") - MAX(" + start + ", " + Contract.DiaryColumns.START + ")) as " + Contract.DiaryStats.DURATION
                            + ", (SUM(MIN(IFNULL(" + Contract.DiaryColumns.END + ",strftime('%s','now') * 1000), " + end + ") - MAX(" + start + ", " + Contract.DiaryColumns.START + ")) * 100.0 " +
                            "/ (" + subselect + ")) as " + Contract.DiaryStats.PORTION
                            + " FROM " + Contract.Diary.TABLE_NAME + ", " + Contract.DiaryActivity.TABLE_NAME
                            + " WHERE " + Contract.Diary.TABLE_NAME + "." + Contract.DiaryColumns.ACT_ID + " = " + Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._ID + " AND"
                            + " ((start >= " + start + " AND start < " + end + ") OR (end > " + start + " AND end <= " + end + ") OR (start < " + start + " AND end > " + end + "))")
                if (selection.isNotEmpty()) {
                    sql += " AND ($selection)"
                    val newArgs = Arrays.copyOf(selectionArgs!!, selectionArgs.size * 2)
                    System.arraycopy(selectionArgs, 0, newArgs, selectionArgs.size, selectionArgs.size)
                    selectionArgs = newArgs
                }
                sql += " GROUP BY " + Contract.DiaryActivity.TABLE_NAME + "." + Contract.DiaryActivityColumns._ID
                if (!sortOrder.isNullOrEmpty()) {
                    sql += " ORDER by $sortOrder"
                }
            }

            SEARCH_RECENT_SUGGESTION -> {
                sql = "SELECT " + Contract.DiarySearchSuggestion.SUGGESTION + ", " +
                        Contract.DiarySearchSuggestion.ACTION + " FROM " +
                        Contract.DiarySearchSuggestion.TABLE_NAME +
                        " ORDER BY " + Contract.DiarySearchSuggestion._ID + " DESC"
                c = mOpenHelper!!.readableDatabase.rawQuery(sql, selectionArgs)
                if (c != null && c.moveToFirst()) {
                    do {
                        var icon: Any? = null
                        val action = c.getString(1)
                        var q = c.getString(0) // what do we want to display
                        when (action) {
                            SEARCH_ACTIVITY -> {
                                /* icon stays null */
                                val i = q.toInt()
                                q = ActivityHelper.helper.activityWithId(i)!!.mName
                            }

                            SEARCH_NOTE -> {
                                q = context!!.resources.getString(R.string.search_notes, q)
                                icon = R.drawable.ic_search
                            }

                            SEARCH_GLOBAL, Intent.ACTION_SEARCH -> {
                                q = context!!.resources.getString(R.string.search_diary, q)
                                icon = R.drawable.ic_search
                            }

                            SEARCH_DATE -> {
                                q = context!!.resources.getString(R.string.search_date, q)
                                icon = R.drawable.ic_calendar
                            }
                        }
                        result.addRow(
                            arrayOf(
                                id++,
                                q,  /* icon */
                                icon,  /* intent action */
                                action,  /* intent data */
                                Uri.withAppendedPath(SEARCH_URI, c.getString(0)),  /* rewrite query */
                                c.getString(0)
                            )
                        )
                    } while (c.moveToNext())
                }
                c.close()
                return result
            }

            SEARCH_SUGGESTION -> {
                val query = uri.lastPathSegment //.toLowerCase();
                if (!query.isNullOrEmpty()) {
                    // ACTIVITIES matching the current search
                    val filtered = sortedActivities(query)

                    // TODO: make the amount of ACTIVITIES shown configurable
                    var i = 0
                    while (i < 3) {
                        if (i < filtered.size) {
                            result.addRow(
                                arrayOf<Any?>(
                                    id++,
                                    filtered[i].mName,  /* icon */
                                    null,  /* intent action */
                                    SEARCH_ACTIVITY,  /* intent data */
                                    Uri.withAppendedPath(
                                        SEARCH_URI, filtered[i].mId.toString()
                                    ),  /* rewrite query */
                                    filtered[i].mName
                                )
                            )
                        }
                        i++
                    }
                    // Notes
                    result.addRow(
                        arrayOf<Any>(
                            id++,
                            context!!.resources.getString(R.string.search_notes, query),  /* icon */
                            R.drawable.ic_search,  /* intent action */
                            SEARCH_NOTE,  /* intent data */
                            Uri.withAppendedPath(SEARCH_URI, query),  /* rewrite query */
                            query
                        )
                    )

                    // Global search
                    result.addRow(
                        arrayOf<Any>(
                            id++,
                            context!!.resources.getString(R.string.search_diary, query),  /* icon */
                            R.drawable.ic_search,  /* intent action */
                            SEARCH_GLOBAL,  /* intent data */
                            Uri.withAppendedPath(SEARCH_URI, query),  /* rewrite query */
                            query
                        )
                    )

                    // Date
                    result.addRow(
                        arrayOf<Any>(
                            id++,
                            context!!.resources.getString(R.string.search_date, query),  /* icon */
                            R.drawable.ic_calendar,  /* intent action */
                            SEARCH_DATE,  /* intent data */
                            Uri.withAppendedPath(SEARCH_URI, query),  /* rewrite query */
                            query
                        )
                    )

                    // has Pictures
                    // TODO: add picture search

                    // Location (GPS)
                    // TODO: add location search
                }
                return result
            }

            CONDITIONS_ID, CONDITIONS -> {}
            else -> {}
        }
        c = if (useRawQuery) {
            mOpenHelper!!.readableDatabase.rawQuery(sql, selectionArgs)
        } else {
            qBuilder.query(
                mOpenHelper!!.readableDatabase,
                projection,
                selection,
                selectionArgs,
                grouping,
                null,
                sortOrder
            )
        }
        c.setNotificationUri(context!!.contentResolver, uri)
        return c
    }

    override fun getType(uri: Uri): String {
        return when (sUriMatcher.match(uri)) {
            ACTIVITIES -> Contract.DiaryActivity.CONTENT_TYPE
            ACTIVITIES_ID -> Contract.DiaryActivity.CONTENT_ITEM_TYPE
            DIARY -> Contract.Diary.CONTENT_TYPE
            DIARY_ID -> Contract.Diary.CONTENT_ITEM_TYPE
            DIARY_LOCATION -> Contract.DiaryLocation.CONTENT_TYPE
            DIARY_LOCATION_ID -> Contract.DiaryLocation.CONTENT_ITEM_TYPE
            DIARY_STATS -> Contract.DiaryStats.CONTENT_TYPE
            else -> {
                Log.e(TAG, "MIME type for $uri not defined.")
                ""
            }
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        val table: String
        var resultUri: Uri?
        when (sUriMatcher.match(uri)) {
            ACTIVITIES -> {
                table = Contract.DiaryActivity.TABLE_NAME
                resultUri = Contract.DiaryActivity.CONTENT_URI
            }

            DIARY -> {
                table = Contract.Diary.TABLE_NAME
                resultUri = Contract.Diary.CONTENT_URI
            }

            DIARY_IMAGE -> {
                table = Contract.DiaryImage.TABLE_NAME
                resultUri = Contract.DiaryImage.CONTENT_URI
            }

            DIARY_LOCATION -> {
                table = Contract.DiaryLocation.TABLE_NAME
                resultUri = Contract.DiaryLocation.CONTENT_URI
            }

            DIARY_SUGGESTION -> {
                table = Contract.DiarySearchSuggestion.TABLE_NAME
                resultUri = Contract.DiarySearchSuggestion.CONTENT_URI
            }

            CONDITIONS, DIARY_STATS -> throw IllegalArgumentException(
                "Unsupported URI for insertion: $uri"
            )

            else -> throw IllegalArgumentException(
                "Unsupported URI for insertion: $uri"
            )
        }
        val db = mOpenHelper!!.writableDatabase
        db.beginTransaction()
        val id = db.insertOrThrow(
            table,
            null,
            values
        )
        db.setTransactionSuccessful()
        db.endTransaction()
        return if (id > 0) {
            resultUri = ContentUris.withAppendedId(resultUri, id)
            context!!.contentResolver.notifyChange(resultUri, null)
            resultUri
        } else {
            throw SQLException(
                "Problem while inserting into uri: " + uri + " values " + values.toString()
            )
        }
    }

    /**
     * Implement this to handle requests to delete one or more rows.
     * The implementation should apply the selection clause when performing
     * deletion, allowing the operation to affect multiple rows in a directory.
     * This method can be called from multiple threads, as described in
     * [Processes
 * and Threads]({@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads).
     *
     *
     *
     * The implementation is responsible for parsing out a row ID at the end
     * of the URI, if a specific row is being deleted. That is, the client would
     * pass in `content://contacts/people/22` and the implementation is
     * responsible for parsing the record number (22) when creating a SQL statement.
     *
     * @param uri           The full URI to query, including a row ID (if a specific record is requested).
     * @param selection     An optional restriction to apply to rows when deleting.
     * @param selectionArgs
     * @return The number of rows affected.
     * @throws SQLException
     */
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        var selection = selection
        var isGlobalDelete = false
        val table: String
        val values = ContentValues()
        when (sUriMatcher.match(uri)) {
            ACTIVITIES_ID -> table = Contract.DiaryActivity.TABLE_NAME
            DIARY -> {
                isGlobalDelete = true
                table = Contract.Diary.TABLE_NAME
            }

            DIARY_ID -> table = Contract.Diary.TABLE_NAME
            DIARY_IMAGE -> {
                isGlobalDelete = true
                table = Contract.DiaryImage.TABLE_NAME
            }

            DIARY_IMAGE_ID -> table = Contract.DiaryImage.TABLE_NAME
            DIARY_LOCATION -> {
                isGlobalDelete = true
                table = Contract.DiaryLocation.TABLE_NAME
            }

            DIARY_LOCATION_ID -> table = Contract.DiaryLocation.TABLE_NAME
            DIARY_SUGGESTION -> {
                table = Contract.DiarySearchSuggestion.TABLE_NAME
                val db = mOpenHelper!!.writableDatabase
                return db.delete(table, selection, selectionArgs)
            }

            CONDITIONS_ID, DIARY_STATS -> throw IllegalArgumentException(
                "Unsupported URI for deletion: $uri"
            )

            else -> throw IllegalArgumentException(
                "Unsupported URI for deletion: $uri"
            )
        }
        val db = mOpenHelper!!.writableDatabase
        if (!isGlobalDelete) {
            selection = if (selection != null) {
                "$selection AND "
            } else {
                ""
            }
            selection = selection + "_id=" + uri.lastPathSegment
        }
        values.put(Contract.DiaryActivityColumns._DELETED, "1")
        db.beginTransaction()
        val upds = db.update(
            table,
            values,
            selection,
            selectionArgs
        )
        if (upds > 0) {
            context!!.contentResolver.notifyChange(uri, null)
        } else {
            Log.i(TAG, "Could not delete anything for uri: $uri with selection '$selection'")
        }
        db.setTransactionSuccessful()
        db.endTransaction()
        return upds
    }

    /**
     * Implement this to handle requests to update one or more rows.
     * The implementation should update all rows matching the selection
     * to set the columns according to the provided values map.
     * This method can be called from multiple threads, as described in
     * [Processes
 * and Threads]({@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads).
     *
     * @param uri           The URI to query. This can potentially have a record ID if this
     * is an update request for a specific record.
     * @param values        A set of column_name/value pairs to update in the database.
     * This must not be `null`.
     * @param selection     An optional filter to match rows to update.
     * @param selectionArgs
     * @return the number of rows affected.
     */
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        var selection = selection
        val table: String
        var isID = false
        when (sUriMatcher.match(uri)) {
            ACTIVITIES_ID -> {
                isID = true
                table = Contract.DiaryActivity.TABLE_NAME
            }

            DIARY_ID -> {
                isID = true
                table = Contract.Diary.TABLE_NAME
            }

            DIARY -> table = Contract.Diary.TABLE_NAME
            DIARY_IMAGE -> table = Contract.DiaryImage.TABLE_NAME
            DIARY_LOCATION_ID -> {
                isID = true
                table = Contract.DiaryLocation.TABLE_NAME
            }

            DIARY_LOCATION -> table = Contract.DiaryLocation.TABLE_NAME
            CONDITIONS_ID, DIARY_STATS -> throw IllegalArgumentException(
                "Unsupported URI for update: $uri"
            )

            else -> throw IllegalArgumentException(
                "Unsupported URI for update: $uri"
            )
        }
        val db = mOpenHelper!!.writableDatabase
        if (isID) {
            selection = if (selection != null) {
                "$selection AND "
            } else {
                ""
            }
            selection = selection + "_id=" + uri.lastPathSegment
        }
        val upds = db.update(
            table,
            values,
            selection,
            selectionArgs
        )
        if (upds > 0) {
            context!!.contentResolver.notifyChange(uri, null)
        } else if (isID) {
            throw SQLException(
                "Problem while updating uri: $uri with selection '$selection'"
            )
        }
        return upds
    }

    fun resetDatabase() {
        mOpenHelper!!.close()
    }

    /**
     * Search for all dates in database which match start/end date or are in range (between start and end date)
     * @param dateInMillis - date is searched
     * @return query (string) with ids that fulfills defined CONDITIONS
     */
    @SuppressLint("Range")
    fun searchDate(dateInMillis: Long): String {
        // TODO: move this into the method query, for the case DIARY,
        // similar to DIARY_STATS, we can modify selection and selection args there
        // or maybe better, invent a new URI like "DIARY/number" where number is the dateInMillis
        // Alternative: move all this directly into HistoryActivity.onCreateLoader
        var querySelection = " "
        var id: String
        val searchedValuePlusDay =
            dateInMillis + 86400000 // TODO: replace magic numbers by the formula to calculate them...
        val searchSpecialCase = dateInMillis + 86399999 //used for searching for still running activity
        try {
            mOpenHelper!!.readableDatabase.rawQuery(
                "SELECT " + Contract.DiaryColumns._ID
                        + " FROM " + Contract.Diary.TABLE_NAME
                        + " WHERE " + "(" + dateInMillis + " >= " + Contract.DiaryColumns.START + " AND " + dateInMillis + " <= " + Contract.DiaryColumns.END + ")" + " OR " +
                        "(" + searchedValuePlusDay + " >= " + Contract.DiaryColumns.START + " AND " + searchedValuePlusDay + " <= " + Contract.DiaryColumns.END + ")" + " OR " +
                        "(" + dateInMillis + " < " + Contract.DiaryColumns.START + " AND " + searchedValuePlusDay + " > " + Contract.DiaryColumns.END + ")" + " OR " +
                        "(" + searchSpecialCase + " >= " + Contract.DiaryColumns.START + " AND " + Contract.DiaryColumns.END + " IS NULL" + ")",
                null
            ).use { allRowsStart ->
// TODO: -> this query should not be executed outside of the method LDContentProvider.query
                if (allRowsStart.moveToFirst()) {
                    do {
                        for (name in allRowsStart.columnNames) {
                            id = allRowsStart.getString(allRowsStart.getColumnIndex(name))
                            querySelection += if (querySelection == " ") Contract.Diary.TABLE_NAME + "." + Contract.DiaryColumns._ID + " =" + id else " OR " + Contract.Diary.TABLE_NAME + "." + Contract.DiaryColumns._ID + " =" + id
                        }
                    } while (allRowsStart.moveToNext())
                }
            }
        } catch (e: Exception) {
            // TODO: add proper exception handling. Also "Exception" seems quite generic -> catch all exceptions that can occur directly
        }

        // if there is no matching dates it returns query which links to find nothings
        // otherwise it will return query with IDs of matching dates
        return if (querySelection == " ") " start=null" else querySelection
    }

    companion object {
        private const val ACTIVITIES = 1
        private const val ACTIVITIES_ID = 2
        private const val CONDITIONS = 3
        private const val CONDITIONS_ID = 4
        private const val DIARY = 5
        private const val DIARY_ID = 6
        private const val DIARY_IMAGE = 7
        private const val DIARY_IMAGE_ID = 8
        private const val DIARY_LOCATION = 9
        private const val DIARY_LOCATION_ID = 10
        private const val DIARY_STATS = 11
        private const val SEARCH_RECENT_SUGGESTION = 12
        private const val SEARCH_SUGGESTION = 13
        private const val DIARY_SUGGESTION = 14
        private val TAG = LDContentProvider::class.java.name
        const val SEARCH_ACTIVITY = "com.mdiqentw.lifedots.action.SEARCH_ACTIVITY"
        const val SEARCH_NOTE = "com.mdiqentw.lifedots.action.SEARCH_NOTE"
        const val SEARCH_GLOBAL = "com.mdiqentw.lifedots.action.SEARCH_GLOBAL"
        const val SEARCH_DATE = "com.mdiqentw.lifedots.action.SEARCH_DATE"

        // TODO: isn't this already somewhere else?
        val SEARCH_URI = Uri.parse("content://" + Contract.AUTHORITY)
        private val sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        private val plusPattern = Pattern.compile("^/+")

        init {
            sUriMatcher.addURI(
                Contract.AUTHORITY,
                plusPattern.matcher(Contract.DiaryActivity.CONTENT_URI.path!!).replaceAll(""),
                ACTIVITIES
            )
            sUriMatcher.addURI(
                Contract.AUTHORITY,
                plusPattern.matcher(Contract.DiaryActivity.CONTENT_URI.path!!).replaceAll("") + "/#",
                ACTIVITIES_ID
            )
            sUriMatcher.addURI(
                Contract.AUTHORITY,
                plusPattern.matcher(Contract.Diary.CONTENT_URI.path!!).replaceAll(""),
                DIARY
            )
            sUriMatcher.addURI(
                Contract.AUTHORITY,
                plusPattern.matcher(Contract.Diary.CONTENT_URI.path!!).replaceAll("") + "/#",
                DIARY_ID
            )
            sUriMatcher.addURI(
                Contract.AUTHORITY,
                plusPattern.matcher(Contract.DiaryImage.CONTENT_URI.path!!).replaceAll(""),
                DIARY_IMAGE
            )
            sUriMatcher.addURI(
                Contract.AUTHORITY,
                plusPattern.matcher(Contract.DiaryImage.CONTENT_URI.path!!).replaceAll("") + "/#",
                DIARY_IMAGE_ID
            )
            sUriMatcher.addURI(
                Contract.AUTHORITY,
                plusPattern.matcher(Contract.DiaryStats.CONTENT_URI.path!!).replaceAll(""),
                DIARY_STATS
            )
            sUriMatcher.addURI(
                Contract.AUTHORITY,
                plusPattern.matcher(Contract.DiaryStats.CONTENT_URI.path!!).replaceAll("") + "/#/#",
                DIARY_STATS
            )
            sUriMatcher.addURI(
                Contract.AUTHORITY,
                plusPattern.matcher(Contract.DiaryLocation.CONTENT_URI.path!!).replaceAll(""),
                DIARY_LOCATION
            )
            sUriMatcher.addURI(
                Contract.AUTHORITY,
                plusPattern.matcher(Contract.DiaryLocation.CONTENT_URI.path!!).replaceAll("") + "/#",
                DIARY_LOCATION_ID
            )
            sUriMatcher.addURI(
                Contract.AUTHORITY,
                plusPattern.matcher(Contract.DiaryLocation.CONTENT_URI.path!!).replaceAll(""),
                DIARY_LOCATION
            )
            sUriMatcher.addURI(
                Contract.AUTHORITY,
                plusPattern.matcher(Contract.DiaryLocation.CONTENT_URI.path!!).replaceAll("") + "/#",
                DIARY_LOCATION_ID
            )
            // TODO:
            sUriMatcher.addURI(
                Contract.AUTHORITY,
                "history/" + SearchManager.SUGGEST_URI_PATH_QUERY + "/",
                SEARCH_RECENT_SUGGESTION
            )
            sUriMatcher.addURI(
                Contract.AUTHORITY,
                "history/" + SearchManager.SUGGEST_URI_PATH_QUERY + "/*",
                SEARCH_SUGGESTION
            )
            sUriMatcher.addURI(
                Contract.AUTHORITY,
                plusPattern.matcher(Contract.DiarySearchSuggestion.CONTENT_URI.path!!).replaceAll(""),
                DIARY_SUGGESTION
            )

            /* TODO #18 */sUriMatcher.addURI(Contract.AUTHORITY, "CONDITIONS", CONDITIONS)
            sUriMatcher.addURI(Contract.AUTHORITY, "CONDITIONS/#", CONDITIONS_ID)
        }
    }
}