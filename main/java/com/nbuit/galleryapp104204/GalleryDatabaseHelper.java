package com.nbuit.galleryapp104204;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class GalleryDatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "gallery_showcase.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_IMAGES = "images";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_URI = "uri";
    private static final String COLUMN_NAME = "name";

    public GalleryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_IMAGES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_URI + " TEXT, " +
                COLUMN_NAME + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGES);
        onCreate(db);
    }

    // Insert new image metadata into the database
    public void insertImageMetadata(String uri, String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_URI, uri);
        values.put(COLUMN_NAME, name);
        db.insert(TABLE_IMAGES, null, values);
        db.close();
    }

    public void deleteImageMetadata(String uri) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_IMAGES, COLUMN_URI + " = ?", new String[]{uri});
        db.close();
    }

    public Cursor getAllImages() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_IMAGES, null);
    }
}
