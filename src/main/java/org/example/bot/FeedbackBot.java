package org.example.bot;

import org.example.FeedBackManager;
import org.example.model.Feedback;
import org.example.model.UserProfile;
import org.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class FeedbackBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(FeedbackBot.class);

    private final FeedBackManager feedBackManager;
    private final String username;
    private final Map<Long, ConversationState> sessions = new ConcurrentHashMap<>();
    private final ReplyKeyboardMarkup rolesKeyboard;

    public FeedbackBot(String botToken, String username, FeedBackManager feedBackManager) {
        super(botToken);
        this.username = username;
        this.feedBackManager = feedBackManager;
        this.rolesKeyboard = createRolesKeyboard();
    }

    private static ReplyKeyboardMarkup createRolesKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(true);
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("Механік"));
        row.add(new KeyboardButton("Електрик"));
        row.add(new KeyboardButton("Менеджер"));
        keyboard.setKeyboard(List.of(row));
        return keyboard;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;
        Message msg = update.getMessage();
        long chatId = msg.getChatId();
        String text = msg.getText().trim();

        try {
            ConversationState cs = sessions.get(chatId);
            BotState state = cs == null ? null : cs.state;

            if (state == null) state = updateState(chatId);

            if (text.startsWith("/start")) {
                handleStart(msg);
                return;
            }

            switch (state) {
                case AWAITING_ROLE -> handleRole(chatId, text);
                case AWAITING_BRANCH -> handleBranch(chatId, text);
                case READY -> handleFeedback(chatId, text);
                default -> {
                    sendText(chatId, "Розпочніть з /start");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                sendText(chatId, "Сталася помилка. Спробуйте пізніше.");
            } catch (Exception ignored) {
            }
        }
    }

    private BotState updateState(long chatId) {
        Optional<UserProfile> user = feedBackManager.getUserRepo().findByChatId(chatId);

        log.debug("Update state, find user= {}", user);

        if (user.isPresent()) {
            ConversationState state = new ConversationState(BotState.READY);
            UserProfile profile = user.get();
            state.branch = profile.getBranch();
            state.role = profile.getRole();
            state.userProfile = profile;
            sessions.put(chatId, state);
            return BotState.READY;
        }
        return BotState.NONE;
    }

    private void sendText(long chatId, String text) throws TelegramApiException {
        SendMessage m = new SendMessage();
        m.setChatId(String.valueOf(chatId));
        m.enableMarkdown(true);
        m.setText(text);
        execute(m);
    }

    private void sendTextWithKeyboard(long chatId, String text, ReplyKeyboardMarkup keyboard) throws TelegramApiException {
        SendMessage m = new SendMessage();
        m.setChatId(String.valueOf(chatId));
        m.setText(text);
        m.setReplyMarkup(keyboard);
        execute(m);
    }

    private void handleStart(Message msg) throws Exception {
        long chatId = msg.getChatId();
        sessions.put(chatId, new ConversationState(BotState.AWAITING_ROLE));
        sendTextWithKeyboard(chatId, "Привіт! Будь ласка, оберіть вашу посаду:", rolesKeyboard);
    }

    private void handleRole(long chatId, String text) throws Exception {
        KeyboardRow allowed = rolesKeyboard.getKeyboard().get(0);
        if (!allowed.contains(text)) {
            sendTextWithKeyboard(chatId, "Невірний вибір. Будь ласка, оберіть одну з кнопок:", rolesKeyboard);
            return;
        }

        ConversationState cs = sessions.get(chatId);
        cs.state = BotState.AWAITING_BRANCH;
        cs.role = text;
        sendText(chatId, "Дякую. Тепер напишіть, будь ласка, назву філії (відділення СТО).");
    }

    private void handleBranch(long chatId, String text) throws Exception {
        ConversationState cs = sessions.get(chatId);

        log.debug("Role: " + cs.role);
        log.debug("Branch: " + cs.branch);
        log.debug("Chat id: " + chatId);

        cs.branch = text;

        UserProfile profile = new UserProfile();
        profile.setChatId(chatId);
        profile.setRole(cs.role);
        profile.setBranch(cs.branch);

        UserRepository userRepo = feedBackManager.getUserRepo();
        userRepo.upsertByChatId(profile);
        Optional<UserProfile> optUserProfile = userRepo.findByChatId(chatId);
        if (optUserProfile.isPresent()) {
            cs.userProfile = optUserProfile.get();
        } else {
            throw new RuntimeException("User with " + chatId + " not present in DB");
        }

        cs.state = BotState.READY;

        sendText(chatId, "*Посада:* " + cs.role + "\n" +
                         "*Філія: *" + cs.branch + "\n\n" +
                         "Тепер можете надcилати фідбеки!");
    }

    private void handleFeedback(long chatId, String text) throws TelegramApiException {
        ConversationState cs = sessions.get(chatId);
        log.debug("Feedback: " + text);

        Feedback feedback = new Feedback();
        feedback.setUser(cs.userProfile);
        feedback.setMessage(text);

        feedBackManager.save(feedback, (cid, resultMsg) -> {
            try {
                sendText(cid, resultMsg);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        });

        sendText(chatId, "Дякую, Ваш фідбек збережено!");
    }


    @Override
    public String getBotUsername() {
        return username;
    }

    private static final class ConversationState {
        private UserProfile userProfile;
        private BotState state;
        private String role;
        private String branch;

        public ConversationState(BotState state) {
            this.state = state;
        }
    }
}