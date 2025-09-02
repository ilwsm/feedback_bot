package org.example.dto;

import org.example.model.Feedback;

public record FeedbackDto(Long id,
                          String message,
                          String sentiment,
                          int criticality,
                          String recommendation,
                          Long chatId,
                          String userRole,
                          String userBranch,
                          String createdAt) {
    public static FeedbackDto fromEntity(Feedback f) {
        return new FeedbackDto(
                f.getId(),
                f.getMessage(),
                f.getSentiment(),
                f.getCriticality(),
                f.getRecommendation(),
                f.getUser().getChatId(),
                f.getUser().getRole(),
                f.getUser().getBranch(),
                f.getCreatedAt().toString()
        );
    }
}
