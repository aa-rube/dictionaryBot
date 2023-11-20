package app.bot.service;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
public class Keyboards {
    public InlineKeyboardMarkup start() {
        InlineKeyboardMarkup inLineKeyBoard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardMatrix = new ArrayList<>();

        List<InlineKeyboardButton> firstRow = new ArrayList<>();
        InlineKeyboardButton settings = new InlineKeyboardButton();
        settings.setText("Настройки");
        settings.setCallbackData("settings");

        InlineKeyboardButton resetList = new InlineKeyboardButton();
        resetList.setText("Сбросить");
        resetList.setCallbackData("reset0");

        firstRow.add(settings);
        firstRow.add(resetList);

        keyboardMatrix.add(firstRow);
        inLineKeyBoard.setKeyboard(keyboardMatrix);
        return inLineKeyBoard;
    }

    public InlineKeyboardMarkup resetZero() {
        InlineKeyboardMarkup inLineKeyBoard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardMatrix = new ArrayList<>();

        List<InlineKeyboardButton> firstRow = new ArrayList<>();
        InlineKeyboardButton settings = new InlineKeyboardButton();
        settings.setText("ТОЧНО СБРОСИТЬ");
        settings.setCallbackData("reset1");

        InlineKeyboardButton resetList = new InlineKeyboardButton();
        resetList.setText("Не не, случайно");
        resetList.setCallbackData("start");

        firstRow.add(settings);
        firstRow.add(resetList);

        keyboardMatrix.add(firstRow);
        inLineKeyBoard.setKeyboard(keyboardMatrix);
        return inLineKeyBoard;
    }

    public InlineKeyboardMarkup addWord() {
        InlineKeyboardMarkup inLineKeyBoard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardMatrix = new ArrayList<>();
        List<InlineKeyboardButton> firstRow = new ArrayList<>();

        InlineKeyboardButton settings = new InlineKeyboardButton();
        settings.setText("Добавить слово");
        settings.setCallbackData("add");
        firstRow.add(settings);

        InlineKeyboardButton back = new InlineKeyboardButton();
        back.setText("Назад");
        back.setCallbackData("start");
        firstRow.add(back);

        keyboardMatrix.add(firstRow);
        inLineKeyBoard.setKeyboard(keyboardMatrix);
        return inLineKeyBoard;
    }

    public InlineKeyboardMarkup back() {
        InlineKeyboardMarkup inLineKeyBoard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardMatrix = new ArrayList<>();
        List<InlineKeyboardButton> firstRow = new ArrayList<>();
        InlineKeyboardButton settings = new InlineKeyboardButton();
        settings.setText("Назад");
        settings.setCallbackData("settings");
        firstRow.add(settings);
        keyboardMatrix.add(firstRow);
        inLineKeyBoard.setKeyboard(keyboardMatrix);
        return inLineKeyBoard;
    }
}
