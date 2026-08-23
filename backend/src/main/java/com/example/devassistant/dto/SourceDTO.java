package com.example.devassistant.dto;

public class SourceDTO {
    private String sourceType; // POLICY | CODE | COMMIT
    private Long sourceId;
    private String sourceLabel;

    public SourceDTO() {}

    public SourceDTO(String sourceType, Long sourceId, String sourceLabel) {
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.sourceLabel = sourceLabel;
    }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getSourceLabel() { return sourceLabel; }
    public void setSourceLabel(String sourceLabel) { this.sourceLabel = sourceLabel; }
}
