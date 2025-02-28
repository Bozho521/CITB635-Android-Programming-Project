package com.nbuit.galleryapp104204;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;

public class EditPhotoActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1;

    private ImageView imageViewPhoto;
    private Bitmap originalBitmap;
    private Bitmap editedBitmap;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_photo);

        imageViewPhoto = findViewById(R.id.imageViewPhoto);
        setUpButtons();

        Intent intent = getIntent();
        imageUri = Uri.parse(intent.getStringExtra("photoUri"));
        loadOriginalImage();
    }

    private void setUpButtons() {
        findViewById(R.id.buttonGreyscale).setOnClickListener(v -> applyEffect(this::applyGreyscale));
        findViewById(R.id.buttonInvertColors).setOnClickListener(v -> applyEffect(this::applyInvertColors));
        findViewById(R.id.buttonUndo).setOnClickListener(v -> resetImage());
        findViewById(R.id.buttonSave).setOnClickListener(v -> saveNewPhoto());
        findViewById(R.id.buttonReplace).setOnClickListener(v -> showReplaceConfirmationDialog());
        findViewById(R.id.buttonDelete).setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void loadOriginalImage() {
        if (imageUri != null) {
            try {
                originalBitmap = loadImageFromUri(imageUri);
                imageViewPhoto.setImageBitmap(originalBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap loadImageFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        return BitmapFactory.decodeStream(inputStream);
    }

    private void applyEffect(BitmapProcessor processor) {
        if (originalBitmap != null) {
            editedBitmap = processor.process(originalBitmap);
            imageViewPhoto.setImageBitmap(editedBitmap);
            showSaveReplaceButtons();
        }
    }

    private void resetImage() {
        imageViewPhoto.setImageBitmap(originalBitmap);
        hideSaveReplaceButtons();
    }

    private void showSaveReplaceButtons() {
        findViewById(R.id.saveReplacePanel).setVisibility(View.VISIBLE);
    }

    private void hideSaveReplaceButtons() {
        findViewById(R.id.saveReplacePanel).setVisibility(View.GONE);
    }

    private void saveNewPhoto() {
        if (checkPermissions()) {
            try {
                String savedImageURL = MediaStore.Images.Media.insertImage(
                        getContentResolver(),
                        editedBitmap,
                        "Edited Photo",
                        "Edited photo from app"
                );

                Uri savedImageUri = Uri.parse(savedImageURL);
                if (savedImageUri != null) {
                    Toast.makeText(this, "Image saved: " + savedImageUri, Toast.LENGTH_SHORT).show();
                    notifyGalleryUpdate();
                    hideSaveReplaceButtons();
                } else {
                    Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        } else {
            requestPermissions();
        }
    }

    private void showReplaceConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Replace Photo")
                .setMessage("You are about to replace the image. Proceed?")
                .setPositiveButton("Yes", (dialog, which) -> replaceOriginalPhoto())
                .setNegativeButton("No", null)
                .show();
    }

    private void replaceOriginalPhoto() {
        if (checkPermissions()) {
            try {
                String imagePath = getImagePathFromUri(imageUri);

                getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Images.Media.DATA + "=?", new String[]{imagePath});

                String savedImageURL = MediaStore.Images.Media.insertImage(
                        getContentResolver(),
                        editedBitmap,
                        "Replaced Photo",
                        "Replaced photo"
                );
                Uri savedImageUri = Uri.parse(savedImageURL);
                Toast.makeText(this, "Image replaced: " + savedImageUri, Toast.LENGTH_SHORT).show();

                notifyGalleryUpdate();
                hideSaveReplaceButtons();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to replace image", Toast.LENGTH_SHORT).show();
            }
        } else {
            requestPermissions();
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Photo")
                .setMessage("Do you want to permanently delete the photo?")
                .setPositiveButton("Yes", (dialog, which) -> deletePhoto())
                .setNegativeButton("No", null)
                .show();
    }

    private void deletePhoto() {
        if (checkPermissions()) {
            try {
                String imagePath = getImagePathFromUri(imageUri);
                getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Images.Media.DATA + "=?", new String[]{imagePath});
                Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show();
                notifyGalleryUpdate();
                finish();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to delete photo", Toast.LENGTH_SHORT).show();
            }
        } else {
            requestPermissions();
        }
    }

    private String getImagePathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
            }
        }
        return null;
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void notifyGalleryUpdate() {
        Intent intent = new Intent("ACTION_UPDATE_GALLERY");
        sendBroadcast(intent);
    }

    private interface BitmapProcessor {
        Bitmap process(Bitmap bitmap);
    }

    private Bitmap applyGreyscale(Bitmap bitmap) {
        return applyColorTransformation(bitmap, Color.rgb(0, 0, 0));
    }

    private Bitmap applyInvertColors(Bitmap bitmap) {
        return applyColorTransformation(bitmap, Color.rgb(255, 255, 255));
    }

    private Bitmap applyColorTransformation(Bitmap bitmap, int colorTransform) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap transformedBitmap = Bitmap.createBitmap(width, height, bitmap.getConfig());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmap.getPixel(x, y);
                int r = colorTransform == Color.rgb(0, 0, 0) ? (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3 : 255 - Color.red(pixel);
                int g = colorTransform == Color.rgb(0, 0, 0) ? (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3 : 255 - Color.green(pixel);
                int b = colorTransform == Color.rgb(0, 0, 0) ? (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3 : 255 - Color.blue(pixel);
                transformedBitmap.setPixel(x, y, Color.rgb(r, g, b));
            }
        }
        return transformedBitmap;
    }
}

