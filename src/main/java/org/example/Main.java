package org.example;

import jakarta.persistence.EntityManagerFactory;
import org.example.admin.AdminServer;
import org.example.bot.FeedbackBot;
import org.example.openai.OpenAIClassifier;
import org.example.openai.OpenAIService;
import org.example.repository.FeedbackRepository;
import org.example.repository.GoogleSheetsService;
import org.example.repository.UserRepository;
import org.example.trello.TrelloClient;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Properties;

public class Main {

    public static void main(String[] args) throws Exception {
        Properties cfg = ConfigLoader.load();

        String token = cfg.getProperty("telegram_token");
        String username = cfg.getProperty("telegram_username");

        // if token is empty, abort
        if (token == null || token.isBlank()) {
            System.err.println("telegram_token is not set (use config.properties)");
            System.exit(1);
        }

        EntityManagerFactory emf = JPAUtil.createEntityManagerFactory(cfg);
        GoogleSheetsService gss = GoogleSheetsService.create(cfg);

        FeedBackManager feedBackManager = new FeedBackManager(
                new UserRepository(emf), new FeedbackRepository(emf), gss,
                new OpenAIClassifier(new OpenAIService(cfg.getProperty("openai_key"))),
                new TrelloClient(cfg.getProperty("trello_key"), cfg.getProperty("trello_token"), cfg.getProperty("trello_listId"))
        );

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(new FeedbackBot(token, username, feedBackManager));

        System.out.println("Bot started");

        new AdminServer(feedBackManager.getFeedbackRepo()).start();
    }


}