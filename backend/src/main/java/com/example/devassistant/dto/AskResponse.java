package com.example.devassistant.dto;

import java.util.List;

public class AskResponse {
    private String questionType;
    private String answer;
    private List<SourceDTO> sources;

    public AskResponse() {}

    public AskResponse(String questionType, String answer, List<SourceDTO> sources) {
        this.questionType = questionType;
        this.answer = answer;
        this.sources = sources;
    }

    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public List<SourceDTO> getSources() { return sources; }
    public void setSources(List<SourceDTO> sources) { this.sources = sources; }
}
