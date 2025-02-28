package com.nbuit.galleryapp104204;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InspirationsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewInspirations;
    private GalleriesAdapter galleriesAdapter;
    private List<Uri> imageUris;
    private Set<Uri> selectedPhotoUris;
    private Button buttonSave, buttonChangeGrid, buttonShare;
    private ExecutorService executorService;
    private boolean isGridView = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspirations);

        recyclerViewInspirations = findViewById(R.id.recyclerViewInspirations);
        buttonSave = findViewById(R.id.buttonSave);
        buttonChangeGrid = findViewById(R.id.buttonChangeGrid);
        buttonShare = findViewById(R.id.buttonShare);

        imageUris = new ArrayList<>();
        selectedPhotoUris = new HashSet<>();
        executorService = Executors.newSingleThreadExecutor();

        galleriesAdapter = new GalleriesAdapter(
                this,
                imageUris,
                this::onPhotoClick,
                this::onPhotoLongClick,
                GalleriesAdapter.IMAGE_SOURCE_API
        );
        recyclerViewInspirations.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerViewInspirations.setAdapter(galleriesAdapter);

        downloadPhotosFromUnsplash();

        buttonChangeGrid.setOnClickListener(v -> toggleGridView());
        buttonShare.setOnClickListener(v -> sharePhotos());
        buttonSave.setOnClickListener(v -> saveSelectedPhotos());
    }

    private void toggleGridView() {
        isGridView = !isGridView;
        int spanCount = isGridView ? 3 : 1;
        recyclerViewInspirations.setLayoutManager(new GridLayoutManager(this, spanCount));
    }

    private void sharePhotos() {
        if (selectedPhotoUris.isEmpty()) {
            Toast.makeText(this, "No photos selected to share", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.setType("image/*");
        ArrayList<Uri> uris = new ArrayList<>(selectedPhotoUris);
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        startActivity(Intent.createChooser(shareIntent, "Share photos using"));
    }

    private void saveSelectedPhotos() {
        if (selectedPhotoUris.isEmpty()) {
            Toast.makeText(this, "No photos selected to save", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Uri uri : selectedPhotoUris) {
            savePhotoToGallery(uri);
        }

        selectedPhotoUris.clear();
    }

    private void onPhotoClick(Uri photoUri) {
        Intent intent = new Intent(this, PhotoDetailActivity.class);
        intent.putExtra("photoUri", photoUri.toString());
        startActivity(intent);
    }

    private void onPhotoLongClick(Uri photoUri) {
        if (selectedPhotoUris.contains(photoUri)) {
            selectedPhotoUris.remove(photoUri);
        } else {
            selectedPhotoUris.add(photoUri);
        }
        galleriesAdapter.notifyDataSetChanged();
    }

    private void downloadPhotosFromUnsplash() {
        executorService.submit(() -> {
            String apiUrl = "https://api.unsplash.com/photos?client_id=Da7aNY1lhfatAJX-J9yRL65HCJJge2W_3SAe8qYjaHA";
            String downloadedData = downloadPhotos(apiUrl);
            List<Uri> downloadedPhotoUris = parseDownloadedData(downloadedData);

            runOnUiThread(() -> {
                if (downloadedPhotoUris != null && !downloadedPhotoUris.isEmpty()) {
                    imageUris.addAll(downloadedPhotoUris);
                    galleriesAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(this, "No images found", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private String downloadPhotos(String apiUrl) {
        StringBuilder result = new StringBuilder();
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(apiUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();
        } catch (Exception e) {
            Log.e("InspirationsActivity", "Error downloading photos", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return result.toString();
    }

    private List<Uri> parseDownloadedData(String downloadedData) {
        List<Uri> photoUris = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(downloadedData);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject photoObject = jsonArray.getJSONObject(i);
                String photoUrl = photoObject.getJSONObject("urls").getString("regular");
                Uri photoUri = Uri.parse(photoUrl);
                photoUris.add(photoUri);
            }
        } catch (Exception e) {
            Log.e("InspirationsActivity", "Error parsing downloaded data", e);
        }
        return photoUris;
    }

    private void savePhotoToGallery(Uri photoUri) {
        executorService.submit(() -> {
            Bitmap bitmap = null;
            try {
                InputStream inputStream = new URL(photoUri.toString()).openStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();

                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, "Saved Photo");
                values.put(MediaStore.Images.Media.DISPLAY_NAME, "Saved_Photo_" + System.currentTimeMillis());
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.close();
                    runOnUiThread(() -> Toast.makeText(this, "Photo saved to gallery", Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e("InspirationsActivity", "Error saving photo to gallery", e);
                runOnUiThread(() -> Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
