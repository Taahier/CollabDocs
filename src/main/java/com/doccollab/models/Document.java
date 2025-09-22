package com.doccollab.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Document {
    @JsonProperty("documentId")
    private String documentId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("originalFile")
    private String originalFile;

    @JsonProperty("currentFile")
    private String currentFile;

    @JsonProperty("currentEditNumber")
    private int currentEditNumber;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("lastModified")
    private String lastModified;

    // Constructors
    public Document() {}

    public Document(String documentId, String title) {
        this.documentId = documentId;
        this.title = title;
        this.currentEditNumber = 1;
    }

    // Getters and Setters
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOriginalFile() { return originalFile; }
    public void setOriginalFile(String originalFile) { this.originalFile = originalFile; }

    public String getCurrentFile() { return currentFile; }
    public void setCurrentFile(String currentFile) { this.currentFile = currentFile; }

    public int getCurrentEditNumber() { return currentEditNumber; }
    public void setCurrentEditNumber(int currentEditNumber) { this.currentEditNumber = currentEditNumber; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getLastModified() { return lastModified; }
    public void setLastModified(String lastModified) { this.lastModified = lastModified; }
}
