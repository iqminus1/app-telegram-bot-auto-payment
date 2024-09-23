package uz.pdp.apptelegrambotautopayment.service.impl;

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
import uz.pdp.apptelegrambotautopayment.enums.PaymentMethod;
import uz.pdp.apptelegrambotautopayment.model.User;
import uz.pdp.apptelegrambotautopayment.repository.TransactionRepository;
import uz.pdp.apptelegrambotautopayment.service.ButtonService;
import uz.pdp.apptelegrambotautopayment.service.LangService;
import uz.pdp.apptelegrambotautopayment.utils.AppConstants;
import uz.pdp.apptelegrambotautopayment.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ButtonServiceImpl implements ButtonService {
    private final LangService langService;
    private final CommonUtils commonUtils;
    private final TransactionRepository transactionRepository;

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

        list.add(langService.getMessage(LangFields.BUTTON_LANGUAGE_UZBEKKR, userId));

        return withString(list);
    }

    @Override
    public ReplyKeyboard start(Long userId) {
        User user = commonUtils.getUser(userId);
        List<String> list = new LinkedList<>();

        if (commonUtils.getUser(userId).getAdmin() != 0) {
            list.add(langService.getMessage(LangFields.ADMIN_MENU_TEXT, userId));
        }

        //card button
        if (AppConstants.IS_PAYMENT) {
            if (user.getMethod() == null || user.getMethod().equals(PaymentMethod.PAYMENT)) {
                String message = langService.getMessage(LangFields.ADD_CARD_NUMBER_TEXT, userId);
                if (user.getCardToken() != null) {
                    message = langService.getMessage(LangFields.REMOVE_CARD_NUMBER_TEXT, userId);
                }
                list.add(message);
            }
        }
        //transfer button
        if (AppConstants.IS_TRANSFER) {
            if (user.getMethod() == null || user.getMethod().equals(PaymentMethod.TRANSFER)) {
                list.add(langService.getMessage(LangFields.TRANSFER_BUTTON, userId));
            }
        }

        if (AppConstants.IS_CARD) {
            if (user.getMethod() == null || user.getMethod().equals(PaymentMethod.CARD)) {
                list.add(langService.getMessage(LangFields.PAY_CARD_NUMBER_TEXT, userId));
            }
        }

        if (user.isSubscribed()) {
            String history = langService.getMessage(LangFields.BUTTON_PAYMENT_HISTORY_TEXT, userId);

            //payment status
            String paymentStatus = langService.getMessage(LangFields.START_PAYMENT_TEXT, userId);
            if (user.isPayment()) {
                paymentStatus = langService.getMessage(LangFields.STOP_PAYMENT_TEXT, userId);
            }
            list.add(history);
            list.add(paymentStatus);
        }
//        String changeLang = langService.getMessage(LangFields.BUTTON_LANG_SETTINGS, userId);
        return withString(list);
    }

    @Override
    public ReplyKeyboard requestContact(Long userId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();
        //contact req
        KeyboardRow row1 = new KeyboardRow();
        KeyboardButton request = new KeyboardButton(langService.getMessage(LangFields.REQUEST_CONTACT_TEXT, userId));
        request.setRequestContact(true);
        row1.add(request);

        //back
        KeyboardButton back = new KeyboardButton(langService.getMessage(LangFields.BACK_TEXT, userId));
        KeyboardRow row2 = new KeyboardRow();
        row2.add(back);

        rows.add(row1);
        rows.add(row2);
        markup.setKeyboard(rows);
        return markup;
    }

    @Override
    public ReplyKeyboard adminMenu(Long userId, int adminLvl) {
        List<String> list = new LinkedList<>();
        if (adminLvl >= 5)
            list.add(langService.getMessage(LangFields.ADMINS_LIST_TEXT, userId));

        if (adminLvl >= 4)
            list.add(langService.getMessage(LangFields.ADD_WITH_TRANSFER_TEXT, userId));

        if (adminLvl >= 3)
            list.add(langService.getMessage(LangFields.SCREENSHOTS_LIST_TEXT, userId));

        if (adminLvl >= 2)
            list.add(langService.getMessage(LangFields.TRANSACTIONS_LIST_TEXT, userId));

        if (adminLvl >= 1)
            list.add(langService.getMessage(LangFields.USERS_LIST_TEXT, userId));

        list.add(langService.getMessage(LangFields.BACK_TEXT, userId));
        return withString(list);
    }
}
