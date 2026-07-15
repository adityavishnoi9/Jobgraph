package com.JobGraph.jobgraph_backend.DTO;

public class ProfileUploadResponse {

    private String resumeRawText;
    private String additionalInfo;

    public ProfileUploadResponse(String resumeRawText, String additionalInfo) {
        this.resumeRawText = resumeRawText;
        this.additionalInfo = additionalInfo;
    }

    public String getResumeRawText() { return resumeRawText; }
    public void setResumeRawText(String resumeRawText) { this.resumeRawText = resumeRawText; }

    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }
}
