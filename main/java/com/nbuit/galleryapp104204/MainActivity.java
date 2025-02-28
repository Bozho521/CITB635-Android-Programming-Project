package com.nbuit.galleryapp104204;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GalleryUpdateListener {

    private static final int REQUEST_MEDIA_PERMISSION = 101;
    private static final int REQUEST_CODE_PICK_IMAGES = 102;
    private static final int IMAGE_SELECTION_LIMIT = 10;

    private RecyclerView recyclerViewGallery;
    private GalleriesAdapter galleriesAdapter;
    private Button buttonChangeGrid;
    private Button buttonInspirations;
    private int currentSpanCount = 3;
    private GalleryDatabaseHelper databaseHelper;
    private List<Uri> selectedImages = new ArrayList<>();

    private final BroadcastReceiver galleryUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onGalleryUpdated();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerViewGallery = findViewById(R.id.recyclerViewGallery);
        buttonChangeGrid = findViewById(R.id.buttonChangeGrid);
        buttonInspirations = findViewById(R.id.buttonInspirations);

        databaseHelper = new GalleryDatabaseHelper(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            openPhotoPicker();
        } else {
            requestMediaPermission();
        }

        buttonChangeGrid.setOnClickListener(v -> changeGridPattern());

        buttonInspirations.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, InspirationsActivity.class);
            startActivity(intent);
        });

        registerReceiver(galleryUpdateReceiver, new IntentFilter("ACTION_UPDATE_GALLERY"));
    }

    @Override
    public void onGalleryUpdated() {
        loadImages();
    }

    private void requestMediaPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_MEDIA_PERMISSION);
            } else {
                loadImages();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_MEDIA_PERMISSION);
            } else {
                loadImages();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private void openPhotoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGES);
    }

    private boolean isDatabaseEmpty() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + GalleryDatabaseHelper.TABLE_IMAGES;
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count == 0;
    }

    private void resetDatabase() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete(GalleryDatabaseHelper.TABLE_IMAGES, null, null);
        db.execSQL("DELETE FROM sqlite_sequence WHERE name='" + GalleryDatabaseHelper.TABLE_IMAGES + "'");
        db.close();
    }

    private void loadImages() {
        List<Uri> photoUris = new ArrayList<>();
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME};
        Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null
        );

        if (isDatabaseEmpty()) {
            resetDatabase();
        }

        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                Uri contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Integer.toString(id));

                databaseHelper.insertImageMetadata(contentUri.toString(), name);
                photoUris.add(contentUri);
            }
            cursor.close();
        }

        galleriesAdapter = new GalleriesAdapter(
                this,
                photoUris,
                this::onPhotoClick,
                null,
                GalleriesAdapter.IMAGE_SOURCE_INTERNAL
        );

        recyclerViewGallery.setLayoutManager(new GridLayoutManager(this, currentSpanCount));
        recyclerViewGallery.setAdapter(galleriesAdapter);

        logDatabaseEntries();
    }

    private void onPhotoClick(Uri uri) {
        Intent intent = new Intent(this, EditPhotoActivity.class);
        intent.putExtra("photoUri", uri.toString());
        startActivity(intent);
    }

    private void changeGridPattern() {
        currentSpanCount = (currentSpanCount % 5) + 1;
        GridLayoutManager layoutManager = (GridLayoutManager) recyclerViewGallery.getLayoutManager();
        if (layoutManager != null) {
            layoutManager.setSpanCount(currentSpanCount);
            galleriesAdapter.notifyDataSetChanged();
        }
    }

    private void logDatabaseEntries() {
        Cursor cursor = databaseHelper.getAllImages();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String uri = cursor.getString(cursor.getColumnIndexOrThrow("uri"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));

                Log.d("GalleryDatabase", "ID: " + id + ", URI: " + uri + ", Name: " + name);
            } while (cursor.moveToNext());
        } else {
            Log.d("GalleryDatabase", "No entries found in the database.");
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(galleryUpdateReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MEDIA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadImages();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_IMAGES && resultCode == RESULT_OK) {
            selectedImages.clear();

            if (data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    int limit = IMAGE_SELECTION_LIMIT;

                    for (int i = 0; i < Math.min(count, limit); i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        selectedImages.add(imageUri);
                    }

                    if (count > limit) {
                        Toast.makeText(this, "You can select a maximum of " + limit + " images.", Toast.LENGTH_SHORT).show();
                    }
                } else if (data.getData() != null) {
                    selectedImages.add(data.getData());
                }

                for (Uri uri : selectedImages) {
                    Toast.makeText(this, "Selected image URI: " + uri.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}

