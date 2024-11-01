package uz.pdp.apptelegrambotautopayment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import uz.pdp.apptelegrambotautopayment.enums.LangFields;
import uz.pdp.apptelegrambotautopayment.enums.PaymentMethod;
import uz.pdp.apptelegrambotautopayment.enums.State;
import uz.pdp.apptelegrambotautopayment.enums.Status;
import uz.pdp.apptelegrambotautopayment.model.Group;
import uz.pdp.apptelegrambotautopayment.model.Photo;
import uz.pdp.apptelegrambotautopayment.model.Transaction;
import uz.pdp.apptelegrambotautopayment.model.User;
import uz.pdp.apptelegrambotautopayment.repository.GroupRepository;
import uz.pdp.apptelegrambotautopayment.repository.PhotoRepository;
import uz.pdp.apptelegrambotautopayment.repository.TransactionRepository;
import uz.pdp.apptelegrambotautopayment.repository.UserRepository;
import uz.pdp.apptelegrambotautopayment.service.CallbackService;
import uz.pdp.apptelegrambotautopayment.service.LangService;
import uz.pdp.apptelegrambotautopayment.service.MessageService;
import uz.pdp.apptelegrambotautopayment.service.telegram.Sender;
import uz.pdp.apptelegrambotautopayment.utils.AppConstants;
import uz.pdp.apptelegrambotautopayment.utils.CommonUtils;

import java.time.LocalDateTime;
import java.util.List;

import static uz.pdp.apptelegrambotautopayment.utils.AppConstants.getChatToString;
import static uz.pdp.apptelegrambotautopayment.utils.AppConstants.setSubscriptionTime;

@Service
@RequiredArgsConstructor
public class CallbackServiceImpl implements CallbackService {
    private final CommonUtils commonUtils;
    private final PhotoRepository photoRepository;
    private final LangService langService;
    private final Sender sender;
    private final TransactionRepository transactionRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;

    @Override
    public void process(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        if (data.equals(AppConstants.OFERTA_I_AGREE_DATA)) {
            setAgree(callbackQuery);
        } else if (commonUtils.getState(callbackQuery.getFrom().getId()).equals(State.ADMIN_MENU)) {
            if (data.startsWith(AppConstants.ACCEPT_SCREENSHOT_DATA)) {
                acceptScreenshot(callbackQuery);
            } else if (data.startsWith(AppConstants.REJECT_SCREENSHOT_DATA))
                rejectScreenshot(callbackQuery);
        }
    }

    private void setAgree(CallbackQuery callbackQuery) {
        Long userId = callbackQuery.getFrom().getId();
        User user = commonUtils.getUser(userId);
        user.setAgreed(true);
        userRepository.save(user);
        Integer messageId = callbackQuery.getMessage().getMessageId();
        sender.deleteMessage(userId, messageId);
        Message message = new Message();
        message.setChat(callbackQuery.getMessage().getChat());
        message.setFrom(callbackQuery.getFrom());
        message.setText("/start");
        messageService.process(message);
    }

    private void rejectScreenshot(CallbackQuery callbackQuery) {
        Integer messageId = callbackQuery.getMessage().getMessageId();
        Long userId = callbackQuery.getFrom().getId();
        long photoId = Long.parseLong(callbackQuery.getData().split(":")[1]);
        Photo screenshot = updatePhoto(photoId, Status.REJECT);
        String message = langService.getMessage(LangFields.REJECTED_SCREENSHOT_TEXT, userId);
        String tariff = "Oylik";
        if (screenshot.getTariff() == 2)
            tariff = "2 oylik";
        else if (screenshot.getTariff() == 3)
            tariff = "Bir martalik";
        message = message + "\n" + getChatToString(sender.getChat(userId)) + "\n" + "Tariff: " + tariff;
        sender.changeCaption(userId, messageId, message);

        sender.sendMessage(screenshot.getSendUserId(), langService.getMessage(LangFields.SCREENSHOT_IS_INVALID_TEXT, screenshot.getSendUserId()));
    }

    private void acceptScreenshot(CallbackQuery callbackQuery) {
        Integer messageId = callbackQuery.getMessage().getMessageId();
        Long userId = callbackQuery.getFrom().getId();
        long photoId = Long.parseLong(callbackQuery.getData().split(":")[1]);
        Photo screenshot = updatePhoto(photoId, Status.ACCEPT);
        String message = langService.getMessage(LangFields.ACCEPTED_SCREENSHOT_TEXT, userId);
        String tariff = "Oylik";
        if (screenshot.getTariff() == 2)
            tariff = "2 oylik";
        else if (screenshot.getTariff() == 3)
            tariff = "Bir martalik";
        message = message + "\n" + getChatToString(sender.getChat(userId)) + "\n" + "Tariff: " + tariff;
        sender.changeCaption(userId, messageId, message);

        Transaction transaction = new Transaction(null, null, screenshot.getId().toString(), screenshot.getSendUserId(), AppConstants.PRICE_ONCE, LocalDateTime.now(), PaymentMethod.CARD);
        if (screenshot.getTariff() == 2)
            transaction.setAmount(AppConstants.PRICE_TWICE);
        else if (screenshot.getTariff() == 3)
            transaction.setAmount(AppConstants.PRICE_UNLIMITED);

        transactionRepository.save(transaction);

        User user = commonUtils.getUser(screenshot.getSendUserId());
        user.setMethod(PaymentMethod.CARD);
        if (screenshot.getTariff() != 3)
            userRepository.save(setSubscriptionTime(user, screenshot.getTariff()));
        else userRepository.save(setSubscriptionTime(user, 12000));

        List<Group> groups = groupRepository.findAll();
        if (groups.size() == 1) {
            sender.sendMessage(user.getId(), langService.getMessage(LangFields.SCREENSHOT_IS_VALID_TEXT, screenshot.getSendUserId()) + " -> " + sender.getLink(groups.get(0).getGroupId()));
        }
    }


    private Photo updatePhoto(long photoId, Status status) {
        Photo photo = photoRepository.findById(photoId).orElseThrow();
        photo.setStatus(status);
        photo.setActiveAt(LocalDateTime.now());
        return photoRepository.save(photo);
    }
}
