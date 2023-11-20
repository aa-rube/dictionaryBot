package app.bot.config.environment;

import app.bot.model.Word;
import app.bot.service.Keyboards;
import app.bot.service.WordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@Service
public class CreateMessage {
    @Autowired
    private Keyboards keyboards;
    @Autowired
    private WordService wordService;
    private final StringBuilder builder = new StringBuilder();

    private SendMessage getSendMessage(Long chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        msg.setReplyMarkup(markup);
        msg.enableHtml(true);
        msg.setParseMode(ParseMode.HTML);
        return msg;
    }

    public SendMessage getStartMessage(Long chatId) {
        builder.setLength(0);
        builder.append("Приветствую! Это бот со списком слов.\n")
                .append("В админке можно добавить или удалить слова, сбросить список как будто ни одного слова не отправили сегодня.")
                .append("\n\nЕсли перезагрузить программу или компьютер, список тоже сброситься =))");

        return getSendMessage(chatId, builder.toString(), keyboards.start());
    }

    public SendMessage getAddNewWordAndList(Long chatId) {
        builder.setLength(0);
        builder.append("Список слов:\n");

        for (Word word : wordService.findAllWords()) {
            builder.append(word.getWord()).append("\n")
                    .append("/deleteWord_").append(word.getId()).append("\n");
        }
        return getSendMessage(chatId, builder.toString(), keyboards.addWord());
    }

    public SendMessage getAddWordMessage(Long chatId, String result) {
        builder.setLength(0);
        builder.append(result).append("\n\n");
        builder.append("Введите слово, что бы добавить его в список.\n\nЕсли нужно добавить много слов введите их через запятую.\n\n" +
                "Что бы добавить фразу с одной заптой ввдите ее отдельно(не в списке)");
        return getSendMessage(chatId, builder.toString(), keyboards.back());
    }

    public SendMessage resetList(Long chatId) {
        return getSendMessage(chatId, "Вы уверены, что хотите сбросить список?", keyboards.resetZero());
    }

    public SendMessage listRested(Long chatId) {
        return getSendMessage(chatId, "Список сброшен", null);
    }
}
