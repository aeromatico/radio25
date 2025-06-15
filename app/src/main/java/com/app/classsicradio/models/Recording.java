package com.app.classsicradio.models;

import java.io.File;

public class Recording {

    private final String fileName;
    private final String filePath;

    public Recording(String fileName, String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
    }

    public String getName() {
        return fileName;
    }

    public String setName(String name) {
        return fileName;
    }

    public String getPath() {
        return filePath;
    }

    public String setPath(String path) {
        return filePath;
    }

    public File getFile() {
        return new File(filePath);
    }

    public File setFile(File file) {
        return new File(filePath);
    }
}