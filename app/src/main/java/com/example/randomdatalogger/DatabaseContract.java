/*
 * Contract class: database schema (defines how the database is set up. Everything in the contract
 * class is declared as static, because you will never be instantiating a Contract class, but only
 * referring to the constants defined in it.
 *
 * Suppose you want to change the name of one of your columns. Rather than making changes to
 * multiple files, only the value for the column in the contract class needs to be changed. No
 * computation work takes place inside of the contract class.
 *
 * https://stackoverflow.com/questions/17451931/how-to-use-a-contract-class-in-android
 */

package com.example.randomdatalogger;

import android.provider.BaseColumns;

public class DatabaseContract {

    // Database information.
    public static final String DATABASE_NAME = "SIGNAL_STRENGTH.db";
    public static final int DATABASE_VERSION = 1;

    // To prevent someone from accidentally instantiating the contract class, make the constructor
    // private.
    private DatabaseContract() {}

    // Inner class that defines the table contents.
    public static abstract class Table1 implements BaseColumns {
        /* BaseColumns: interface which adds two fields:
         *     _ID = "_id"        (unique ID for a row)
         *     _COUNT = "_count"  (number of rows in a directory)
         */

        // Table name.
        public static final String TABLE_NAME = "SIGNAL";

        // Table columns.
        public static final String COLUMN_TIME = "time";
        public static final String COLUMN_STRENGTH = "strength";

        // Creating table query.
        public static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_TIME + " INTEGER, " +
                        COLUMN_STRENGTH + " INTEGER) ;";  // May need " INTEGER);"; ???

        // Deleting table query.
        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;

        // Resetting _id to 0 after deleting rows.
        public static final String SQL_RESET_AUTOINCREMENT = "UPDATE SQLITE_SEQUENCE SET SEQ = 0 " +
                "WHERE NAME = '" + TABLE_NAME + "';";
    }
}

