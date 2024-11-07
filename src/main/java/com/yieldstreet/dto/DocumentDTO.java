package com.yieldstreet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DocumentDTO {

    private String id;
    private String name;
    private String mimeType; // TODO enum, use JDK MimeType class
    private String content;

    @JsonProperty("document_id")
    public String getId() {
        return id;
    }

    @JsonProperty("document_id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("mime_type")
    public String getMimeType() {
        return mimeType;
    }

    @JsonProperty("mime_type")
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @JsonProperty("content")
    public String getContent() {
        return content;
    }

    @JsonProperty("content")
    public void setContent(String content) {
        this.content = content;
    }

}
