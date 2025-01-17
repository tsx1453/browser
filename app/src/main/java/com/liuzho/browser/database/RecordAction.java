package com.liuzho.browser.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.liuzho.browser.Browser;
import com.liuzho.browser.storage.BrowserPref;
import com.liuzho.browser.unit.RecordUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class RecordAction {
    public static final int HISTORY_ITEM = 0;
    public static final int STARTSITE_ITEM = 1;
    public static final int BOOKMARK_ITEM = 2;

    private SQLiteDatabase database;
    private final RecordHelper helper;

    public RecordAction(Context context) {
        this.helper = new RecordHelper(context);
    }

    public void open(boolean rw) {
        database = rw ? helper.getWritableDatabase() : helper.getReadableDatabase();
    }

    public void close() {
        helper.close();
    }

    //StartSite

    public boolean addStartSite(Record record) {
        if (record == null
                || record.getTitle() == null
                || record.getTitle().trim().isEmpty()
                || record.getURL() == null
                || record.getURL().trim().isEmpty()
                || record.getDesktopMode() == null
                || record.getNightMode() == null
                || record.getTime() < 0L
                || record.getOrdinal() < 0) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put(RecordUnit.COLUMN_TITLE, record.getTitle().trim());
        values.put(RecordUnit.COLUMN_URL, record.getURL().trim());

        // Bookmark time is used for color, desktop mode, javascript, and List_standard content
        // bit 0..3  icon color
        // bit 4: 1 = Desktop Mode
        // bit 5: 0 = NightMode (0 due backward compatibility)
        // bit 6: 0 = List_standard Content allowed (0 due to backward compatibility)

        values.put(RecordUnit.COLUMN_FILENAME, (long) (record.getDesktopMode() ? 16 : 0) + (long) (record.getNightMode() ? 32 : 0));
        values.put(RecordUnit.COLUMN_ORDINAL, record.getOrdinal());
        database.insert(RecordUnit.TABLE_START, null, values);
        return true;
    }

    public List<Record> listStartSite() {

        List<Record> list = new LinkedList<>();
        BrowserPref pref = BrowserPref.getInstance();
        String sortBy = pref.getSortStartSite();

        Cursor cursor;
        cursor = database.query(
                RecordUnit.TABLE_START,
                new String[]{
                        RecordUnit.COLUMN_TITLE,
                        RecordUnit.COLUMN_URL,
                        RecordUnit.COLUMN_FILENAME,
                        RecordUnit.COLUMN_ORDINAL
                },
                null,
                null,
                null,
                null,
                sortBy + " COLLATE NOCASE;"
        );
        if (cursor == null) {
            return list;
        }
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            list.add(getRecord(cursor, STARTSITE_ITEM));
            cursor.moveToNext();
        }
        cursor.close();
        if (!sortBy.equals("ordinal")) {
            Collections.reverse(list);
        }
        return list;
    }

    //BOOKMARK

    public void addBookmark(Record record) {
        if (record == null
                || record.getTitle() == null
                || record.getTitle().trim().isEmpty()
                || record.getURL() == null
                || record.getURL().trim().isEmpty()
                || record.getDesktopMode() == null
                || record.getNightMode() == null
                || record.getTime() < 0L) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(RecordUnit.COLUMN_TITLE, record.getTitle().trim());
        values.put(RecordUnit.COLUMN_URL, record.getURL().trim());

        // Bookmark time is used for color, desktop mode, javascript, and List_standard content
        // bit 0..3  icon color
        // bit 4: 1 = Desktop Mode
        // bit 5: 0 = NightMode (0 due backward compatibility)
        // bit 6: 0 = List_standard Content allowed (0 due to backward compatibility)

        values.put(RecordUnit.COLUMN_TIME, record.getIconColor() + (long) (record.getDesktopMode() ? 16 : 0) + (long) (record.getNightMode() ? 32 : 0));
        database.insert(RecordUnit.TABLE_BOOKMARK, null, values);
    }

    public List<Record> listBookmark(Context context, boolean filter, long filterBy) {

        List<Record> list = new LinkedList<>();
        // 可以在这里修改书签sort
        String sortBy = RecordUnit.COLUMN_TITLE;
        Cursor cursor;
        cursor = database.query(
                RecordUnit.TABLE_BOOKMARK,
                new String[]{
                        RecordUnit.COLUMN_TITLE,
                        RecordUnit.COLUMN_URL,
                        RecordUnit.COLUMN_TIME
                },
                null,
                null,
                null,
                null,
                sortBy + " COLLATE NOCASE;"
        );
        if (cursor == null) {
            return list;
        }
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            if (filter) {
                Record record = getRecord(cursor, BOOKMARK_ITEM);
                if (record.getIconColor() == filterBy) {
                    list.add(record);
                }
            } else {
                list.add(getRecord(cursor, BOOKMARK_ITEM));
            }
            cursor.moveToNext();
        }
        cursor.close();

        // noinspection ConstantConditions, maybe later use 'time'
        if (sortBy.equals(RecordUnit.COLUMN_TIME)) {  //ignore desktop mode, JavaScript, and remote content when sorting colors
            Collections.sort(list, (o1, o2) -> o1.getTitle().compareTo(o2.getTitle()));
            Collections.sort(list, (o1, o2) -> Long.compare(o1.getIconColor(), o2.getIconColor()));
        }
        Collections.reverse(list);
        return list;
    }

    //History

    public void addHistory(Record record) {
        if (record == null
                || record.getTitle() == null
                || record.getTitle().trim().isEmpty()
                || record.getURL() == null
                || record.getURL().trim().isEmpty()
                || record.getTime() < 0L) {
            return;
        }
        record.setTime(record.getTime() & (~255));    //blank out lower 8bits of time

        ContentValues values = new ContentValues();
        values.put(RecordUnit.COLUMN_TITLE, record.getTitle().trim());
        values.put(RecordUnit.COLUMN_URL, record.getURL().trim());
        values.put(RecordUnit.COLUMN_TIME, record.getTime() + (long) (record.getDesktopMode() ? 16 : 0) + (long) (record.getNightMode() ? 32 : 0));
        database.insert(RecordUnit.TABLE_HISTORY, null, values);
    }

    public List<Record> listHistory() {
        List<Record> list = new ArrayList<>();
        Cursor cursor;
        cursor = database.query(
                RecordUnit.TABLE_HISTORY,
                new String[]{
                        RecordUnit.COLUMN_TITLE,
                        RecordUnit.COLUMN_URL,
                        RecordUnit.COLUMN_TIME
                },
                null,
                null,
                null,
                null,
                RecordUnit.COLUMN_TIME + " COLLATE NOCASE;"
        );

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            list.add(getRecord(cursor, HISTORY_ITEM));
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }


    // General

    public void addDomain(String domain, String table) {
        if (domain == null || domain.trim().isEmpty()) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(RecordUnit.COLUMN_DOMAIN, domain.trim());
        database.insert(table, null, values);
    }

    public boolean checkDomain(String domain, String table) {
        if (domain == null || domain.trim().isEmpty()) {
            return false;
        }
        Cursor cursor = database.query(
                table,
                new String[]{RecordUnit.COLUMN_DOMAIN},
                RecordUnit.COLUMN_DOMAIN + "=?",
                new String[]{domain.trim()},
                null,
                null,
                null
        );
        if (cursor != null) {
            boolean result = cursor.moveToFirst();
            cursor.close();
            return result;
        }
        return false;
    }

    public void deleteDomain(String domain, String table) {
        if (domain == null || domain.trim().isEmpty()) {
            return;
        }
        database.execSQL("DELETE FROM " + table + " WHERE " + RecordUnit.COLUMN_DOMAIN + " = " + "\"" + domain.trim() + "\"");
    }

    public List<String> listDomains(String table) {
        List<String> list = new ArrayList<>();
        Cursor cursor = database.query(
                table,
                new String[]{RecordUnit.COLUMN_DOMAIN},
                null,
                null,
                null,
                null,
                RecordUnit.COLUMN_DOMAIN
        );
        if (cursor == null) {
            return list;
        }
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            list.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }

    public boolean checkUrl(String url, String table) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        Cursor cursor = database.query(
                table,
                new String[]{RecordUnit.COLUMN_URL},
                RecordUnit.COLUMN_URL + "=?",
                new String[]{url.trim()},
                null,
                null,
                null
        );
        if (cursor != null) {
            boolean result = cursor.moveToFirst();
            cursor.close();

            return result;
        }
        return false;
    }

    public void deleteURL(String domain, String table) {
        if (domain == null || domain.trim().isEmpty()) {
            return;
        }
        database.execSQL("DELETE FROM " + table + " WHERE " + RecordUnit.COLUMN_URL + " = " + "\"" + domain.trim() + "\"");
    }

    public void clearTable(String table) {
        database.execSQL("DELETE FROM " + table);
    }

    private Record getRecord(Cursor cursor, int type) {
        Record record = new Record();
        record.setTitle(cursor.getString(0));
        record.setURL(cursor.getString(1));
        record.setTime(cursor.getLong(2));
        record.setType(type);

        if ((type == STARTSITE_ITEM) || (type == BOOKMARK_ITEM)) {
            record.setDesktopMode((record.getTime() & 16) == 16);
            record.setNightMode((record.getTime() & 32) == 32);
            if (type == BOOKMARK_ITEM) {
                record.setIconColor(record.getTime() & 15);
            }
            record.setTime(0);  //time is no longer needed after extracting data
        } else if (type == HISTORY_ITEM) {
            record.setDesktopMode((record.getTime() & 16) == 16);
            record.setNightMode((record.getTime() & 32) == 32);
            record.setTime(record.getTime() & (~255));  //blank out lower 8bits of time
        }
        return record;
    }

    public List<Record> listEntries() {
        Context context = Browser.getConfig().context;
        List<Record> list = new ArrayList<>();
        RecordAction action = new RecordAction(context);
        action.open(false);
        list.addAll(action.listBookmark(context, false, 0)); //move bookmarks to top of list
        list.addAll(action.listStartSite());
        list.addAll(action.listHistory());
        action.close();
        return list;
    }
}