package com.peek.time.wrap.scan.timewrap.model;

import java.io.File;

public class SavedModel{
    private File file;
    private String title;
    private String path;
    private boolean isVideo;
    String created_date;
    String file_size;

    public String getCreated_date() {
        return created_date;
    }

    public void setCreated_date(String created_date) {
        this.created_date = created_date;
    }

    public String getFile_size() {
        return file_size;
    }

    public void setFile_size(String file_size) {
        this.file_size = file_size;
    }

    public SavedModel(File file, String title, String path, String file_size, String created_date) {
        this.file = file;
        this.title = title;
        this.path = path;
        this.file_size=file_size;
        this.created_date=created_date;

        String MP4 = ".mp4";
        this.isVideo = file.getName().endsWith(MP4);
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isVideo() {
        return isVideo;
    }

    public void setVideo(boolean video) {
        isVideo = video;
    }
}
