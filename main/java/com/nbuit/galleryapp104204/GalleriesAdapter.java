package com.nbuit.galleryapp104204;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GalleriesAdapter extends RecyclerView.Adapter<GalleriesAdapter.PhotoViewHolder> {

    public static final int IMAGE_SOURCE_INTERNAL = 0;
    public static final int IMAGE_SOURCE_API = 1;

    private List<Uri> imageUris;
    private Context context;
    private OnPhotoClickListener clickListener;
    private OnPhotoLongClickListener longClickListener;
    private int imageSource;
    private Set<Uri> markedImages = new HashSet<>();
    private ExecutorService executorService = Executors.newCachedThreadPool();

    public GalleriesAdapter(Context context, List<Uri> imageUris,
                            OnPhotoClickListener clickListener,
                            OnPhotoLongClickListener longClickListener,
                            int imageSource) {
        this.context = context;
        this.imageUris = imageUris;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        this.imageSource = imageSource;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView imageView;

        DownloadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            Bitmap bmp = null;
            HttpURLConnection connection = null;
            InputStream inputStream = null;

            try {
                URL imageUrl = new URL(url);
                connection = (HttpURLConnection) imageUrl.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                inputStream = connection.getInputStream();
                bmp = BitmapFactory.decodeStream(inputStream);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            return bmp;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                imageView.setImageBitmap(result);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Uri imageUri = imageUris.get(position);

        if (imageSource == IMAGE_SOURCE_API) {
            new DownloadImageTask(holder.imageViewPhoto).execute(imageUri.toString());
        } else {
            final PhotoViewHolder finalHolder = holder;
            new Thread(() -> {
                try {
                    InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();

                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() -> finalHolder.imageViewPhoto.setImageBitmap(bitmap));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        updateMarkVisuals(holder, imageUri);

        holder.itemView.setOnClickListener(v -> clickListener.onPhotoClick(imageUri));

        holder.itemView.setOnLongClickListener(v -> {
            toggleMarkImage(holder, imageUri);

            if (longClickListener != null) {
                longClickListener.onPhotoLongClick(imageUri);
            } else {
                Log.w("GalleriesAdapter", "LongClickListener is null");
            }

            return true;
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    private void toggleMarkImage(PhotoViewHolder holder, Uri imageUri) {
        if (markedImages.contains(imageUri)) {
            markedImages.remove(imageUri);
        } else {
            markedImages.add(imageUri);
        }
        updateMarkVisuals(holder, imageUri);
    }

    private void updateMarkVisuals(PhotoViewHolder holder, Uri imageUri) {
        if (markedImages.contains(imageUri)) {
            holder.overlayView.setVisibility(View.VISIBLE);
            holder.borderView.setVisibility(View.VISIBLE);
        } else {
            holder.overlayView.setVisibility(View.GONE);
            holder.borderView.setVisibility(View.GONE);
        }
    }

    private void loadImageFromUri(PhotoViewHolder holder, Uri imageUri) {
        executorService.execute(() -> {
            try (InputStream inputStream = context.getContentResolver().openInputStream(imageUri)) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                new Handler(Looper.getMainLooper()).post(() -> holder.imageViewPhoto.setImageBitmap(bitmap));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewPhoto;
        View overlayView;
        View borderView;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewPhoto = itemView.findViewById(R.id.imageViewItemPhoto);
            overlayView = itemView.findViewById(R.id.overlayView);
            borderView = itemView.findViewById(R.id.borderView);
        }
    }

    public interface OnPhotoClickListener {
        void onPhotoClick(Uri photoUri);
    }

    public interface OnPhotoLongClickListener {
        void onPhotoLongClick(Uri photoUri);
    }
}
