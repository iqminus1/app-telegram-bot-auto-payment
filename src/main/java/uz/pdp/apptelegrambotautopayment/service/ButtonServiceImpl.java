package uz.pdp.apptelegrambotautopayment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import uz.pdp.apptelegrambotautopayment.enums.LangFields;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ButtonServiceImpl implements ButtonService {
    private final LangService langService;

    @Override
    public ReplyKeyboard withString(List<String> list, int rowSize) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        int i = 1;
        for (String text : list) {
            row.add(new KeyboardButton(text));
            if (i == rowSize) {
                rows.add(row);
                row = new KeyboardRow();
                i = 0;
            }
            i++;
        }
        markup.setKeyboard(rows);
        return markup;
    }

    @Override
    public InlineKeyboardMarkup callbackKeyboard(List<Map<String, String>> textData) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (Map<String, String> map : textData) {

            for (String text : map.keySet()) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setCallbackData(map.get(text));
                button.setText(text);
                row.add(button);
            }

            rows.add(row);
            row = new ArrayList<>();

        }
        markup.setKeyboard(rows);
        return markup;
    }

    @Override
    public InlineKeyboardMarkup webAppKeyboard(String text, String url) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton(text);
        inlineKeyboardButton.setWebApp(new WebAppInfo(url));
        rows.add(List.of(inlineKeyboardButton));
        markup.setKeyboard(rows);
        return markup;
    }

    @Override
    public ReplyKeyboard language(Long userId) {
        List<String> list = new ArrayList<>();

        list.add(langService.getMessage(LangFields.BUTTON_LANGUAGE_UZBEK, userId));

        list.add(langService.getMessage(LangFields.BUTTON_LANGUAGE_RUSSIAN, userId));

        list.add(langService.getMessage(LangFields.BUTTON_LANGUAGE_ENGLISH, userId));

        return withString(list);
    }

    @Override
    public ReplyKeyboard start(String userLang) {
        String message = langService.getMessage(LangFields.ADD_CARD_NUMBER_TEXT, userLang);
        String changeLang = langService.getMessage(LangFields.BUTTON_LANG_SETTINGS, userLang);
        return withString(List.of(message, changeLang));
    }
}
