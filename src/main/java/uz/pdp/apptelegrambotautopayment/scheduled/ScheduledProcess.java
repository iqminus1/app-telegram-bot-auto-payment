package uz.pdp.apptelegrambotautopayment.scheduled;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.pdp.apptelegrambotautopayment.dto.request.CardRequest;
import uz.pdp.apptelegrambotautopayment.dto.response.ApplyResponse;
import uz.pdp.apptelegrambotautopayment.enums.LangFields;
import uz.pdp.apptelegrambotautopayment.model.Group;
import uz.pdp.apptelegrambotautopayment.model.Transaction;
import uz.pdp.apptelegrambotautopayment.model.User;
import uz.pdp.apptelegrambotautopayment.repository.GroupRepository;
import uz.pdp.apptelegrambotautopayment.repository.TransactionRepository;
import uz.pdp.apptelegrambotautopayment.repository.UserRepository;
import uz.pdp.apptelegrambotautopayment.service.AtmosService;
import uz.pdp.apptelegrambotautopayment.service.LangService;
import uz.pdp.apptelegrambotautopayment.service.Sender;
import uz.pdp.apptelegrambotautopayment.utils.AppConstants;
import uz.pdp.apptelegrambotautopayment.utils.CommonUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ScheduledProcess {
    private final UserRepository userRepository;
    private final AtmosService atmosService;
    private final Sender sender;
    private final GroupRepository groupRepository;
    private final TransactionRepository transactionRepository;
    private final CommonUtils commonUtils;
    private final LangService langService;

    @Scheduled(fixedRate = 30, timeUnit = TimeUnit.SECONDS)
    public void getPayment() {
        //Save qivolib userlani keyin paymant yechiladi.
        //State tudum sudmlari o`zgarib ketmasligi uchun.
        commonUtils.saveUsers();
        List<Group> groups = groupRepository.findAll();
        LocalDateTime localDateTime = LocalDateTime.now().minusDays(3);
        if (groups.size() == 1) {
            Long groupId = groups.get(0).getGroupId();
            List<User> users = userRepository.findAllBySubscribedAndSubscriptionEndTimeIsBefore(true, LocalDateTime.now());
            for (User user : users) {
                Long userId = user.getId();
                if (!sender.checkChatMember(userId, groupId)) {
                    notChatMember(user);
                } else if (user.getCardToken() == null) {
                    kickUserAndSendMessage(user, groupId, LangFields.REMOVED_CARD_NUMBER_TEXT);
                } else if (!user.isPayment()) {
                    kickUserAndSendMessage(user, groupId, LangFields.STOPPED_PAYMENT_END_ORDER_TEXT);
                } else {
                    if (localDateTime.isBefore(user.getSubscriptionEndTime()))
                        kickUserAndSendMessage(user, groupId, LangFields.DONT_PAY_WITHIN_DAYS_TEXT);
                    else
                        withdrawMoney(user);
                }
            }
        }
    }

    private void withdrawMoney(User user) {
        ApplyResponse applyResponse = atmosService.autoPayment(user.getId());
        if (applyResponse.getSuccessTransId() == null) {
            return;
        }
        Transaction transaction = new Transaction(applyResponse);
        transactionRepository.save(transaction);
        AppConstants.setSubscriptionTime(user);
        userRepository.save(user);
        commonUtils.updateUser(user);
        sender.sendMessage(user.getId(), langService.getMessage(LangFields.YOU_PAID_TEXT, user.getId()));
    }

    @Async
    void kickUserAndSendMessage(User user, Long groupId, LangFields field) {
        sender.kickChatMember(user.getId(), groupId);
        user.setSubscribed(false);
        userRepository.save(user);
        commonUtils.updateUser(user);
        sender.sendMessage(user.getId(), langService.getMessage(field, user.getId()));

    }

    private void notChatMember(User user) {
        clearUser(user);
        userRepository.save(user);
        commonUtils.updateUser(user);
        sender.sendMessage(user.getId(), langService.getMessage(LangFields.NOT_CHAT_MEMBER_TEXT, user.getId()));
    }

    private void clearUser(User user) {
        atmosService.removeCard(new CardRequest(user.getCardId(), user.getCardToken()));
        user.setSubscribed(false);
        user.setCardToken(null);
        user.setCardExpiry(null);
        user.setCardId(null);
        user.setCardNumber(null);
    }
}