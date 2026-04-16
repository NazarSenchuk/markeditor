package com.markeditor.model;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class Document {
    private File file;
    private String title;
    private String content;

    public Document(File file) {
        this.file = file;
        this.title = file.getName().replace(".md", "");
        this.content = "";
    }

    public File getFile() {
        return file;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFileName() {
        return file != null ? file.getName() : "Без назви";
    }

    /**
     * Завантажує вміст документа з диску
     */
    public void load() throws IOException {
        if (file.exists()) {
            this.content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Зберігає поточний стан документа на диск
     */
    public void save() throws IOException {
        if (file != null) {
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        }
    }

    /**
     * Видаляє файл документа з диску
     */
    public void delete() throws IOException {
        if (file != null) {
            Files.deleteIfExists(file.toPath());
        }
    }
}
