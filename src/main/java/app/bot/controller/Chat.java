package app.bot.controller;

import app.bot.config.BotConfig;
import app.bot.config.environment.CreateMessage;
import app.bot.model.Word;
import app.bot.service.WordService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class Chat extends TelegramLongPollingBot {
    @Autowired
    private BotConfig botConfig;
    @Autowired
    private CreateMessage createMessage;
    @Autowired
    private WordService wordService;


    private final TreeSet<String> tempWord = new TreeSet<>();
    private final HashSet<Long> waitWorAddWord = new HashSet<>();
    private final HashMap<Long, Integer> chatIdMsgId = new HashMap<>();
    private final HashMap<Long, List<Integer>> messageToEdit = new HashMap<>();


    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @PostConstruct
    public void init() {
        for (Word w : wordService.findAllWords()) {
            tempWord.add(w.getWord());
        }
    }
    @Scheduled(cron = "0 1 0 * * *")
    private void fillTheList() {
        init();
    }
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().getViaBot() != null) {
            if (("@" + update.getMessage().getViaBot().getUserName()).equals(botConfig.getBotName())) {
                System.out.println("!!!!");
                tempWord.remove(update.getMessage().getText());
            }
        }

        if (update.hasInlineQuery()) {
            waitWorAddWord.clear();
            ;
            inlineAnswer(update);
            return;
        }

        if (update.hasCallbackQuery()) {
            callBackDataHandle(update.getCallbackQuery().getMessage().getChatId(), update.getCallbackQuery().getData());
            return;
        }

        if (update.hasMessage()) {
            textMessageHandle(update.getMessage().getChatId(), update.getMessage().getText());
        }
    }

    private void inlineAnswer(Update update) {
        InlineQuery inlineQuery = update.getInlineQuery();
        String query = inlineQuery.getQuery();

        List<InlineQueryResultArticle> results = tempWord.stream()
                .filter(word -> word.toLowerCase().trim().contains(query.toLowerCase()))
                .map(this::getInlineQueryResult)
                .limit(50)
                .collect(Collectors.toList());

        AnswerInlineQuery answerInlineQuery = new AnswerInlineQuery();
        answerInlineQuery.setInlineQueryId(inlineQuery.getId());
        answerInlineQuery.setResults(Collections.unmodifiableList(results));
        answerInlineQuery.setCacheTime(0);
        try {
            execute(answerInlineQuery);
        } catch (TelegramApiException ignored) {
        }

    }

    public InlineQueryResultArticle getInlineQueryResult(String word) {
        InlineQueryResultArticle article = new InlineQueryResultArticle();
        article.setId(word);
        article.setTitle(word);
        InputTextMessageContent messageContent = new InputTextMessageContent();
        messageContent.setMessageText(word);
        article.setInputMessageContent(messageContent);
        return article;
    }

    private void callBackDataHandle(Long chatId, String data) {
        deleteKeyboard(chatId);

        if (data.equals("settings")) {
            executeLongMsg(createMessage.getAddNewWordAndList(chatId));
            return;
        }

        if (data.equals("add")) {
            waitWorAddWord.add(chatId);
            executeMsg(createMessage.getAddWordMessage(chatId, ""));
            return;
        }

        if (data.equals("reset0")) {
            waitWorAddWord.remove(chatId);
            executeMsg(createMessage.resetList(chatId));
            return;
        }

        if (data.equals("reset1")) {
            waitWorAddWord.remove(chatId);
            init();
            executeMsg(createMessage.listRested(chatId));
            executeMsg(createMessage.getStartMessage(chatId));
            return;
        }

        if (data.equals("start")) {
            waitWorAddWord.remove(chatId);
            executeMsg(createMessage.getStartMessage(chatId));
        }
    }

    private void textMessageHandle(Long chatId, String text) {
        if (text.equals("/start")) {
            waitWorAddWord.remove(chatId);
            executeMsg(createMessage.getStartMessage(chatId));
            return;

        }

        if (text.contains("/deleteWord_")) {
            try {
                deleteKeyboard(chatId);
                int id = Integer.parseInt(text.trim().split("_")[1]);
                tempWord.remove(wordService.findById(id));
                wordService.deleteById(id);
                executeLongMsg(createMessage.getAddNewWordAndList(chatId));
            } catch (Exception e) {
                executeLongMsg(createMessage.getAddNewWordAndList(chatId));
            }
            return;
        }

        if (waitWorAddWord.contains(chatId)) {
            deleteKeyboard(chatId);
            try {
                if (text.trim().split(",").length > 2) {
                    for (String s : text.split(",")) {
                        wordService.save(s.trim());
                        tempWord.add(s.trim());
                    }

                } else {
                    wordService.save(text.trim());
                    tempWord.add(text.trim());
                }

                executeMsg(createMessage.getAddWordMessage(chatId, "Данные сохранены!"));
            } catch (Exception e) {
                executeMsg(createMessage.getAddWordMessage(chatId, "Что-то пошло не так. Повторите попытку"));
            }
        }
    }

    private void executeMsg(SendMessage msg) {
        try {
            chatIdMsgId.put(Long.valueOf(msg.getChatId()),execute(msg).getMessageId());
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized void executeLongMsg(SendMessage msg) {
        String text = msg.getText();
        int chunkSize = 4096;

        if (text.length() > chunkSize) {
            int numChunks = (int) Math.ceil((double) text.length() / chunkSize);

            for (int i = 0; i < numChunks; i++) {
                int start = i * chunkSize;
                int end = Math.min((i + 1) * chunkSize, text.length());
                String chunk = text.substring(start, end);

                SendMessage chunkMsg = new SendMessage();
                chunkMsg.setChatId(msg.getChatId());
                chunkMsg.setText(chunk);
                chunkMsg.setReplyMarkup(msg.getReplyMarkup());

                try {
                    int msgId = execute(chunkMsg).getMessageId();
                    Long chatIdChunkMsg = Long.valueOf(chunkMsg.getChatId());

                    if (messageToEdit.containsKey(chatIdChunkMsg)) {
                        messageToEdit.get(chatIdChunkMsg).add(msgId);
                    } else {
                        List<Integer> list = new ArrayList<>();
                        list.add(msgId);
                        messageToEdit.put(chatIdChunkMsg, list);
                    }
                } catch (TelegramApiException e) {

                }
            }

        } else {
            try {
                chatIdMsgId.put(Long.valueOf(msg.getChatId()), execute(msg).getMessageId());
            } catch (TelegramApiException e) {
            }
        }
    }

    private void deleteKeyboard(Long chatId) {
        EditMessageReplyMarkup e = new EditMessageReplyMarkup();
        e.setChatId(chatId);
        e.setMessageId(chatIdMsgId.get(chatId));
        e.setReplyMarkup(null);
        try {
            execute(e);
        } catch (TelegramApiException ex) {
            throw new RuntimeException(ex);
        }
    }
}