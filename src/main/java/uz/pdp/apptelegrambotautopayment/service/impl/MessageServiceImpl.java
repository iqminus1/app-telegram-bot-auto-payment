package uz.pdp.apptelegrambotautopayment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import uz.pdp.apptelegrambotautopayment.dto.request.*;
import uz.pdp.apptelegrambotautopayment.dto.response.*;
import uz.pdp.apptelegrambotautopayment.enums.*;
import uz.pdp.apptelegrambotautopayment.model.Group;
import uz.pdp.apptelegrambotautopayment.model.Photo;
import uz.pdp.apptelegrambotautopayment.model.Transaction;
import uz.pdp.apptelegrambotautopayment.model.User;
import uz.pdp.apptelegrambotautopayment.repository.GroupRepository;
import uz.pdp.apptelegrambotautopayment.repository.PhotoRepository;
import uz.pdp.apptelegrambotautopayment.repository.TransactionRepository;
import uz.pdp.apptelegrambotautopayment.repository.UserRepository;
import uz.pdp.apptelegrambotautopayment.service.AtmosService;
import uz.pdp.apptelegrambotautopayment.service.ButtonService;
import uz.pdp.apptelegrambotautopayment.service.LangService;
import uz.pdp.apptelegrambotautopayment.service.MessageService;
import uz.pdp.apptelegrambotautopayment.service.telegram.Sender;
import uz.pdp.apptelegrambotautopayment.utils.AppConstants;
import uz.pdp.apptelegrambotautopayment.utils.CommonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import static uz.pdp.apptelegrambotautopayment.utils.AppConstants.getChatToString;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    private final CommonUtils commonUtils;
    private final LangService langService;
    private final ButtonService buttonService;
    private final Sender sender;
    private final AtmosService atmosService;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final PhotoRepository photoRepository;
    private final DecimalFormat decimalFormat;

    @Override
    public void process(Message message) {
        if (message.getChat().getType().equals("private")) {
            if (message.hasText()) {
                String text = message.getText();
                Long userId = message.getFrom().getId();
                User user = commonUtils.getUser(userId);
                if (user.getAgreed() == null || !user.getAgreed()) {
                    oferta(userId);
                    return;
                } else if (text.equals(AppConstants.START)) {
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
                            if (AppConstants.IS_CARD) {
                                commonUtils.setTariffId(userId, 3);
                                sendPayCardNumber(userId);
                            } else
                                sendAddCardNumberText(userId);
                        } else if (langService.getMessage(LangFields.BUTTON_PAYMENT_HISTORY_TEXT, userId).equals(text)) {
                            showPaymentHistory(userId);
                        } else if (langService.getMessage(LangFields.START_PAYMENT_TEXT, userId).equals(text)) {
                            startPayment(userId);
                        } else if (langService.getMessage(LangFields.STOP_PAYMENT_TEXT, userId).equals(text)) {
                            stopPayment(userId);
                        } else if (langService.getMessage(LangFields.TRANSFER_BUTTON, userId).equals(text)) {
                            sendTransferContactNumber(userId);
                        } else if (langService.getMessage(LangFields.PAY_CARD_NUMBER_TEXT, userId).equals(text)) {
                            sendPayCardNumber(userId);
                        } else if (langService.getMessage(LangFields.ADMIN_MENU_TEXT, userId).equals(text)) {
                            sendAdminMenu(userId);
                        } else
                            sender.sendMessage(userId, langService.getMessage(LangFields.USE_BUTTONS, userId));
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
                            sendAddCardNumberText(userId);
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

                    case PAY_CARD_NUMBER -> {
                        if (text.equals(langService.getMessage(LangFields.BACK_TEXT, userId)))
                            start(userId);
                        else
                            sender.sendMessage(userId, langService.getMessage(LangFields.EXCEPTION_ON_PAY_CARD_NUMBER_TEXT, userId));
                    }

                    case ADMIN_MENU -> {
                        if (text.equals(langService.getMessage(LangFields.BACK_TEXT, userId)))
                            start(userId);
                        else if (text.equals(langService.getMessage(LangFields.USERS_LIST_TEXT, userId))) {
                            usersList(userId);
                        } else if (text.equals(langService.getMessage(LangFields.TRANSACTIONS_LIST_TEXT, userId))) {
                            transactionsList(userId);
                        } else if (text.equals(langService.getMessage(LangFields.SCREENSHOTS_LIST_TEXT, userId))) {
                            screenshotsList(userId);
                        } else if (text.equals(langService.getMessage(LangFields.ADD_WITH_TRANSFER_TEXT, userId)))
                            addWithTransfer(userId);
                        else if (text.equals(langService.getMessage(LangFields.ADMINS_LIST_TEXT, userId))) {
                            adminsList(userId);
                        } else if (text.equals("Hisobot")) {
                            report(userId);
                        } else
                            sender.sendMessage(userId, langService.getMessage(LangFields.USE_BUTTONS, userId));
                    }

                    case SENDING_CONTACT_NUMBER -> start(userId);

                    case SELECT_PAYMENT_METHOD -> {
                        if (langService.getMessage(LangFields.BACK_TEXT, userId).equals(text))
                            sendAdminMenu(userId);
                        else if (AppConstants.IS_PAYMENT && langService.getMessage(LangFields.PAYMENT_METHOD_PAYMENT_TEXT, userId).equals(text))
                            sendPaymentsListWithStatus(userId, PaymentMethod.PAYMENT);
                        else if (AppConstants.IS_TRANSFER && langService.getMessage(LangFields.PAYMENT_METHOD_TRANSFER_TEXT, userId).equals(text))
                            sendPaymentsListWithStatus(userId, PaymentMethod.TRANSFER);
                        else if (AppConstants.IS_CARD && langService.getMessage(LangFields.PAYMENT_METHOD_CARD_TEXT, userId).equals(text))
                            sendPaymentsListWithStatus(userId, PaymentMethod.CARD);
                        else
                            sender.sendMessage(userId, langService.getMessage(LangFields.USE_BUTTONS, userId));
                    }
                    case SENDING_TRANSFER_USER_ID -> {
                        if (langService.getMessage(LangFields.BACK_TEXT, userId).equals(text))
                            sendAdminMenu(userId);
                        else
                            checkTransferUserId(message);
                    }
                    case SENDING_TRANSFER_USER_AMOUNT -> {
                        if (langService.getMessage(LangFields.BACK_TEXT, userId).equals(text))
                            addWithTransfer(userId);
                        else
                            checkTransferAmount(message);
                    }
                    case SENDING_TRANSFER_MONTHS_AMOUNT -> {
                        if (langService.getMessage(LangFields.BACK_TEXT, userId).equals(text))
                            backToTransferUserAmount(userId);
                        else
                            checkTransferMonths(message);
                    }
                    case SELECT_LANGUAGE -> changeLanguage(text, userId);
                }
            } else if (message.hasContact()) {
                if (commonUtils.getState(message.getFrom().getId()).equals(State.SENDING_CONTACT_NUMBER))
                    checkContact(message);
            } else if (message.hasPhoto()) {
                if (commonUtils.getState(message.getFrom().getId()).equals(State.PAY_CARD_NUMBER))
                    savePhoto(message);
            } else if (message.hasDocument()) {
                if (commonUtils.getState(message.getFrom().getId()).equals(State.PAY_CARD_NUMBER))
                    saveDocument(message);
            }
        }
    }

    private void report(Long userId) {
        String sb = "Botdan foydalangan userlar soni: " + userRepository.count() + "\n\n" +
                "Admin orqali tastiqlangan rasmlar soni: " +
                photoRepository.countByStatus(Status.ACCEPT);
        sender.sendMessage(userId, sb);
    }

    private void oferta(Long userId) {
        String message = langService.getMessage(LangFields.OFERTA_TEXT, userId);
        InlineKeyboardMarkup button = buttonService.ofertaButton(userId);
        sender.sendMessageWithMarkdown(userId, message, button);
    }

    private void saveDocument(Message message) {
        Long userId = message.getFrom().getId();
        Integer tariffId = commonUtils.getTariffId(userId);

        if (tariffId == null)
            tariffId = 3;

        Document document = message.getDocument();

        String filePath = sender.getFilePath(document.getFileId());
        savePhotoAndSendSuccess(userId, filePath, tariffId);
    }

    private void savePhotoAndSendSuccess(Long userId, String filePath, Integer tariffId) {
        photoRepository.save(new Photo(null, userId, filePath, Status.DONT_SEE, null, tariffId));

        commonUtils.setState(userId, State.START);
        commonUtils.removeTariffId(userId);
        sender.sendMessage(userId, langService.getMessage(LangFields.SCREENSHOT_SAVED_TEXT, userId), buttonService.start(userId));
    }

    private void checkTransferMonths(Message message) {
        Long userId = message.getFrom().getId();
        try {
            long month = Long.parseLong(message.getText());

            Transaction transaction = commonUtils.getTransaction(userId);
            transaction.setSuccessTransId(userId.toString());
            transaction.setPayAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            commonUtils.removeTransaction(userId);

            Long transferUserid = transaction.getUserId();
            User user = commonUtils.getUser(transferUserid);
            if (user.getSubscriptionEndTime().isBefore(LocalDateTime.now())) {
                user.setSubscriptionEndTime(LocalDateTime.now().plusMonths(month));
                List<Group> groups = groupRepository.findAll();
                if (groups.size() == 1) {
                    sender.sendMessage(transferUserid, langService.getMessage(LangFields.ACCEPTED_TRANSFER_TEXT, userId) + " " + langService.getMessage(LangFields.LINK_TEXT, userId) + sender.getLink(groups.get(0).getGroupId()));
                }
            } else {
                user.setSubscriptionEndTime(user.getSubscriptionEndTime().plusMonths(month));
                sender.sendMessage(transferUserid, langService.getMessage(LangFields.ACCEPTED_TRANSFER_TEXT, userId));
            }
            user.setMethod(PaymentMethod.TRANSFER);
            userRepository.save(user);

            commonUtils.setState(userId, State.ADMIN_MENU);
            sender.sendMessage(userId, langService.getMessage(LangFields.END_TRANSFER_TEXT, userId), buttonService.adminMenu(userId, commonUtils.getUser(userId).getAdmin()));
        } catch (NumberFormatException e) {
            sender.sendMessage(userId, langService.getMessage(LangFields.EXCEPTION_TRANSFER_NUMBER_TEXT, userId));
        }
    }

    private void backToTransferUserAmount(Long userId) {
        commonUtils.setState(userId, State.SENDING_TRANSFER_USER_AMOUNT);
        sender.sendMessage(userId, langService.getMessage(LangFields.SEND_TRANSFER_USER_AMOUNT_TEXT, userId));
    }

    private void checkTransferAmount(Message message) {
        Long userId = message.getFrom().getId();
        try {
            long amount = Long.parseLong(message.getText());

            Transaction transaction = commonUtils.getTransaction(userId);
            transaction.setAmount(amount);

            commonUtils.setState(userId, State.SENDING_TRANSFER_MONTHS_AMOUNT);
            sender.sendMessage(userId, langService.getMessage(LangFields.SEND_TRANSFER_USER_MONTHS_TEXT, userId));
        } catch (NumberFormatException e) {
            sender.sendMessage(userId, langService.getMessage(LangFields.EXCEPTION_TRANSFER_NUMBER_TEXT, userId));
        }
    }

    private void checkTransferUserId(Message message) {
        Long userId = message.getFrom().getId();
        try {
            long transferUserId = Long.parseLong(message.getText());
            if (!userRepository.existsById(transferUserId)) {
                sender.sendMessage(userId, langService.getMessage(LangFields.USER_IS_NOT_EXISTS_TEXT, userId));
                return;
            }

            Transaction transaction = commonUtils.getTransaction(userId);
            transaction.setMethod(PaymentMethod.TRANSFER);
            transaction.setUserId(transferUserId);

            commonUtils.setState(userId, State.SENDING_TRANSFER_USER_AMOUNT);
            sender.sendMessage(userId, langService.getMessage(LangFields.SEND_TRANSFER_USER_AMOUNT_TEXT, userId));
        } catch (NumberFormatException e) {
            sender.sendMessage(userId, langService.getMessage(LangFields.EXCEPTION_TRANSFER_NUMBER_TEXT, userId));
        }
    }


    private void sendPaymentsListWithStatus(Long userId, PaymentMethod method) {
        List<Transaction> transactions = transactionRepository.findAllByMethod(method);
        if (transactions.isEmpty()) {
            sender.sendMessage(userId, langService.getMessage(LangFields.EMPTY_PAYMENT_METHOD_LIST, userId));
            return;
        }
        for (Transaction transaction : transactions) {
            LocalDateTime payAt = transaction.getPayAt();
            sender.sendMessage(userId, getChatToString(sender.getChat(transaction.getUserId())) + "\n" + payAt.toLocalDate() + " " + payAt.toLocalTime() + " " + decimalFormat.format(transaction.getAmount()));
        }
    }


    private void adminsList(Long userId) {
        User user = commonUtils.getUser(userId);
        if (user.getAdmin() < 5) {
            sender.sendMessage(userId, langService.getMessage(LangFields.ADMIN_ACCESS_DENIED, userId).formatted(5, user.getAdmin()));
            return;
        }
        List<User> admins = userRepository.findAllByAdminAfter(0);
        StringBuilder sb = new StringBuilder();
        for (User admin : admins) {
            sb.append(getChatToString(sender.getChat(admin.getId()))).append(" - ").append(admin.getAdmin()).append("\n");
        }
        sender.sendMessage(userId, sb.toString());
    }

    private void addWithTransfer(Long userId) {
        if (!AppConstants.IS_TRANSFER) {
            return;
        }
        User user = commonUtils.getUser(userId);
        if (user.getAdmin() < 4) {
            sender.sendMessage(userId, langService.getMessage(LangFields.ADMIN_ACCESS_DENIED, userId).formatted(4, user.getAdmin()));
            return;
        }
        user.setState(State.SENDING_TRANSFER_USER_ID);
        sender.sendMessage(userId, langService.getMessage(LangFields.SEND_TRANSFER_USER_ID_TEXT, userId), buttonService.withString(List.of(langService.getMessage(LangFields.BACK_TEXT, userId))));
    }

    private void screenshotsList(Long userId) {
        if (!AppConstants.IS_CARD) {
            return;
        }
        User user = commonUtils.getUser(userId);
        if (user.getAdmin() < 3) {
            sender.sendMessage(userId, langService.getMessage(LangFields.ADMIN_ACCESS_DENIED, userId).formatted(3, user.getAdmin()));
            return;
        }
        List<Photo> photos = photoRepository.findAllByStatus(Status.DONT_SEE);
        if (photos.isEmpty()) {
            sender.sendMessage(userId, langService.getMessage(LangFields.EMPTY_SCREENSHOTS_LIST_TEXT, userId));
            return;
        }
        for (Photo photo : photos) {
            Long screenshotId = photo.getId();
            InlineKeyboardMarkup keyboard = buttonService.screenshotKeyboard(userId, screenshotId);
            String message = langService.getMessage(LangFields.UN_CHECKED_SCREENSHOT_TEXT, userId);
            try {
                FileTime lastModifiedTime = Files.getLastModifiedTime(Path.of(photo.getPath()));

                Instant instant = lastModifiedTime.toInstant();

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                        .withZone(ZoneId.of("Asia/Tashkent"));

                String formattedDate = formatter.format(instant);
                String tariff = "Oylik";
                if (photo.getTariff() == 2)
                    tariff = "2 oylik";
                else if (photo.getTariff() == 3)
                    tariff = "Bir martalik";
                message = message + "\n" + getChatToString(sender.getChat(photo.getSendUserId())) + "\n" + "Tariff: " + tariff + "\n" + formattedDate;
                sender.sendDocument(userId, message, photo.getPath(), keyboard);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void transactionsList(Long userId) {
        User user = commonUtils.getUser(userId);
        if (user.getAdmin() < 2) {
            sender.sendMessage(userId, langService.getMessage(LangFields.ADMIN_ACCESS_DENIED, userId).formatted(2, user.getAdmin()));
            return;
        }
        user.setState(State.SELECT_PAYMENT_METHOD);
        sender.sendMessage(userId, langService.getMessage(LangFields.CHOOSE_PAYMENT_METHOD_TEXT, userId), buttonService.paymentMethods(userId));
    }

    private void usersList(Long userId) {
        List<User> users = userRepository.findAllBySubscribed(true);
        if (users.isEmpty()) {
            sender.sendMessage(userId, langService.getMessage(LangFields.EMPTY_USERS_LIST_TEXT, userId));
            return;
        }

        String header = langService.getMessage(LangFields.HEADER_USERS_LIST_TEXT, userId);
        StringBuilder sb = new StringBuilder(header);
        for (int i = 0; i < users.size(); i += 10) {
            for (int j = 0; j < 10 && (i + j) < users.size(); j++) {
                User user = users.get(i + j);
                Chat chat = sender.getChat(user.getId());
                sb.append(getChatToString(chat)).append("\n");

                LocalDateTime end = user.getSubscriptionEndTime();
                sb.append(end.toLocalDate()).append(" ").append(end.toLocalTime()).append("\n\n");
            }
            sender.sendMessage(userId, sb.toString());
            sb.setLength(0);
        }
    }


    private void sendAdminMenu(Long userId) {
        User user = commonUtils.getUser(userId);
        if (user.getAdmin() == 0) {
            return;
        }
        commonUtils.setState(userId, State.ADMIN_MENU);
        sender.sendMessage(userId, langService.getMessage(LangFields.WELCOME_TO_ADMIN_MENU_TEXT, userId), buttonService.adminMenu(userId, user.getAdmin()));
    }

    private void savePhoto(Message message) {
        Long userId = message.getFrom().getId();
        Integer tariffId = commonUtils.getTariffId(userId);

        if (tariffId == null)
            tariffId = 3;

        List<PhotoSize> photo = message.getPhoto();
        if (photo.isEmpty()) {
            return;
        }
        PhotoSize photoSize = photo.stream().max(Comparator.comparing(PhotoSize::getFileSize)).get();
        String filePath = sender.getFilePath(photoSize.getFileId());
        savePhotoAndSendSuccess(userId, filePath, tariffId);
    }

    private void sendPayCardNumber(Long userId) {
        if (!AppConstants.IS_CARD)
            return;

        PaymentMethod method = commonUtils.getUser(userId).getMethod();
        if (method == null || method.equals(PaymentMethod.CARD)) {
            String text = langService.getMessage(LangFields.ADMIN_CARD_NUMBER_TEXT, userId);
            String price = decimalFormat.format(AppConstants.PRICE_ONCE);
            if (commonUtils.getTariffId(userId) == 3) {
                price = decimalFormat.format(AppConstants.PRICE_UNLIMITED);
            } else if (commonUtils.getTariffId(userId) == 2)
                price = decimalFormat.format(AppConstants.PRICE_TWICE);

            String som = langService.getMessage(LangFields.SOM_TEXT, userId);
            String withCardNumber = text.formatted(AppConstants.CARD_NAME, AppConstants.CARD_NUMBER, (price + " " + som));
            ReplyKeyboard backButton = buttonService.withString(List.of(langService.getMessage(LangFields.BACK_TEXT, userId)));
            sender.sendMessageWithMarkdown(userId, withCardNumber, backButton);
            commonUtils.setState(userId, State.PAY_CARD_NUMBER);
        }

    }

    private void sendTransferContactNumber(Long userId) {
        if (!AppConstants.IS_TRANSFER)
            return;

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
        int i = 0;
        StringBuilder sb = new StringBuilder();
        int size = transactions.size();
        String som = langService.getMessage(LangFields.SOM_TEXT, userId);
        for (Transaction transaction : transactions) {
            sb.append(size - i++).append(". ")
                    .append(transaction.getPayAt().toLocalDate()).append(" ")
                    .append(transaction.getPayAt().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                    .append(" - ").append(decimalFormat.format(transaction.getAmount())).append(" ").append(som).append("\n");
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
            sendAddCardNumberText(userId);
            return;
        }
        sender.sendMessage(userId, langService.getMessage(LangFields.SEND_YOUR_PHONE_NUMBER_TEXT, userLang), buttonService.requestContact(userId));

    }

    private void removeUserCard(Long userId) {
        User user = commonUtils.getUser(userId);
        if (!user.getMethod().equals(PaymentMethod.PAYMENT)) {
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
        sender.sendMessage(userId, langService.getMessage(LangFields.CARD_NUMBER_DELETED_TEXT, userId), buttonService.start(userId));
    }

    private void checkCardCode(Long userId, String text) {
        User user = commonUtils.getUser(userId);

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
        user.setMethod(PaymentMethod.PAYMENT);
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
        List<Group> groups = groupRepository.findAll();
        if (groups.size() == 1) {
            String link = sender.getLink(groups.get(0).getGroupId());
            sender.sendMessage(userId, langService.getMessage(LangFields.SEND_VALID_ORDER_TEXT, userId) + link, buttonService.start(userId));
        }
        //apply end
    }

    private void exceptionAtmos(Long userId, String errorMessage) {
        commonUtils.setState(userId, State.START);
        sender.sendMessage(userId, langService.getMessage(LangFields.EXCEPTION_ATMOS_TEXT, userId).formatted(errorMessage), buttonService.start(userId));
    }

    private void sendingCardExpire(Long userId, String text) {
        if (text.matches("^(0[1-9]|1[0-2])(\\d{2}$)")) {
            User user = commonUtils.getUser(userId);
            String str = text.substring(2) + text.substring(0, 2);
            CardBindingInitResponse cardBindingInitResponse = atmosService.initializeCardBinding(new CardBindingInitRequest(user.getCardNumber(), str));
            if (cardBindingInitResponse.getTransactionId() != null) {
                commonUtils.setState(userId, State.SENDING_CARD_CODE);
                user.setCardExpiry(text);
                user.setTransactionId(cardBindingInitResponse.getTransactionId());
                user.setCardPhone(cardBindingInitResponse.getPhone());
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
        sender.sendMessage(userId, langService.getMessage(LangFields.SEND_CARD_EXPIRE_TEXT, userId));
    }

    private void sendAddCardNumberText(Long userId) {
        if (!AppConstants.IS_PAYMENT)
            return;

        User user = commonUtils.getUser(userId);
        if (user.getContactNumber() == null) {
            sendContactNumber(userId);
            return;
        }
        if (user.getMethod() != null) {
            return;
        }
        commonUtils.setState(userId, State.START);
        String message = langService.getMessage(LangFields.SEND_CARD_NUMBER_TEXT, userId);
        ReplyKeyboard replyKeyboard = buttonService.withWebApp(userId);
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
        commonUtils.setState(userId, State.START);
        commonUtils.removeTransaction(userId);
        sender.sendPhoto(userId, langService.getMessage(LangFields.HELLO, userId), AppConstants.PHOTO_PATH, buttonService.start(userId));
    }
}
