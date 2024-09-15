package uz.pdp.apptelegrambotautopayment.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.util.List;
import java.util.Map;

public interface ButtonService {

    default ReplyKeyboard withString(List<String> list) {
        return withString(list, 1);
    }

    ReplyKeyboard withString(List<String> list, int rowSize);

    InlineKeyboardMarkup callbackKeyboard(List<Map<String, String>> textData);
    InlineKeyboardMarkup urlKeyboard(List<Map<String, String>> textUrl);
    ReplyKeyboard language(Long userId);

    ReplyKeyboard start(String userLang);
}
