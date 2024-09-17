package uz.pdp.apptelegrambotautopayment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import uz.pdp.apptelegrambotautopayment.dto.request.*;
import uz.pdp.apptelegrambotautopayment.dto.response.*;
import uz.pdp.apptelegrambotautopayment.enums.Lang;
import uz.pdp.apptelegrambotautopayment.enums.LangFields;
import uz.pdp.apptelegrambotautopayment.enums.State;
import uz.pdp.apptelegrambotautopayment.model.Group;
import uz.pdp.apptelegrambotautopayment.model.Transaction;
import uz.pdp.apptelegrambotautopayment.model.User;
import uz.pdp.apptelegrambotautopayment.repository.GroupRepository;
import uz.pdp.apptelegrambotautopayment.repository.TransactionRepository;
import uz.pdp.apptelegrambotautopayment.repository.UserRepository;
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
    private final AtmosService atmosService;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    @Override
    public void process(Message message) {
        if (message.getChat().getType().equals("private")) {
            if (message.hasText()) {
                String text = message.getText();
                Long userId = message.getFrom().getId();
                if (text.equals(AppConstants.START)) {
                    start(userId);
                    return;
                }
                switch (commonUtils.getState(userId)) {
                    case START -> {
//                        if (langService.getMessage(LangFields.BUTTON_LANG_SETTINGS, userId).equals(text)) {
//                            selectLanguage(userId);
//                        }
                        if (langService.getMessage(LangFields.REMOVE_CARD_NUMBER_TEXT, userId).equals(text)) {
                            removeUserCard(userId);
                        } else if (langService.getMessage(LangFields.ADD_CARD_NUMBER_TEXT, userId).equals(text)) {
                            sendAddCardNumberText(userId);
//                        sendWebAppForPayment(userId);
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
                        checkCardCode(userId, text);
                    }
                    case SENDING_CONTACT_NUMBER -> {
                        start(userId);
                    }
                    case SELECT_LANGUAGE -> changeLanguage(text, userId);
                }
            } else if (message.hasContact()) {
                if (commonUtils.getState(message.getFrom().getId()).equals(State.SENDING_CONTACT_NUMBER))
                    checkContact(message);
            }
        }
    }

    private void checkContact(Message message) {
        Long userId = message.getFrom().getId();
        String userLang = commonUtils.getLang(userId);
        if (message.getContact().getUserId().equals(message.getChat().getId())) {
            String phoneNumber = message.getContact().getPhoneNumber();
            User user = commonUtils.getUser(userId);
            user.setContactNumber(phoneNumber);
            user.setState(State.START);
            userRepository.save(user);
            commonUtils.updateUser(user);
            sendAddCardNumberText(userId);
            return;
        }
        sender.sendMessage(userId, langService.getMessage(LangFields.SEND_YOUR_PHONE_NUMBER_TEXT, userLang), buttonService.requestContact(userId));

    }

    private void removeUserCard(Long userId) {
        User user = commonUtils.getUser(userId);
        if (user.getCardToken() == null) {
            return;
        }
        CardRemovalResponse cardRemovalResponse = atmosService.removeCard(new CardRequest(user.getCardId(), user.getCardToken()));
        if (cardRemovalResponse.getCardToken() != null) {
            return;
        }
        user.setCardNumber(null);
        user.setCardId(null);
        user.setCardExpiry(null);
        user.setCardToken(null);
        userRepository.save(user);
        commonUtils.updateUser(user);
        sender.sendMessage(userId, langService.getMessage(LangFields.CARD_NUMBER_DELETED_TEXT, userId), buttonService.start(userId));
    }

    private void checkCardCode(Long userId, String text) {
        User user = temp.getUser(userId);
        CardBindingConfirmResponse cardBindingConfirmResponse = atmosService.confirmCardBinding(new CardBindingConfirmRequest(text, user.getTransactionId()));
//        if (cardBindingConfirmResponse.getBalance() < (AppConstants.PRICE * 100)) {
//            sender.sendMessage(userId, langService.getMessage(LangFields.NOT_ENOUGH_MONEY_TEXT, userId));
//            return;
//        }
        user.setCardToken(cardBindingConfirmResponse.getCardToken());
        user.setCardId(cardBindingConfirmResponse.getCardId());
        commonUtils.updateUser(user);
        TransactionResponse transaction = atmosService.createTransaction(new TransactionRequest(userId));
        String transactionId = transaction.getTransactionId();
        atmosService.preApplyPayment(new PreApplyRequest(transactionId, user.getCardToken()));
        ApplyResponse applyResponse = atmosService.applyPayment(new ApplyRequest(transactionId));
        if (applyResponse.getSuccessTransId() != null) {
            sender.sendMessage(userId, langService.getMessage(LangFields.YOU_PAID_TEXT, userId));
            transactionRepository.save(new Transaction(applyResponse));
            AppConstants.setSubscriptionTime(user);
            user.setState(State.START);
            userRepository.save(user);
            commonUtils.updateUser(user);
            List<Group> groups = groupRepository.findAll();
            if (groups.size() == 1) {
                String link = sender.getLink(groups.get(0).getGroupId());
                sender.sendMessage(userId, langService.getMessage(LangFields.SEND_VALID_ORDER_TEXT, userId) + link, buttonService.start(userId));
            }
        }
    }

    private void sendWebAppForPayment(Long userId) {
        String text = langService.getMessage(LangFields.FOR_ADD_CARD_NUMBER_TEXT, userId);
        String buttonText = langService.getMessage(LangFields.ADD_CARD_TEXT, userId);
        InlineKeyboardMarkup markup = buttonService.webAppKeyboard(buttonText, AppConstants.WEB_APP_URL.formatted(userId));
        sender.sendMessage(userId, text, markup);
    }

    private void sendingCardExpire(Long userId, String text) {
        if (text.matches("^(0[1-9]|1[0-2])(\\d{2}$)")) {
            User user = temp.getUser(userId);
            commonUtils.setState(userId, State.SENDING_CARD_CODE);
            String str = text.substring(2) + text.substring(0, 2);
            CardBindingInitResponse cardBindingInitResponse = atmosService.initializeCardBinding(new CardBindingInitRequest(user.getCardNumber(), str));
            if (cardBindingInitResponse.getTransactionId() != null) {
                user.setCardExpiry(text);
                user.setTransactionId(cardBindingInitResponse.getTransactionId());
                temp.setUser(user);
                sender.sendMessage(userId, langService.getMessage(LangFields.SEND_CARD_CODE_TEXT, userId).formatted(cardBindingInitResponse.getPhone()), new ReplyKeyboardRemove(true));
            }
            return;
        }
        sender.sendMessage(userId, langService.getMessage(LangFields.EXCEPTION_CARD_EXPIRE_TEXT, userId));
    }

    private void sendingCardNumber(Long userId, String text) {
        commonUtils.setState(userId, State.SENDING_CARD_EXPIRE);
        User user = commonUtils.getUser(userId);
        if (text.matches("\\d{16}")) {
            if (userRepository.existsByCardNumber(text)) {
                sender.sendMessage(userId, langService.getMessage(LangFields.INVALID_CARD_NUMBER_TEXT, userId));
                return;
            }
            user.setCardNumber(text);
            temp.setUser(user);
            sender.sendMessage(userId, langService.getMessage(LangFields.SEND_CARD_EXPIRE_TEXT, userId));
            return;
        } else if (text.matches("\\d{4} \\d{4} \\d{4} \\d{4}")) {
            String formattedCardNumber = text.substring(0, 4) +
                    text.substring(5, 9) +
                    text.substring(10, 14) +
                    text.substring(15);
            if (userRepository.existsByCardNumber(formattedCardNumber)) {
                sender.sendMessage(userId, langService.getMessage(LangFields.INVALID_CARD_NUMBER_TEXT, userId));
                return;
            }
            user.setCardNumber(formattedCardNumber);
            temp.setUser(user);
            sender.sendMessage(userId, langService.getMessage(LangFields.SEND_CARD_EXPIRE_TEXT, userId));
            return;
        }
        sender.sendMessage(userId, langService.getMessage(LangFields.EXCEPTION_CARD_NUMBER_TEXT, userId));
    }

    private void sendAddCardNumberText(Long userId) {
        if (commonUtils.getUser(userId).getContactNumber() == null) {
            commonUtils.setState(userId, State.SENDING_CARD_NUMBER);
            sendContactNumber(userId);
            return;
        }
        if (commonUtils.getUser(userId).getCardToken() != null)
            return;
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

    private void sendContactNumber(Long userId) {
        commonUtils.setState(userId, State.SENDING_CONTACT_NUMBER);
        ReplyKeyboard replyKeyboard = buttonService.requestContact(userId);
        sender.sendMessage(userId, langService.getMessage(LangFields.SEND_CONTACT_TEXT, userId), replyKeyboard);
    }

    private void changeLanguage(String text, Long userId) {
        Lang lang = langService.getLanguageEnum(text);
        String userLang = commonUtils.getLang(userId);
        commonUtils.setState(userId, State.START);
        if (lang == null) {
            sender.sendMessage(userId, langService.getMessage(LangFields.EXCEPTION_LANGUAGE, userLang), buttonService.start(userId));
            return;
        }
        commonUtils.setLang(userId, lang);
        sender.sendMessage(userId, langService.getMessage(LangFields.SUCCESSFULLY_CHANGED_LANGUAGE, lang.name()), buttonService.start(userId));
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
        sender.sendMessage(userId, langService.getMessage(LangFields.HELLO, userId), buttonService.start(userId));
    }
}
