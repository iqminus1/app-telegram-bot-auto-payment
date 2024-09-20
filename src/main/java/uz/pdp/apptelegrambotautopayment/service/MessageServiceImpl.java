package uz.pdp.apptelegrambotautopayment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import uz.pdp.apptelegrambotautopayment.dto.request.*;
import uz.pdp.apptelegrambotautopayment.dto.response.*;
import uz.pdp.apptelegrambotautopayment.enums.Lang;
import uz.pdp.apptelegrambotautopayment.enums.LangFields;
import uz.pdp.apptelegrambotautopayment.enums.PaymentMethod;
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

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
                        } else if (langService.getMessage(LangFields.BUTTON_PAYMENT_HISTORY_TEXT, userId).equals(text)) {
                            showPaymentHistory(userId);
                        } else if (langService.getMessage(LangFields.START_PAYMENT_TEXT, userId).equals(text)) {
                            startPayment(userId);
                        } else if (langService.getMessage(LangFields.STOP_PAYMENT_TEXT, userId).equals(text)) {
                            stopPayment(userId);
                        } else if (langService.getMessage(LangFields.TRANSFER_BUTTON, userId).equals(text)) {
                            sendTransferContactNumber(userId);
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
                        if (text.matches("\\d{1,}"))
                            checkCardCode(userId, text);
                        else
                            sender.sendMessage(userId, langService.getMessage(LangFields.SEND_OTP_NOT_DIGIT_TEXT, userId));
                    }

                    case SENDING_CONTACT_NUMBER -> start(userId);

                    case SELECT_LANGUAGE -> changeLanguage(text, userId);
                }
            } else if (message.hasContact()) {
                if (commonUtils.getState(message.getFrom().getId()).equals(State.SENDING_CONTACT_NUMBER))
                    checkContact(message);
            }
        }
    }

    private void sendTransferContactNumber(Long userId) {
        PaymentMethod method = commonUtils.getUser(userId).getMethod();
        if (method == null || method.equals(PaymentMethod.TRANSFER))
            sender.sendMessage(userId, langService.getMessage(LangFields.CONTACT_TRANSFER_TEXT, userId));
    }

    private void stopPayment(Long userId) {
        User user = commonUtils.getUser(userId);
        if (!user.isPayment())
            return;
        user.setPayment(false);
        userRepository.save(user);
        commonUtils.updateUser(user);
        sender.sendMessage(userId, langService.getMessage(LangFields.PAYMENT_IS_STOPPED_TEXT, userId), buttonService.start(userId));
    }

    private void startPayment(Long userId) {
        User user = commonUtils.getUser(userId);
        if (user.getCardToken() == null) {
            sendAddCardNumberText(userId);
            return;
        }
        if (user.isPayment())
            return;
        user.setPayment(true);
        boolean b = false;
        if (user.getSubscriptionEndTime().isBefore(LocalDateTime.now())) {
            AppConstants.setSubscriptionTime(user);
            b = true;
        }
        userRepository.save(user);
        commonUtils.updateUser(user);
        sender.sendMessage(userId, langService.getMessage(LangFields.PAYMENT_IS_STARTED_TEXT, userId), buttonService.start(userId));
        List<Group> groups = groupRepository.findAll();
        if (b)
            if (groups.size() == 1 && groups.get(0).getGroupId() != null)
                sender.sendMessage(userId, langService.getMessage(LangFields.PAYMENT_IS_STARTED_AND_PAID_THE_GROUP, userId) + sender.getLink(groups.get(0).getGroupId()));
    }

    private void showPaymentHistory(Long userId) {
        List<Transaction> transactions = transactionRepository.findAllByUserIdOrderByPayAtDesc(userId);
        if (transactions.isEmpty()) {
            sender.sendMessage(userId, langService.getMessage(LangFields.EMPTY_PAYMENT_HISTORY_TEXT, userId));
            return;
        }
        DecimalFormat decimalFormat = new DecimalFormat("###,###,###");
        int i = 0;
        StringBuilder sb = new StringBuilder();
        int size = transactions.size();
        String som = langService.getMessage(LangFields.SOM_TEXT, userId);
        for (Transaction transaction : transactions) {
            sb.append(size - i++).append(". ")
                    .append(transaction.getPayAt().toLocalDate()).append(" ")
                    .append(transaction.getPayAt().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                    .append(" - ").append(decimalFormat.format(transaction.getAmount() / 100)).append(" ").append(som).append("\n");
        }
        String message = langService.getMessage(LangFields.LIST_PAYMENT_HISTORY_TEXT, userId);
        sender.sendMessage(userId, message + sb);
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
        if (!user.getMethod().equals(PaymentMethod.CARD)) {
            return;
        }
        CardRemovalResponse cardRemovalResponse = atmosService.removeCard(new CardRequest(user.getCardId(), user.getCardToken()));
        if (cardRemovalResponse.getErrorCode() != null) {
            sender.sendMessage(userId, langService.getMessage(LangFields.EXCEPTION_ATMOS_TEXT, userId).formatted(cardRemovalResponse.getErrorMessage()));
            return;
        }
        user.setCardNumber(null);
        user.setCardId(null);
        user.setCardExpiry(null);
        user.setCardToken(null);
        user.setCardPhone(null);
        user.setMethod(null);
        userRepository.save(user);
        commonUtils.updateUser(user);
        sender.sendMessage(userId, langService.getMessage(LangFields.CARD_NUMBER_DELETED_TEXT, userId), buttonService.start(userId));
    }

    private void checkCardCode(Long userId, String text) {
        User user = temp.getUser(userId);

        //card confirm start
        CardBindingConfirmResponse confirmResponse = atmosService.confirmCardBinding(new CardBindingConfirmRequest(text, user.getTransactionId()));
        if (confirmResponse.getErrorMessage() != null) {
//            exceptionAtmos(userId, confirmResponse.getErrorMessage());
            if (confirmResponse.getErrorCode().equals("098"))
                sender.sendMessage(userId, langService.getMessage(LangFields.SEND_OTP_TEXT, userId));
            else
                sender.sendMessage(userId, langService.getMessage(LangFields.EXCEPTION_ATMOS_TEXT, userId).formatted(confirmResponse.getErrorMessage()));
            return;
        }

        user.setCardToken(confirmResponse.getCardToken());
        user.setCardId(confirmResponse.getCardId());
        user.setCardPhone(confirmResponse.getPhone());
        user.setMethod(PaymentMethod.CARD);
        temp.removeUser(userId);
        commonUtils.updateUser(user);
        userRepository.save(user);
        //card confirm end

        if (user.getSubscriptionEndTime().isAfter(LocalDateTime.now())) {
            commonUtils.setState(userId, State.START);
            sender.sendMessage(userId, langService.getMessage(LangFields.DONT_END_PERMISSION_TEXT, userId).formatted(user.getSubscriptionEndTime().toLocalDate().plusDays(1)), buttonService.start(userId));
            return;
        }

        //transaction create start
        TransactionResponse transaction = atmosService.createTransaction(new TransactionRequest(userId));
        if (transaction.getErrorMessage() != null) {
            exceptionAtmos(userId, transaction.getErrorMessage());
            sendAddCardNumberText(userId);
            return;
        }
        String transactionId = transaction.getTransactionId();
        //transaction create end

        //pre apply
        atmosService.preApplyPayment(new PreApplyRequest(transactionId, user.getCardToken()));

        //apply start
        ApplyResponse applyResponse = atmosService.applyPayment(new ApplyRequest(transactionId));
        if (applyResponse.getErrorMessage() != null) {
            exceptionAtmos(userId, applyResponse.getErrorMessage());
            sendAddCardNumberText(userId);
            return;
        }
        sender.sendMessage(userId, langService.getMessage(LangFields.YOU_PAID_TEXT, userId));
        transactionRepository.save(new Transaction(applyResponse));
        AppConstants.setSubscriptionTime(user);
        user.setState(State.START);
        user.setPayment(true);
        userRepository.save(user);
        commonUtils.updateUser(user);
        List<Group> groups = groupRepository.findAll();
        if (groups.size() == 1) {
            String link = sender.getLink(groups.get(0).getGroupId());
            sender.sendMessage(userId, langService.getMessage(LangFields.SEND_VALID_ORDER_TEXT, userId) + link, buttonService.start(userId));
        }
        //apply end
    }

    private void exceptionAtmos(Long userId, String errorMessage) {
        temp.removeUser(userId);
        commonUtils.setState(userId, State.START);
        sender.sendMessage(userId, langService.getMessage(LangFields.EXCEPTION_ATMOS_TEXT, userId).formatted(errorMessage), buttonService.start(userId));
    }

    private void sendingCardExpire(Long userId, String text) {
        if (text.matches("^(0[1-9]|1[0-2])(\\d{2}$)")) {
            User user = temp.getUser(userId);
            String str = text.substring(2) + text.substring(0, 2);
            CardBindingInitResponse cardBindingInitResponse = atmosService.initializeCardBinding(new CardBindingInitRequest(user.getCardNumber(), str));
            if (cardBindingInitResponse.getTransactionId() != null) {
                commonUtils.setState(userId, State.SENDING_CARD_CODE);
                user.setCardExpiry(text);
                user.setTransactionId(cardBindingInitResponse.getTransactionId());
                user.setCardPhone(cardBindingInitResponse.getPhone());
                temp.setUser(user);
                sender.sendMessage(userId, langService.getMessage(LangFields.SEND_CARD_CODE_TEXT, userId).formatted(cardBindingInitResponse.getPhone()), new ReplyKeyboardRemove(true));
                return;
            }
            if (cardBindingInitResponse.getErrorMessage() != null) {
                exceptionAtmos(userId, cardBindingInitResponse.getErrorMessage());
                sendAddCardNumberText(userId);
            }
            return;
        }
        sender.sendMessage(userId, langService.getMessage(LangFields.EXCEPTION_CARD_EXPIRE_TEXT, userId));
    }

    private void sendingCardNumber(Long userId, String text) {
        commonUtils.setState(userId, State.SENDING_CARD_EXPIRE);
        User user = commonUtils.getUser(userId);
        if (text.matches("\\d{16}")) {
            setCardAndSendMessage(userId, text, user);
            return;
        } else if (text.matches("\\d{4} \\d{4} \\d{4} \\d{4}")) {
            String formattedCardNumber = text.substring(0, 4) +
                    text.substring(5, 9) +
                    text.substring(10, 14) +
                    text.substring(15);
            setCardAndSendMessage(userId, formattedCardNumber, user);
            return;
        }
        sender.sendMessage(userId, langService.getMessage(LangFields.EXCEPTION_CARD_NUMBER_TEXT, userId));
    }

    private void setCardAndSendMessage(Long userId, String formattedCardNumber, User user) {
        if (userRepository.existsByCardNumber(formattedCardNumber)) {
            sender.sendMessage(userId, langService.getMessage(LangFields.INVALID_CARD_NUMBER_TEXT, userId));
            return;
        }
        user.setCardNumber(formattedCardNumber);
        temp.setUser(user);
        sender.sendMessage(userId, langService.getMessage(LangFields.SEND_CARD_EXPIRE_TEXT, userId));
    }

    private void sendAddCardNumberText(Long userId) {
        if (commonUtils.getUser(userId).getContactNumber() == null) {
            commonUtils.setState(userId, State.SENDING_CARD_NUMBER);
            sendContactNumber(userId);
            return;
        }
        if (commonUtils.getUser(userId).getMethod() != null) {
            return;
        }
        commonUtils.setState(userId, State.SENDING_CARD_NUMBER);
        temp.removeUser(userId);
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

//    private void selectLanguage(long userId) {
//        commonUtils.setState(userId, State.SELECT_LANGUAGE);
//        String message = langService.getMessage(LangFields.BUTTON_LANG_SETTINGS, userId);
//        sender.sendMessage(userId, message, buttonService.language(userId));
//    }

    private void start(Long userId) {
        User user = commonUtils.getUser(userId);
        user.setState(State.START);
        commonUtils.updateUser(user);
        temp.removeUser(userId);
        sender.sendMessage(userId, langService.getMessage(LangFields.HELLO, userId), buttonService.start(userId));
    }
}
