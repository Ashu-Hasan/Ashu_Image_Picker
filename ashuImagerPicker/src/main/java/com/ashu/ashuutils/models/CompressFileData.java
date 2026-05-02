package com.ashu.ashuutils.models;

import android.graphics.Bitmap;

import java.io.File;
import java.io.Serializable;

/**
 * Model class representing compressed image data.
 *
 * This class contains multiple formats of processed image:
 * - File (for upload / storage)
 * - Base64 string (for APIs)
 * - Bitmap (for UI display)
 * - File path (for reference)
 *
 * ⚠️ Notes:
 * - Bitmap should NOT be serialized or passed between activities
 * - Use filePath or File instead for persistence
 */
public class CompressFileData implements Serializable {

    private static final long serialVersionUID = 1L;

    private File fileFormat;
    private String string64BaseFormat;
    private transient Bitmap bitmapFormat; // ⚠️ transient (important)
    private String filePath;

    /**
     * Default constructor
     */
    public CompressFileData() {}

    /**
     * Full constructor for quick initialization
     */
    public CompressFileData(File fileFormat, String base64, Bitmap bitmap, String filePath) {
        this.fileFormat = fileFormat;
        this.string64BaseFormat = base64;
        this.bitmapFormat = bitmap;
        this.filePath = filePath;
    }

    /**
     * Returns compressed file
     */
    public File getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(File fileFormat) {
        this.fileFormat = fileFormat;
    }

    /**
     * Returns Base64 encoded string of image
     */
    public String getString64BaseFormat() {
        return string64BaseFormat;
    }

    public void setString64BaseFormat(String string64BaseFormat) {
        this.string64BaseFormat = string64BaseFormat;
    }

    /**
     * Returns bitmap (for UI display only)
     */
    public Bitmap getBitmapFormat() {
        return bitmapFormat;
    }

    public void setBitmapFormat(Bitmap bitmapFormat) {
        this.bitmapFormat = bitmapFormat;
    }

    /**
     * Returns absolute file path of compressed image
     */
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Debug helper
     */
    @Override
    public String toString() {
        return "CompressFileData{" +
                "fileFormat=" + (fileFormat != null ? fileFormat.getAbsolutePath() : "null") +
                ", base64Length=" + (string64BaseFormat != null ? string64BaseFormat.length() : 0) +
                ", bitmap=" + (bitmapFormat != null ? "available" : "null") +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}
