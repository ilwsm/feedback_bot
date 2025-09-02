package org.example.openai;

import lombok.ToString;

@ToString
public class FeedbackAnalysis {
    public String role;
    public String branch;
    public String sentiment; // "NEGATIVE", "NEUTRAL", "POSITIVE"
    public int criticality;        // 1..5
    public String recommendation;
}