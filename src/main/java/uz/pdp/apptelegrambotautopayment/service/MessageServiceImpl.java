package uz.pdp.apptelegrambotautopayment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import uz.pdp.apptelegrambotautopayment.enums.Lang;
import uz.pdp.apptelegrambotautopayment.enums.LangFields;
import uz.pdp.apptelegrambotautopayment.enums.State;
import uz.pdp.apptelegrambotautopayment.model.User;
import uz.pdp.apptelegrambotautopayment.utils.AppConstants;
import uz.pdp.apptelegrambotautopayment.utils.CommonUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    private final CommonUtils commonUtils;
    private final LangService langService;
    private final ButtonService buttonService;
    private final Sender sender;

    @Override
    public void process(Message message) {
        if (message.hasText()) {
            String text = message.getText();
            if (text.equals(AppConstants.START)) {
                start(message);
                return;
            } else if (text.equals(AppConstants.CONNECT_CARD)) {
                sendConnectCardText(message);
            }
            Long userId = message.getFrom().getId();
            switch (commonUtils.getState(userId)) {
                case START -> {
                    if (langService.getMessage(LangFields.BUTTON_LANG_SETTINGS, userId).equals(text)) {
                        selectLanguage(userId);
                    } else if (langService.getMessage(LangFields.MY_CARD_TEXT, userId).equals(text)) {
                        sendConnectCardText(message);
                    }
                }
                case SELECT_LANGUAGE -> changeLanguage(text, userId);
            }
        }
    }

    private void changeLanguage(String text, Long userId) {
        Lang lang = langService.getLanguageEnum(text);
        String userLang = commonUtils.getLang(userId);
        commonUtils.setState(userId, State.START);
        if (lang == null) {
            sender.sendMessage(userId, langService.getMessage(LangFields.EXCEPTION_LANGUAGE, userLang), buttonService.start(userLang));
            return;
        }
        commonUtils.setLang(userId, lang);
        sender.sendMessage(userId, langService.getMessage(LangFields.SUCCESSFULLY_CHANGED_LANGUAGE, lang.name()), buttonService.start(lang.name()));
    }

    private void selectLanguage(long userId) {
        commonUtils.setState(userId, State.SELECT_LANGUAGE);
        String message = langService.getMessage(LangFields.BUTTON_LANG_SETTINGS, userId);
        sender.sendMessage(userId, message, buttonService.language(userId));
    }

    private void sendConnectCardText(Message message) {
        Long userId = message.getFrom().getId();
        User user = commonUtils.getUser(userId);
        String text = langService.getMessage(LangFields.EMPTY_CARD_NUMBER_TEXT, userId);
        String button = langService.getMessage(LangFields.ADD_CARD_NUMBER_TEXT, userId);
        if (user.getCardNumber() != null) {
            button = langService.getMessage(LangFields.CHANGE_CARD_NUMBER_TEXT, userId);
            text = langService.getMessage(LangFields.PRESENT_CARD_NUMBER_TEXT, userId).formatted(user.getCardNumber());
        }
        InlineKeyboardMarkup markup = buttonService.urlKeyboard(List.of(Map.of(button, AppConstants.WEB_APP_URL.formatted(userId))));
        sender.sendMessage(userId, text, markup);
    }

    private void start(Message message) {
        Long userId = message.getFrom().getId();
        User user = commonUtils.getUser(userId);
        user.setState(State.START);
        commonUtils.updateUser(user);
        sender.sendMessage(userId, langService.getMessage(LangFields.HELLO, userId), buttonService.start(commonUtils.getLang(userId)));
    }
}
