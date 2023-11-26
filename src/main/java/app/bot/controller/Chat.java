package app.bot.controller;

import app.bot.config.BotConfig;
import app.bot.environment.CreateMessage;
import app.bot.model.Word;
import app.bot.service.WordService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
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
    private final HashSet<Long> waitForAddWord = new HashSet<>();
    private final HashMap<Long, Integer> chatIdMsgId = new HashMap<>();
    private final HashMap<Long, List<Integer>> messageToEdit = new HashMap<>();
    private final HashSet<String> removed = new HashSet<>();
    private  final HashSet<String> fullWordsList = new HashSet<>();

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
        tempWord.clear();
        fullWordsList.clear();

        for (Word w : wordService.findAllWords()) {
            tempWord.add(w.getWord());
        }

        for(Word w : wordService.findAllWords()) {
            fullWordsList.add(w.getWord());
        }
    }

//    @Scheduled(cron = "0 1 0 * * *")
//    private void fillTheList() {
//        init();
//    }

    @Override
    public void onUpdateReceived(Update update) {
        if ((update.getChannelPost() != null && update.getChannelPost().getViaBot() != null) ||
                update.getMessage() != null && update.getMessage().getViaBot() != null) {
            try {
                String command = update.getChannelPost().getText().split(" ")[0].toLowerCase();
                updateListTempWord(command);

            } catch (Exception e) {
                String command = update.getMessage().getText().split(" ")[0].toLowerCase();
                updateListTempWord(command);
            }
            return;
        }

        if (update.hasInlineQuery()) {
            waitForAddWord.clear();
            inlineAnswer(update);
            return;
        }

        if (update.hasCallbackQuery()) {
            callBackDataHandle(update.getCallbackQuery().getMessage().getChatId(), update.getCallbackQuery().getData());
            return;
        }

        if (update.hasMessage() || update.hasChannelPost()) {
            try {
                textMessageHandle(update.getMessage().getChatId(), update.getMessage().getText());
            } catch (Exception e) {
                textMessageHandle(update.getChannelPost().getChatId(), update.getChannelPost().getText());
            }
            return;
        }
    }

    private void updateListTempWord(String command) {
        tempWord.removeIf(item -> {
            if (item.toLowerCase().contains(command)) {
                removed.add(item.split(" ")[0]);
                return true;
            }
            return  false;
        });
    }

    private void inlineAnswer(Update update) {
        InlineQuery inlineQuery = update.getInlineQuery();
        String query = inlineQuery.getQuery();

        List<InlineQueryResultArticle> results = tempWord.stream()
                .filter(word -> word.toLowerCase().trim().startsWith(query.toLowerCase()))
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
        try {
            deleteKeyboard(chatId);
        } catch (Exception ignored) {
        }

        if (data.equals("settings")) {
            executeLongMsg(createMessage.getAddNewWordAndList(chatId));
            return;
        }

        if (data.equals("add")) {
            waitForAddWord.add(chatId);
            executeMsg(createMessage.getAddWordMessage(chatId, ""));
            return;
        }

        if (data.equals("reset0")) {
            waitForAddWord.remove(chatId);
            executeMsg(createMessage.resetList(chatId));
            return;
        }

        if (data.equals("reset1")) {
            waitForAddWord.remove(chatId);
            init();
            executeMsg(createMessage.listRested(chatId));
            executeMsg(createMessage.getStartMessage(chatId));
            return;
        }

        if (data.equals("start")) {
            waitForAddWord.remove(chatId);
            executeMsg(createMessage.getStartMessage(chatId));
        }
    }

    private void textMessageHandle(Long chatId, String text) {
        if (text.equals("/start")) {
            waitForAddWord.remove(chatId);
            executeMsg(createMessage.getStartMessage(chatId));
            return;

        }
        
        if (text.equals("/del")) {
            init();
            executeMsg(createMessage.listRested(chatId));
        }

        if (text.equals("/s")) {
            executeMsg(createMessage.getRemovedWordsList(chatId, removed));
        }

        if (text.contains("/backToLIst_")) {
            String command = text.split("_")[1].toLowerCase();
            for(String s : fullWordsList) {
                if (s.toLowerCase().contains(command)) {
                    tempWord.add(s);
                }
            }

            removed.removeIf(item -> item.toLowerCase().contains(command));
            executeMsg(createMessage.getRemovedWordsList(chatId, removed));
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

        if (waitForAddWord.contains(chatId)) {
            try {
                deleteKeyboard(chatId);
            } catch (Exception ignored) {
            }
            try {
                if (text.trim().split(",").length > 2) {
                    for (String s : text.split(",")) {
                        wordService.save(s.trim());
                        tempWord.add(s.trim());
                        fullWordsList.add(s.trim());
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
            chatIdMsgId.put(Long.valueOf(msg.getChatId()), execute(msg).getMessageId());
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
        }
    }
}