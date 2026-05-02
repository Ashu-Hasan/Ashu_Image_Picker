package com.ashu.ashuutils.models;

import android.graphics.Bitmap;

import java.io.File;
import java.io.Serializable;

public class CompressFileData implements Serializable {
    File fileFormat;
    String string64BaseFormat;
    Bitmap bitmapFormat;
    String filePath;

    public CompressFileData() {}

    public File getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(File fileFormat) {
        this.fileFormat = fileFormat;
    }

    public String getString64BaseFormat() {
        return string64BaseFormat;
    }

    public void setString64BaseFormat(String string64BaseFormat) {
        this.string64BaseFormat = string64BaseFormat;
    }

    public void setBitmapFormat(Bitmap bitmapFormat) {
        this.bitmapFormat = bitmapFormat;
    }

    public Bitmap getBitmapFormat() {
        return bitmapFormat;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
