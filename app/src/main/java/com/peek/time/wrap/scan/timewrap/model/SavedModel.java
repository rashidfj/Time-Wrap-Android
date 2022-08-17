package com.peek.time.wrap.scan.timewrap.model;

import java.io.File;

public class SavedModel{
    private File file;
    private String path;
    String created_date;
    String file_size;

    public SavedModel(File file, String path, String file_size, String created_date) {
        this.file = file;
        this.path = path;
        this.file_size=file_size;
        this.created_date=created_date;
    }

    public File getFile() {
        return file;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


}
