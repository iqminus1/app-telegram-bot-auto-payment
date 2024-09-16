package uz.pdp.apptelegrambotautopayment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import uz.pdp.apptelegrambotautopayment.enums.Lang;
import uz.pdp.apptelegrambotautopayment.enums.LangFields;
import uz.pdp.apptelegrambotautopayment.enums.State;
import uz.pdp.apptelegrambotautopayment.model.User;
import uz.pdp.apptelegrambotautopayment.utils.AppConstants;
import uz.pdp.apptelegrambotautopayment.utils.CommonUtils;
import uz.pdp.apptelegrambotautopayment.utils.Temp;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    private final CommonUtils commonUtils;
    private final LangService langService;
    private final ButtonService buttonService;
    private final Sender sender;
    private final Temp temp;

    @Override
    public void process(Message message) {
        if (message.hasText()) {
            String text = message.getText();
            Long userId = message.getFrom().getId();
            if (text.equals(AppConstants.START)) {
                start(userId);
                return;
            }
            switch (commonUtils.getState(userId)) {
                case START -> {
                    if (langService.getMessage(LangFields.BUTTON_LANG_SETTINGS, userId).equals(text)) {
                        selectLanguage(userId);
                    } else if (langService.getMessage(LangFields.ADD_CARD_NUMBER_TEXT, userId).equals(text)) {
                        sendAddCardNumberText(userId);
                    }
                }
                case SENDING_CARD_NUMBER -> {
                    if (langService.getMessage(LangFields.BACK_TEXT, userId).equals(text)) {
                        start(userId);
                    } else {
                        sendingCardNumber(userId, text);
                    }
                }
                case SENDING_CARD_EXPIRE -> {
                    if (langService.getMessage(LangFields.BACK_TEXT, userId).equals(text)) {
                        start(userId);
                    } else {
                        sendingCardExpire(userId, text);
                    }
                }
                case SENDING_CARD_CODE -> {

                }
                case SELECT_LANGUAGE -> changeLanguage(text, userId);
            }
        }
    }

    private void sendingCardExpire(Long userId, String text) {
        if (text.matches("^(0[1-9]|1[0-2])/(\\d{2}$)")) {
            User user = temp.getUser(userId);
            user.setCardExpiry(text);
            temp.setUser(user);
            commonUtils.setState(userId, State.SENDING_CARD_CODE);
            //Atmosga ulash kerak
            sender.sendMessage(userId, langService.getMessage(LangFields.SEND_CARD_CODE_TEXT, userId), new ReplyKeyboardRemove(true));
            return;
        }
        sender.sendMessage(userId, langService.getMessage(LangFields.EXCEPTION_CARD_EXPIRE_TEXT, userId));
    }

    private void sendingCardNumber(Long userId, String text) {
        commonUtils.setState(userId, State.SENDING_CARD_EXPIRE);
        User user = commonUtils.getUser(userId);
        if (text.matches("\\d{16}")) { // Формат без пробелов
            String formattedCardNumber = text.substring(0, 4) + " " +
                    text.substring(4, 8) + " " +
                    text.substring(8, 12) + " " +
                    text.substring(12);
            user.setCardNumber(formattedCardNumber);
            temp.setUser(user);
            sender.sendMessage(userId, langService.getMessage(LangFields.SEND_CARD_EXPIRE_TEXT, userId));
            return;
        } else if (text.matches("\\d{4} \\d{4} \\d{4} \\d{4}")) {
            user.setCardNumber(text);
            temp.setUser(user);
            sender.sendMessage(userId, langService.getMessage(LangFields.SEND_CARD_EXPIRE_TEXT, userId));
            return;
        }
        sender.sendMessage(userId, langService.getMessage(LangFields.EXCEPTION_CARD_NUMBER_TEXT, userId));
    }

    private void sendAddCardNumberText(Long userId) {
        commonUtils.setState(userId, State.SENDING_CARD_NUMBER);
        User user = temp.getUser(userId);
        if (user != null) {
            user.setCardNumber(null);
            temp.setUser(user);
        }
        String message = langService.getMessage(LangFields.SEND_CARD_NUMBER_TEXT, userId);
        ReplyKeyboard replyKeyboard = buttonService.withString(List.of(langService.getMessage(LangFields.BACK_TEXT, userId)));
        sender.sendMessage(userId, message, replyKeyboard);
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

    private void start(Long userId) {
        User user = commonUtils.getUser(userId);
        user.setState(State.START);
        commonUtils.updateUser(user);
        temp.removeUser(userId);
        sender.sendMessage(userId, langService.getMessage(LangFields.HELLO, userId), buttonService.start(commonUtils.getLang(userId)));
    }
}
