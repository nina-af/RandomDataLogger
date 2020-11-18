/*
 * The SQLLiteOpenhelper class is something that was provided by Google to make working with
 * databases easier. This is where you implement methods that create and set up the initial
 * database. After you implement these methods, all you have to do is instantiate an instance of
 * your helper class, and then call helperClassInstance.getWriteableDatabase() (or
 * getReadableDataBase()) and then your helper class automatically takes care of creating a new
 * database if necessary, or returning the one that already exists, etc.
 *
 * https://stackoverflow.com/questions/17451931/how-to-use-a-contract-class-in-android
 */

package com.example.randomdatalogger;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class DatabaseHelper extends SQLiteOpenHelper {

    private final String TAG = "DatabaseHelper";

    // Make database instance a singleton instance across the entire application's lifecycle.
    private static DatabaseHelper sInstance;

    // Constructor should be private to prevent direct instantiation; make a call to the
    // static method "getInstance()" instead.
    private DatabaseHelper(Context context) {
        super(context, DatabaseContract.DATABASE_NAME, null,
                DatabaseContract.DATABASE_VERSION);
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        // Use the application context, which will ensure that you don't accidentally leak an
        // Activity's context.
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    // Method is called during creation of the database.
    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DatabaseContract.Table1.SQL_CREATE_ENTRIES);
    }

    // Method is called during upgrade of the database.
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is to
        // simply discard the data and start over.
        database.execSQL(DatabaseContract.Table1.SQL_DELETE_ENTRIES);
        onCreate(database);
    }

    // Method is called during downgrade of the database.
    @Override
    public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        onUpgrade(database, oldVersion, newVersion);
    }

    /**
     * Helper function that parses a given table into a string
     * and returns it for easy printing. The string consists of
     * the table name and then each row is iterated through with
     * column_name: value pairs printed out.
     *
     * @param database the database to get the table from
     * @param tableName the the name of the table to parse
     * @return the table tableName as a string
     *
     * https://stackoverflow.com/questions/27003486/printing-all-rows-of-a-sqlite-database-in-android
     */

    public String getTableAsString(SQLiteDatabase database, String tableName) {
        Log.d(TAG, "getTableAsString called");
        String tableString = String.format("Table %s:\n", tableName);

        Cursor allRows = database.rawQuery("SELECT * FROM " + tableName, null);

        if (allRows.moveToFirst()) {
            String[] columnNames = allRows.getColumnNames();
            do {
                for (String name : columnNames) {
                    tableString += String.format("%s: %s\n", name,
                            allRows.getString(allRows.getColumnIndex(name)));
                }
                tableString += "\n";
            } while (allRows.moveToNext());
        }
        return tableString;
    }

    // Delete the database. OnCreate() needs to be triggered to recreate the table, which requires
    // reinstalling the app; use clearTable() to clear the table rows instead.
    public void deleteAll()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(DatabaseContract.Table1.SQL_DELETE_ENTRIES);
        db.close();
    }

    // Delete all rows from the table.
    public void clearTable()   {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(DatabaseContract.Table1.TABLE_NAME, null,null);
        db.execSQL(DatabaseContract.Table1.SQL_RESET_AUTOINCREMENT);
    }
}
