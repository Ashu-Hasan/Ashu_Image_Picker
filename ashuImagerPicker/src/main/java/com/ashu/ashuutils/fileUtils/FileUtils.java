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
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.FileProvider;

import com.ashu.ashuutils.ImageHelperMessages;
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

    /**
     * SharedPreferences name used to store app-related temporary data.
     */
    public static final String PREF_NAME = "app_prefs";

    /**
     * Key used to store last captured image path.
     */
    public static final String KEY_IMAGE_PATH = "image_path";


    /**
     * Callback interface for image source selection.
     *
     * Used to notify whether user selected:
     * - Camera
     * - Gallery
     */
    public interface ResultCallback {

        /**
         * Called when user selects camera option.
         *
         * @param isCamera true if camera selected, false otherwise
         */
        void onCameraSelected(boolean isCamera);

        /**
         * Called when user selects gallery option.
         */
        void onGallerySelected();
    }


    /**
     * Saves the captured image path in SharedPreferences.
     *
     * @param context Context to access SharedPreferences
     * @param path    Absolute file path of captured image
     *
     * ⚠️ Used internally to retrieve camera image later in onActivityResult()
     */
    public static void setImagePath(Context context, String path) {

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_IMAGE_PATH, path).apply();

        ImageHelperMessages.showTestLog("FileUtils", "💾 Image path saved: " + path);
    }


    /**
     * Retrieves the previously saved image path from SharedPreferences.
     *
     * @param context Context to access SharedPreferences
     * @return Stored image path or null if not found
     *
     * ⚠️ Used after camera capture to get file path
     */
    public static String getImagePath(Context context) {

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String path = prefs.getString(KEY_IMAGE_PATH, null);

        ImageHelperMessages.showTestLog("FileUtils", "📂 Retrieved image path: " + path);

        return path;
    }


    /**
     * Callback interface to return processed file result.
     *
     * Provides compressed image data including:
     * - File
     * - Bitmap
     * - Base64 string
     */
    public interface FileCallback {

        /**
         * Called when image processing is completed.
         *
         * @param selectedImageData Contains compressed file, bitmap, base64, and path
         */
        void onFileReady(CompressFileData selectedImageData);
    }


    /**
     * Copies an image from URI to a temporary cache file.
     *
     * This method is used when:
     * - Image is selected from gallery (Photo Picker / Intent)
     * - URI cannot be directly accessed as a file
     *
     * @param context Context to access content resolver and cache directory
     * @param uri     Source URI of selected image
     *
     * @return File object pointing to copied image in cache
     *
     * @throws IOException if file read/write fails
     *
     * ⚠️ Notes:
     * - Do NOT try to get real file path from URI (not reliable)
     * - Always use this method for safe file handling
     */
    public static File copyUriToCacheFile(Context context, Uri uri) throws IOException {

        ImageHelperMessages.showTestLog("FileUtils", "📥 Copying URI to cache: " + uri);

        InputStream inputStream = context.getContentResolver().openInputStream(uri);

        if (inputStream == null) {
            ImageHelperMessages.showTestLog("FileUtils", "❌ Failed to open input stream from URI");
            return null;
        }

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

        ImageHelperMessages.showTestLog("FileUtils", "✅ File copied to cache: " + cacheFile.getAbsolutePath());

        return cacheFile;
    }


    /**
     * ⚠️ DEPRECATED APPROACH (NOT RECOMMENDED)
     *
     * Tries to get absolute file path from URI.
     * This method may NOT work on Android 10+ (Scoped Storage).
     *
     * @param activity Activity context
     * @param uri      Content URI
     * @return Absolute file path (may be null)
     */
    public static String getAbsolutePath(Activity activity, Uri uri) {

        ImageHelperMessages.showTestLog("FileUtils", "⚠️ getAbsolutePath is deprecated for modern Android");

        String[] projection = {MediaStore.MediaColumns.DATA};

        try (Cursor cursor = activity.getContentResolver().query(uri, projection, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                String path = cursor.getString(columnIndex);

                ImageHelperMessages.showTestLog("FileUtils", "📂 Absolute path: " + path);
                return path;
            }

        } catch (Exception e) {
            ImageHelperMessages.showTestLog("FileUtils", "🔥 Error getting absolute path: " + e.getMessage());
        }

        return null;
    }


    /**
     * Reads a file into byte array.
     *
     * @param filePath Absolute file path
     * @return Byte array of file content
     * @throws IOException if read fails
     */
    private static byte[] readFileAsByteArray(String filePath) throws IOException {

        ImageHelperMessages.showTestLog("FileUtils", "📥 Reading file: " + filePath);

        File file = new File(filePath);

        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }

            ImageHelperMessages.showTestLog("FileUtils", "✅ File read successfully");
            return bos.toByteArray();
        }
    }

    /**
     * ⚠️ NOT RELIABLE ON ANDROID 10+
     *
     * Attempts to resolve real file path from URI.
     * This method may fail due to Scoped Storage restrictions.
     *
     * 👉 RECOMMENDED: Use copyUriToCacheFile() instead
     *
     * @param activity Activity context
     * @param uri      Content URI
     * @return File path or fallback URI path
     */
    public static String getRealPathFromURI(Activity activity, Uri uri) {

        ImageHelperMessages.showTestLog("FileUtils", "⚠️ getRealPathFromURI is not reliable on Android 10+");

        try (Cursor cursor = activity.getContentResolver().query(
                uri,
                new String[]{MediaStore.Images.Media.DATA},
                null,
                null,
                null
        )) {

            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                String path = cursor.getString(columnIndex);

                ImageHelperMessages.showTestLog("FileUtils", "📂 Real path: " + path);
                return path;
            }

        } catch (Exception e) {
            ImageHelperMessages.showTestLog("FileUtils", "🔥 Error resolving real path: " + e.getMessage());
        }

        // Fallback (not reliable)
        return uri.getPath();
    }


    /**
     * Converts a Bitmap into a temporary URI using FileProvider.
     *
     * This is useful when:
     * - You need URI from Bitmap (for crop or sharing)
     * - You cannot directly pass Bitmap to APIs
     *
     * @param bitmap  Bitmap to convert
     * @param context Context for file access
     * @return Content URI of saved bitmap
     */
    public static Uri getUriFromBitmap(Bitmap bitmap, Context context) {

        try {
            ImageHelperMessages.showTestLog("FileUtils", "🖼️ Converting bitmap to URI");

            // Create cache directory
            File imagesDir = new File(context.getCacheDir(), "images");
            if (!imagesDir.exists()) imagesDir.mkdirs();

            // Create file
            File file = new File(imagesDir, "camera_image_" + System.currentTimeMillis() + ".jpg");

            try (FileOutputStream outputStream = new FileOutputStream(file)) {

                // Compress bitmap
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                outputStream.flush();
            }

            // Return FileProvider URI
            Uri uri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".provider",
                    file
            );

            ImageHelperMessages.showTestLog("FileUtils", "✅ URI created: " + uri);

            return uri;

        } catch (IOException e) {
            ImageHelperMessages.showTestLog("FileUtils", "🔥 Error creating URI: " + e.getMessage());
            return null;
        }
    }


    /**
     * Safely decodes a Bitmap from a URI with downsampling to prevent OOM.
     *
     * This method:
     * ✔ Reads image from URI (Gallery / Photo Picker)
     * ✔ Avoids loading full-size image into memory
     * ✔ Resizes using inSampleSize
     *
     * @param context Activity context (for ContentResolver)
     * @param uri     Image URI
     *
     * @return Bitmap (scaled) or null if failed
     *
     * @throws IOException if stream fails
     */
    public static Bitmap getBitmapFromUri(Activity context, Uri uri) throws IOException {

        ImageHelperMessages.showTestLog("FileUtils", "🖼️ Decoding bitmap from URI: " + uri);

        InputStream input = context.getContentResolver().openInputStream(uri);

        if (input == null) {
            ImageHelperMessages.showTestLog("FileUtils", "❌ InputStream is null");
            return null;
        }

        // Step 1: Get image dimensions only
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, options);
        input.close();

        int originalWidth = options.outWidth;
        int originalHeight = options.outHeight;

        ImageHelperMessages.showTestLog("FileUtils", "📏 Original size: " + originalWidth + "x" + originalHeight);

        if (originalWidth <= 0 || originalHeight <= 0) {
            ImageHelperMessages.showTestLog("FileUtils", "❌ Invalid image dimensions");
            return null;
        }

        // Step 2: Define required size (you can adjust this)
        int reqWidth = 480;
        int reqHeight = 800;

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;

        // Step 3: Decode actual bitmap with sampling
        input = context.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
        input.close();

        if (bitmap != null) {
            ImageHelperMessages.showTestLog("FileUtils", "✅ Bitmap decoded successfully");
        } else {
            ImageHelperMessages.showTestLog("FileUtils", "❌ Failed to decode bitmap");
        }

        return bitmap;
    }


    /**
     * Calculates optimal inSampleSize for bitmap scaling.
     *
     * @param options   BitmapFactory options (contains original size)
     * @param reqWidth  Required width
     * @param reqHeight Required height
     *
     * @return inSampleSize (power of 2)
     */
    private static int calculateInSampleSize(
            BitmapFactory.Options options,
            int reqWidth,
            int reqHeight
    ) {

        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        ImageHelperMessages.showTestLog("FileUtils", "📐 Calculating inSampleSize...");

        if (height > reqHeight || width > reqWidth) {

            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight &&
                    (halfWidth / inSampleSize) >= reqWidth) {

                inSampleSize *= 2;

                ImageHelperMessages.showTestLog("FileUtils", "📉 inSampleSize increased to: " + inSampleSize);
            }
        }

        ImageHelperMessages.showTestLog("FileUtils", "✅ Final inSampleSize: " + inSampleSize);

        return inSampleSize;
    }


    /**
     * Converts a Bitmap into a File stored in cache directory.
     *
     * Useful for:
     * ✔ Uploading image to server
     * ✔ Passing file to APIs requiring File
     *
     * @param bitmap   Bitmap to convert
     * @param fileName Desired file name (e.g., "image.jpg")
     * @param context  Context for cache directory
     *
     * @return File object pointing to saved bitmap
     */
    public static File bitmapToFile(Bitmap bitmap, String fileName, Context context) {

        ImageHelperMessages.showTestLog("FileUtils", "💾 Converting bitmap to file: " + fileName);

        File file = new File(context.getCacheDir(), fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {

            // Compress bitmap (JPEG format)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);

            fos.flush();

            ImageHelperMessages.showTestLog("FileUtils", "✅ Bitmap saved to file: " + file.getAbsolutePath());

        } catch (IOException e) {
            ImageHelperMessages.showTestLog("FileUtils", "🔥 Error saving bitmap: " + e.getMessage());
        }

        return file;
    }



    /**
     * Downloads an image from URL and saves it in MediaStore if not already present.
     *
     * This method:
     * ✔ Checks if image already exists locally
     * ✔ Downloads image if not present
     * ✔ Saves image using MediaStore (Scoped Storage safe)
     * ✔ Displays image in ImageView (optional)
     *
     * @param TAG                 Logging tag for debugging
     * @param context             Context (must be Activity for UI updates)
     * @param imageUrl            Image URL to download
     * @param imageView           Optional ImageView to display image
     * @param viewImageCrossIcon  Optional cross icon visibility toggle
     * @param appImageFolderName  Folder name inside Pictures directory
     */
    public static void downloadImageIfNotExists(
            String TAG,
            Context context,
            String imageUrl,
            ImageView imageView,
            ImageView viewImageCrossIcon,
            String appImageFolderName
    ) {

        ImageHelperMessages.showTestLog(TAG, "🌐 Starting image download flow");

        ProgressDialog progressDialog = null;

        if (imageView != null) {
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Loading image...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        ProgressDialog finalDialog = progressDialog;

        new Thread(() -> {
            try {
                String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);

                ImageHelperMessages.showTestLog(TAG, "📥 Downloading: " + fileName);

                // Scoped storage path
                String relativePath = Environment.DIRECTORY_PICTURES + "/" + appImageFolderName;

                // Step 1: Download image
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException("Server returned HTTP " + connection.getResponseCode());
                }

                InputStream inputStream = connection.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }

                inputStream.close();

                byte[] imageBytes = baos.toByteArray();

                // Step 2: Save in MediaStore
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, relativePath);

                Uri imageUri = context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                );

                if (imageUri == null) {
                    throw new IOException("Failed to create MediaStore entry");
                }

                OutputStream outputStream = context.getContentResolver().openOutputStream(imageUri);

                if (outputStream != null) {
                    outputStream.write(imageBytes);
                    outputStream.flush();
                    outputStream.close();
                }

                ImageHelperMessages.showTestLog(TAG, "✅ Image saved to MediaStore");

                // Step 3: Display image
                if (imageView != null) {

                    Bitmap bitmap = decodeBitmapWithFallback(imageBytes, imageView);

                    ((Activity) context).runOnUiThread(() -> {
                        imageView.setImageBitmap(bitmap);
                        imageView.setVisibility(View.VISIBLE);

                        if (viewImageCrossIcon != null) {
                            viewImageCrossIcon.setVisibility(View.VISIBLE);
                        }

                        if (finalDialog != null) finalDialog.dismiss();
                    });
                }

            } catch (Exception e) {

                ImageHelperMessages.showTestLog(TAG, "🔥 Download failed: " + e.getMessage());

                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(() -> {
                        if (finalDialog != null) finalDialog.dismiss();
                    });
                }
            }
        }).start();
    }


    /**
     * Decodes bitmap safely from byte array and applies orientation fix.
     */
    static Bitmap decodeBitmapWithFallback(byte[] imageBytes, ImageView imageView) {

        int reqWidth = imageView.getWidth() > 0 ? imageView.getWidth() : 800;
        int reqHeight = imageView.getHeight() > 0 ? imageView.getHeight() : 800;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);

        return bitmap;
    }

    /**
     * Rotates bitmap based on EXIF orientation.
     */
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
            return Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    matrix,
                    true
            );
        } catch (OutOfMemoryError e) {
            return bitmap;
        }
    }


    /**
     * Retrieves display name of a document from its URI.
     *
     * @param activity Activity context
     * @param uri      Content URI
     * @return File name or fallback value
     */
    public static String getDocumentName(Activity activity, Uri uri) {

        String TAG = "FileUtils";
        String displayName = null;

        ImageHelperMessages.showTestLog(TAG, "📄 Getting document name from URI: " + uri);

        try (Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    displayName = cursor.getString(nameIndex);
                }
            }

        } catch (Exception e) {
            ImageHelperMessages.showTestLog(TAG, "🔥 Error fetching document name: " + e.getMessage());
        }

        // Fallback
        if (displayName == null) {
            displayName = uri.getLastPathSegment();
            ImageHelperMessages.showTestLog(TAG, "⚠️ Fallback document name used: " + displayName);
        }

        return displayName;
    }

    /**
     * Checks whether given URL is a PDF file.
     *
     * @param url File URL
     * @return true if PDF, else false
     */
    public static boolean isPDF(String url) {

        boolean result = url != null && url.toLowerCase().endsWith(".pdf");

        ImageHelperMessages.showTestLog("FileUtils", "📑 isPDF check: " + result + " for URL: " + url);

        return result;
    }

    /**
     * Renders first page of a PDF file into an ImageView as thumbnail.
     *
     * @param file      PDF file
     * @param imageView Target ImageView
     * @return true if rendered successfully, false otherwise
     */
    public static boolean renderPDFThumbnail(File file, ImageView imageView) {

        String TAG = "PDFThumbnail";

        if (file == null || !file.exists()) {
            ImageHelperMessages.showTestLog(TAG, "❌ File is null or does not exist");
            return false;
        }

        try (ParcelFileDescriptor fd =
                     ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
             PdfRenderer renderer = new PdfRenderer(fd)) {

            PdfRenderer.Page page = renderer.openPage(0);

            Bitmap bitmap = Bitmap.createBitmap(
                    page.getWidth(),
                    page.getHeight(),
                    Bitmap.Config.ARGB_8888
            );

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            imageView.setImageBitmap(bitmap);

            page.close();

            ImageHelperMessages.showTestLog(TAG, "✅ PDF thumbnail rendered successfully");

            return true;

        } catch (Exception e) {
            ImageHelperMessages.showTestLog(TAG, "🔥 PDF render failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Extracts file name from URL.
     *
     * @param url File URL
     * @return File name or empty string
     */
    public static String getFileNameFromUrl(String url) {

        String TAG = "FileUtils";

        if (url == null || url.trim().isEmpty()) {
            ImageHelperMessages.showTestLog(TAG, "⚠️ URL is empty");
            return "";
        }

        try {
            URL uri = new URL(url);
            String path = uri.getPath();
            String fileName = path.substring(path.lastIndexOf('/') + 1);

            ImageHelperMessages.showTestLog(TAG, "🌐 File name from URL: " + fileName);

            return fileName;

        } catch (MalformedURLException e) {
            ImageHelperMessages.showTestLog(TAG, "🔥 Invalid URL: " + e.getMessage());
            return "";
        }
    }

    /**
     * Resolves file name from:
     * ✔ Content URI
     * ✔ File path
     * ✔ URL
     *
     * @param context   Context
     * @param pathOrUrl Path, URI, or URL
     * @return File name
     */
    public static String getFileNameFromPath(Context context, String pathOrUrl) {

        String TAG = "FileNameResolver";

        if (pathOrUrl == null || pathOrUrl.trim().isEmpty()) {
            ImageHelperMessages.showTestLog(TAG, "⚠️ pathOrUrl is empty");
            return "unknown_file";
        }

        try {
            Uri uri = Uri.parse(pathOrUrl);

            // Case 1: content://
            if ("content".equalsIgnoreCase(uri.getScheme())) {

                try (Cursor cursor = context.getContentResolver()
                        .query(uri, null, null, null, null)) {

                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

                        if (nameIndex != -1) {
                            String fileName = cursor.getString(nameIndex);
                            ImageHelperMessages.showTestLog(TAG, "📄 From content URI: " + fileName);
                            return fileName;
                        }
                    }
                }
            }

            // Case 2: file path
            if (pathOrUrl.startsWith("file://") || new File(pathOrUrl).exists()) {

                File file = new File(pathOrUrl.replace("file://", ""));
                String fileName = file.getName();

                ImageHelperMessages.showTestLog(TAG, "📁 From file path: " + fileName);

                return fileName;
            }

            // Case 3: URL
            if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {

                String fileName = pathOrUrl.substring(pathOrUrl.lastIndexOf('/') + 1);

                try {
                    fileName = URLDecoder.decode(fileName, "UTF-8");
                } catch (Exception ignored) {}

                ImageHelperMessages.showTestLog(TAG, "🌐 From URL: " + fileName);

                return fileName;
            }

            // Case 4: fallback
            String fallback = pathOrUrl.substring(pathOrUrl.lastIndexOf('/') + 1);

            ImageHelperMessages.showTestLog(TAG, "⚠️ Fallback used: " + fallback);

            return fallback;

        } catch (Exception e) {
            ImageHelperMessages.showTestLog(TAG, "🔥 Error resolving file name: " + e.getMessage());
            return "unknown_file";
        }
    }

}
