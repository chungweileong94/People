package com.lcw.people.Helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.ContactsContract;

import java.util.ArrayList;

public class FavoritesSQLiteHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "FavoritesDB";
    public static final String FAVORITES_TABLE = "favorites";

    public FavoritesSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_BOOK_TABLE = "CREATE TABLE " + FAVORITES_TABLE + " ( " +
                ContactsContract.Contacts.LOOKUP_KEY + " TEXT PRIMARY KEY )";
        db.execSQL(CREATE_BOOK_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + FAVORITES_TABLE);
        this.onCreate(db);
    }

    public void addContact(String lookup_key) {
        SQLiteDatabase database = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(ContactsContract.Contacts.LOOKUP_KEY, lookup_key);

        database.insert(FAVORITES_TABLE, null, values);
        database.close();
    }

    public boolean isContactExist(String lookup_key) {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.rawQuery("SELECT * FROM " + FAVORITES_TABLE +
                " WHERE " + ContactsContract.Contacts.LOOKUP_KEY + " = '" + lookup_key + "'", null);

        int count = cursor.getCount();
        database.close();

        return count == 0 ? false : true;
    }

    public ArrayList<String> getAllContacts() {
        ArrayList<String> contacts = new ArrayList<>();

        SQLiteDatabase database = getReadableDatabase();

        Cursor cursor = database.rawQuery("SELECT * FROM " + FAVORITES_TABLE, null);

        while (cursor.moveToNext()) {
            contacts.add(cursor.getString(0));
        }

        database.close();

        return contacts;
    }

    public void removeContact(String lookup_key) {
        SQLiteDatabase database = getWritableDatabase();
        database.delete(FAVORITES_TABLE, ContactsContract.Contacts.LOOKUP_KEY + "= ?", new String[]{lookup_key});
        database.close();
    }
}
