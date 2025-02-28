package com.nbuit.galleryapp104204;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class PhotoDetailActivity extends AppCompatActivity {

    private ImageView imageViewPhoto;
    private Button buttonSave;
    private Uri photoUri;

    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();
    private PointF startPoint = new PointF();
    private float scaleFactor = 1f;
    private boolean isZoomed = false;
    private Bitmap currentBitmap;

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);

        imageViewPhoto = findViewById(R.id.imageViewPhoto);
        buttonSave = findViewById(R.id.buttonSave);

        imageViewPhoto.setBackgroundColor(getResources().getColor(android.R.color.black));

        Intent intent = getIntent();
        photoUri = Uri.parse(intent.getStringExtra("photoUri"));

        if (photoUri != null) {
            displayPhoto(photoUri);
        }

        gestureDetector = new GestureDetector(this, new GestureListener());

        imageViewPhoto.setOnTouchListener((v, event) -> {
            if (gestureDetector.onTouchEvent(event)) {
                return true;
            }

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    savedMatrix.set(matrix);
                    startPoint.set(event.getX(), event.getY());
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (isZoomed) {
                        float dx = event.getX() - startPoint.x;
                        float dy = event.getY() - startPoint.y;
                        matrix.set(savedMatrix);
                        matrix.postTranslate(dx, dy);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    v.performClick();
                    break;
            }

            imageViewPhoto.setImageMatrix(matrix);
            return true;
        });

        buttonSave.setOnClickListener(v -> {
            if (photoUri != null) {
                savePhotoToGallery(photoUri);
            } else {
                Toast.makeText(this, "No photo to save", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayPhoto(Uri photoUri) {
        new Thread(() -> {
            try {
                InputStream inputStream = new URL(photoUri.toString()).openStream();
                currentBitmap = BitmapFactory.decodeStream(inputStream);
                runOnUiThread(() -> {
                    imageViewPhoto.setImageBitmap(currentBitmap);
                    centerImage(currentBitmap);
                });
                inputStream.close();
            } catch (Exception e) {
                Log.e("PhotoDetailActivity", "Error loading photo", e);
            }
        }).start();
    }

    private void centerImage(Bitmap bitmap) {
        if (bitmap != null) {
            matrix.reset();
            float viewWidth = imageViewPhoto.getWidth();
            float viewHeight = imageViewPhoto.getHeight();
            float bitmapWidth = bitmap.getWidth();
            float bitmapHeight = bitmap.getHeight();

            float scaleX = viewWidth / bitmapWidth;
            float scaleY = viewHeight / bitmapHeight;
            float scale = Math.min(scaleX, scaleY);

            float dx = (viewWidth - bitmapWidth * scale) / 2;
            float dy = (viewHeight - bitmapHeight * scale) / 2;

            matrix.postScale(scale, scale);
            matrix.postTranslate(dx, dy);

            imageViewPhoto.setImageMatrix(matrix);
        }
    }

    private void resetImageView() {
        centerImage(currentBitmap);
        isZoomed = false;
        imageViewPhoto.setImageMatrix(matrix);
    }

    private void savePhotoToGallery(Uri photoUri) {
        new Thread(() -> {
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
                Log.e("PhotoDetailActivity", "Error saving photo to gallery", e);
                runOnUiThread(() -> Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (currentBitmap != null) {
                if (!isZoomed) {
                    scaleFactor = 2.0f;
                    matrix.reset();
                    float imageCenterX = currentBitmap.getWidth() / 2f;
                    float imageCenterY = currentBitmap.getHeight() / 2f;
                    matrix.postScale(scaleFactor, scaleFactor, imageCenterX, imageCenterY);
                    recenterImageAfterZoom();
                    isZoomed = true;
                    Toast.makeText(PhotoDetailActivity.this, "Zoomed In", Toast.LENGTH_SHORT).show();
                } else {
                    scaleFactor = 1f;
                    resetImageView();
                    isZoomed = false;
                    Toast.makeText(PhotoDetailActivity.this, "Zoomed Out", Toast.LENGTH_SHORT).show();
                }
                imageViewPhoto.setImageMatrix(matrix);
            }
            return true;
        }
    }

    private float oldDist = 1f;
    private PointF midPoint = new PointF();

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void recenterImageAfterZoom() {
        float viewWidth = imageViewPhoto.getWidth();
        float viewHeight = imageViewPhoto.getHeight();
        float newWidth = currentBitmap.getWidth() * scaleFactor;
        float newHeight = currentBitmap.getHeight() * scaleFactor;
        float dx = (viewWidth - newWidth) / 2;
        float dy = (viewHeight - newHeight) / 2;
        matrix.postTranslate(dx, dy);
        imageViewPhoto.setImageMatrix(matrix);
    }
}
