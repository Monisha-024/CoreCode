package com.example.devassistant.dto;

import java.util.List;

public class PolicyCompareResponse {
    private Integer fromVersion;
    private Integer toVersion;
    private List<String> addedLines;
    private List<String> removedLines;
    private List<String> unchangedSummaryNote;

    public Integer getFromVersion() { return fromVersion; }
    public void setFromVersion(Integer fromVersion) { this.fromVersion = fromVersion; }
    public Integer getToVersion() { return toVersion; }
    public void setToVersion(Integer toVersion) { this.toVersion = toVersion; }
    public List<String> getAddedLines() { return addedLines; }
    public void setAddedLines(List<String> addedLines) { this.addedLines = addedLines; }
    public List<String> getRemovedLines() { return removedLines; }
    public void setRemovedLines(List<String> removedLines) { this.removedLines = removedLines; }
    public List<String> getUnchangedSummaryNote() { return unchangedSummaryNote; }
    public void setUnchangedSummaryNote(List<String> unchangedSummaryNote) { this.unchangedSummaryNote = unchangedSummaryNote; }
}
