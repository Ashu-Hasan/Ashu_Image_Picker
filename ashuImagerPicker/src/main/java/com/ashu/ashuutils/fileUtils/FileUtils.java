package com.ashu.ashuutils.fileUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.FileProvider;

import com.ashu.ashuutils.Messages;
import com.ashu.ashuutils.models.CompressFileData;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

public interface FileUtils {

    public static final String PREF_NAME = "app_prefs";
    public static final String KEY_IMAGE_PATH = "image_path";

    public interface ResultCallback {
        void onCameraSelected(boolean isCamera);
        void onGallerySelected();
    }

    public static void setImagePath(Context context, String path) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_IMAGE_PATH, path).apply();
    }

    public static String getImagePath(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_IMAGE_PATH, null);
    }

    public interface FileCallback {
        void onFileReady(CompressFileData selectedImageData);
    }




    public static File copyUriToCacheFile(Context context, Uri uri) throws IOException {

        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) return null;

        File cacheFile = new File(
                context.getCacheDir(),
                "IMG_" + System.currentTimeMillis() + ".jpg"
        );

        OutputStream outputStream = new FileOutputStream(cacheFile);

        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }

        outputStream.flush();
        outputStream.close();
        inputStream.close();

        return cacheFile;
    }


    public static String getAbsolutePath(Activity activity, Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DATA};
        @SuppressWarnings("deprecation")
        Cursor cursor = activity.managedQuery(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else
            return null;
    }

    private static byte[] readFileAsByteArray(String filePath) throws IOException {
        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
            bos.write(buffer, 0, bytesRead);
        }
        fis.close();
        return bos.toByteArray();
    }

    public static String getRealPathFromURI(Activity activity, Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = activity.getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(columnIndex);
            cursor.close();
            return path;
        }
        return uri.getPath(); // Fallback
    }

    public static Uri getUriFromBitmap(Bitmap bitmap, Context context) {
        try {
            // Create a temporary file in the cache directory
            File imagesDir = new File(context.getCacheDir(), "images");
            if (!imagesDir.exists()) imagesDir.mkdirs();

            File file = new File(imagesDir, "camera_image.jpg");

            FileOutputStream outputStream = new FileOutputStream(file);

            // Compress and save the bitmap to the file
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            // Return a content URI using FileProvider
            return FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap getBitmapFromUri(Activity context, Uri uri) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);

        if (input == null) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, options);
        input.close();

        int originalWidth = options.outWidth;
        int originalHeight = options.outHeight;

        if (originalWidth <= 0 || originalHeight <= 0) {
            return null;
        }

        // Calculate a suitable inSampleSize
        int reqWidth = 480;  // Example width
        int reqHeight = 800; // Example height
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        options.inJustDecodeBounds = false;
        input = context.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
        input.close();

        return bitmap;
    }

    // Method to calculate the appropriate sample size
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    // Method to convert Bitmap to File
    public static File bitmapToFile(Bitmap bitmap, String fileName, Context context) {
        // Create a temporary file in the cache directory
        File file = new File(context.getCacheDir(), fileName);
        try {
            // Compress the bitmap and save it to the file
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream); // Use PNG if transparency is required
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }


    public static void downloadImageIfNotExists(String TAG, Context context, String imageUrl, ImageView imageView, ImageView viewImageCrossIcon, String appImageFolderName) {
        ProgressDialog progressDialog = null;

        if (imageView != null) {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Loading image...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        ProgressDialog finalProgressDialog = progressDialog;

        new Thread(() -> {
            try {
                String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
                String relativePath = Environment.DIRECTORY_PICTURES + "/AshuXKit";
                File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), appImageFolderName);
                File imageFile = new File(directory, fileName);

                // If image exists, load from file
                if (imageFile.exists()) {
                    if (imageView != null) {
                        Bitmap bitmap = decodeBitmapWithFallback(imageFile.getAbsolutePath(), imageView);
                        ((Activity) context).runOnUiThread(() -> {
                            imageView.setImageBitmap(bitmap);
                            imageView.setVisibility(View.VISIBLE);
                            viewImageCrossIcon.setVisibility(View.VISIBLE);
                            if (finalProgressDialog != null) finalProgressDialog.dismiss();
                        });
                    }
                    return;
                }

                // Download from URL
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream inputStream = connection.getInputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;

                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, relativePath);

                Uri imageUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (imageUri == null) {
                    Messages.showTestLog(TAG, "Failed to create image URI in MediaStore");
                    if (finalProgressDialog != null)
                        ((Activity) context).runOnUiThread(finalProgressDialog::dismiss);
                    return;
                }

                OutputStream outputStream = context.getContentResolver().openOutputStream(imageUri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    baos.write(buffer, 0, bytesRead);
                }

                outputStream.flush();
                outputStream.close();
                inputStream.close();

                if (imageView != null) {
                    byte[] imageBytes = baos.toByteArray();
                    Bitmap bitmap = decodeBitmapWithFallback(imageBytes, imageView);
                    ((Activity) context).runOnUiThread(() -> {
                        imageView.setImageBitmap(bitmap);
                        imageView.setVisibility(View.VISIBLE);
                        viewImageCrossIcon.setVisibility(View.VISIBLE);
                        if (finalProgressDialog != null) finalProgressDialog.dismiss();
                    });
                }

                Messages.showTestLog(TAG, "Image saved in "+appImageFolderName);

            } catch (Exception e) {
                Messages.showTestLog(TAG, "Download failed: " + e.getMessage());
                ((Activity) context).runOnUiThread(() -> {
                    if (finalProgressDialog != null) finalProgressDialog.dismiss();
                });
            }
        }).start();
    }

    static Bitmap decodeBitmapWithFallback(String path, ImageView imageView) {
        int reqWidth = imageView.getWidth() > 0 ? imageView.getWidth() : 800;
        int reqHeight = imageView.getHeight() > 0 ? imageView.getHeight() : 800;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);

        // Fix orientation
        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            bitmap = rotateBitmap(bitmap, orientation);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return bitmap;
        }

        try {
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return rotatedBitmap;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    // Helper for decoding from byte[]
    static Bitmap decodeBitmapWithFallback(byte[] data, ImageView imageView) {
        int reqWidth = imageView.getWidth() > 0 ? imageView.getWidth() : 800;
        int reqHeight = imageView.getHeight() > 0 ? imageView.getHeight() : 800;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    public static String getDocumentName(Activity activity, Uri uri) {
        String displayName = null;

        Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1 && cursor.moveToFirst()) {
                displayName = cursor.getString(nameIndex);
            }
            cursor.close();
        }

        if (displayName == null) {
            displayName = uri.getLastPathSegment();
        }
        return displayName;
    }

    public static boolean isPDF(String url) {
        return url != null && url.toLowerCase().endsWith(".pdf");
    }

    public static boolean renderPDFThumbnail(File file, ImageView imageView) {
        if (file == null || !file.exists()) {
            Log.e("PDF Thumbnail", "File is null or does not exist");
            return false;
        }

        try {
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(fd);
            PdfRenderer.Page page = renderer.openPage(0);

            Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            imageView.setImageBitmap(bitmap);

            page.close();
            renderer.close();
            fd.close();

            return true; // ✅ Successfully rendered
        } catch (Exception e) {
            e.printStackTrace();
            return false; // ❌ Rendering failed
        }
    }

    public static String getFileNameFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) return "";

        try {
            URL uri = new URL(url);
            String path = uri.getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getFileNameFromPath(Context context, String pathOrUrl) {
        String TAG = "FileNameResolver";
        if (pathOrUrl == null || pathOrUrl.trim().isEmpty()) {
            Log.w(TAG, "⚠️ Provided pathOrUrl is null or empty.");
            return "unknown_file";
        }

        try {
            Uri uri = Uri.parse(pathOrUrl);

            // Case 1️⃣: If it's a content:// URI (from MediaStore or FileProvider)
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                Cursor cursor = null;
                try {
                    cursor = context.getContentResolver().query(uri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (nameIndex != -1) {
                            String fileName = cursor.getString(nameIndex);
                            Log.i(TAG, "📄 File name from content URI: " + fileName);
                            return fileName;
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "🔥 Error reading content URI: " + e.getMessage());
                } finally {
                    if (cursor != null) cursor.close();
                }
            }

            // Case 2️⃣: If it's a file:// URI or a normal file path
            if (pathOrUrl.startsWith("file://") || new File(pathOrUrl).exists()) {
                File file = new File(pathOrUrl.replace("file://", ""));
                String fileName = file.getName();
                Log.i(TAG, "📁 File name from path: " + fileName);
                return fileName;
            }

            // Case 3️⃣: If it's a URL (e.g., https://example.com/file.pdf)
            if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
                String fileName = pathOrUrl.substring(pathOrUrl.lastIndexOf('/') + 1);
                try {
                    fileName = URLDecoder.decode(fileName, "UTF-8"); // handle %20 etc.
                } catch (Exception ignore) {
                }
                Log.i(TAG, "🌐 File name from URL: " + fileName);
                return fileName;
            }

            // Case 4️⃣: Fallback (extract after last slash)
            String fallback = pathOrUrl.substring(pathOrUrl.lastIndexOf('/') + 1);
            Log.w(TAG, "⚠️ Fallback file name used: " + fallback);
            return fallback;

        } catch (Exception e) {
            Log.e(TAG, "🔥 Exception in getFileNameFromPath: " + e.getMessage(), e);
            return "unknown_file";
        }
    }

}
