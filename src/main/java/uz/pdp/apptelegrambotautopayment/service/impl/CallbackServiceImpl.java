package uz.pdp.apptelegrambotautopayment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
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

    @Override
    public void process(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        if (commonUtils.getState(callbackQuery.getFrom().getId()).equals(State.ADMIN_MENU)) {
            if (data.startsWith(AppConstants.ACCEPT_SCREENSHOT_DATA)) {
                acceptScreenshot(callbackQuery);
            } else if (data.startsWith(AppConstants.REJECT_SCREENSHOT_DATA))
                rejectScreenshot(callbackQuery);
        }
    }

    private void rejectScreenshot(CallbackQuery callbackQuery) {
        Integer messageId = callbackQuery.getMessage().getMessageId();
        Long userId = callbackQuery.getFrom().getId();
        long photoId = Long.parseLong(callbackQuery.getData().split(":")[1]);
        Photo screenshot = updatePhoto(photoId, Status.REJECT);
        String message = langService.getMessage(LangFields.REJECTED_SCREENSHOT_TEXT, userId);
        message = message + "\n" + getChatToString(sender.getChat(userId));
        sender.changeCaption(userId, messageId, message);

        sender.sendMessage(userId, langService.getMessage(LangFields.SCREENSHOT_IS_INVALID_TEXT, screenshot.getSendUserId()));
    }

    private void acceptScreenshot(CallbackQuery callbackQuery) {
        Integer messageId = callbackQuery.getMessage().getMessageId();
        Long userId = callbackQuery.getFrom().getId();
        long photoId = Long.parseLong(callbackQuery.getData().split(":")[1]);
        Photo screenshot = updatePhoto(photoId, Status.ACCEPT);
        String message = langService.getMessage(LangFields.ACCEPTED_SCREENSHOT_TEXT, userId);
        message = message + "\n" + getChatToString(sender.getChat(userId));
        sender.changeCaption(userId, messageId, message);

        Transaction transaction = new Transaction(null, null, screenshot.getId().toString(), userId, AppConstants.PRICE, LocalDateTime.now(), PaymentMethod.CARD);
        transactionRepository.save(transaction);

        User user = commonUtils.getUser(screenshot.getSendUserId());
        user.setMethod(PaymentMethod.CARD);
        userRepository.save(setSubscriptionTime(user));

        List<Group> groups = groupRepository.findAll();
        if (groups.size() == 1) {
            sender.sendMessage(userId, langService.getMessage(LangFields.SCREENSHOT_IS_VALID_TEXT, screenshot.getSendUserId()) + " -> " + sender.getLink(groups.get(0).getGroupId()));
        }
    }


    private Photo updatePhoto(long photoId, Status status) {
        Photo photo = photoRepository.findById(photoId).orElseThrow();
        photo.setStatus(status);
        photo.setActiveAt(LocalDateTime.now());
        return photoRepository.save(photo);
    }
}
