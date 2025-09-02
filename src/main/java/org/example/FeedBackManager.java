package org.example;

import org.example.model.Feedback;
import org.example.model.UserProfile;
import org.example.openai.FeedbackAnalysis;
import org.example.openai.OpenAIClassifier;
import org.example.repository.FeedbackRepository;
import org.example.repository.GoogleSheetsService;
import org.example.repository.UserRepository;
import org.example.trello.TrelloClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;

public class FeedBackManager {
    private static final Logger log = LoggerFactory.getLogger(FeedBackManager.class);

    private final UserRepository userRepo;
    private final FeedbackRepository feedbackRepo;
    private final GoogleSheetsService gss;
    private final OpenAIClassifier aiClassifier;
    private final TrelloClient trelloClient;

    public FeedBackManager(UserRepository userRepo, FeedbackRepository feedbackRepo, GoogleSheetsService gss, OpenAIClassifier aiClassifier, TrelloClient trelloClient) {

        this.userRepo = userRepo;
        this.feedbackRepo = feedbackRepo;
        this.gss = gss;
        this.aiClassifier = aiClassifier;
        this.trelloClient = trelloClient;
    }

    public void save(Feedback feedback, BiConsumer<Long, String> callback) {
        ThreadSaverRunnable saverRunnable = new ThreadSaverRunnable(feedback, callback);
        new Thread(saverRunnable).start();
    }

    public FeedbackRepository getFeedbackRepo() {
        return feedbackRepo;
    }

    public UserRepository getUserRepo() {
        return userRepo;
    }

    private class ThreadSaverRunnable implements Runnable {
        private final Feedback feedback;
        private final BiConsumer<Long, String> callback;

        public ThreadSaverRunnable(Feedback feedback, BiConsumer<Long, String> callback) {
            this.feedback = feedback;
            this.callback = callback;
        }

        @Override
        public void run() {
            try {
                UserProfile user = feedback.getUser();
                log.info("Start processing feedback from chatId={}",
                        user.getChatId());

                FeedbackAnalysis analysis = aiClassifier.analyze(feedback.getMessage());
                log.debug("AI analysis result: {}", analysis);

                feedback.setSentiment(analysis.sentiment);
                feedback.setCriticality(analysis.criticality);
                feedback.setRecommendation(analysis.recommendation);

                feedbackRepo.save(feedback);

                appendFeedackOnSheet(user);

                StringBuilder sb = new StringBuilder("Дякую за відгук! Результат аналізу:\n" +
                                                     "Sentiment: " + analysis.sentiment + "\n" +
                                                     "Criticality: " + analysis.criticality + "\n" +
                                                     "Recommendation: " + analysis.recommendation);


                if (feedback.getCriticality() >= 4) {
                    try {
                        String cardUrl = createTrelloCard(user);
                        sb.append("\n\n Створено критичну картку в Trello: ").append(cardUrl);
                    } catch (Exception e) {
                        log.error("Failed to create Trello card for chatId=" + user.getChatId(), e);
                    }
                }

                callback.accept(user.getChatId(), sb.toString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void appendFeedackOnSheet(UserProfile user) throws IOException {
            gss.appendFeedbackRow(
                    user.getChatId().toString(),
                    List.of(
                            user.getRole(),
                            user.getBranch(),
                            feedback.getMessage(),
                            feedback.getSentiment(),
                            feedback.getCriticality(),
                            feedback.getRecommendation(),
                            feedback.getCreatedAt().toString()
                    )
            );
            log.info("Feedback appended to Google Sheet for chatId={}", user.getChatId());
        }

        private String createTrelloCard(UserProfile user) throws IOException, InterruptedException {
            String cardName = String.format("Critical %d — %s / %s",
                    feedback.getCriticality(),
                    user.getRole(),
                    user.getBranch());

            String desc = String.format(
                    "Chat ID: %d\nRole: %s\nBranch: %s\n\nMessage: %s\n\nSentiment: %s\nCriticality: %d\nRecommendation: %s\nCreated: %s",
                    user.getChatId(),
                    user.getRole(),
                    user.getBranch(),
                    feedback.getMessage(),
                    feedback.getSentiment(),
                    feedback.getCriticality(),
                    feedback.getRecommendation(),
                    feedback.getCreatedAt()
            );

            String cardUrl = trelloClient.createCard(cardName, desc);
            log.info("Trello card created for critical feedback from chatId={}", user.getChatId());
            return cardUrl;
        }
    }
}
