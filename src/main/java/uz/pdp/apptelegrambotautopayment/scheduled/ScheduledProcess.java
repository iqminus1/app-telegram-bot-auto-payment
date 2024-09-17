package uz.pdp.apptelegrambotautopayment.scheduled;

import lombok.RequiredArgsConstructor;
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
        List<Group> groups = groupRepository.findAll();
        if (groups.size() == 1) {
            Long groupId = groups.get(0).getGroupId();
            List<User> users = userRepository.findAllBySubscribedAndSubscriptionEndTimeIsBefore(true, LocalDateTime.now());
            for (User user : users) {
                Long userId = user.getId();
                if (!sender.checkChatMember(userId, groupId)) {
                    clearUser(user);
                    userRepository.save(user);
                    commonUtils.updateUser(user);
                    sender.sendMessage(userId, langService.getMessage(LangFields.REMOVED_ANY_TEXT, userId));
                    return;
                }
                if (user.getCardToken() == null) {
                    sender.kickChatMember(userId, groupId);
                    user.setSubscribed(false);
                    userRepository.save(user);
                    commonUtils.updateUser(user);
                    sender.sendMessage(userId, langService.getMessage(LangFields.CARD_TOKEN_NULL_TEXT, userId));
                    return;
                }
                ApplyResponse applyResponse = atmosService.autoPayment(userId);
                if (applyResponse.getSuccessTransId() == null) {
                    sender.kickChatMember(userId, groupId);
                    clearUser(user);
                    sender.sendMessage(userId, langService.getMessage(LangFields.YOU_ARE_KICKED_TEXT, userId));
                    return;
                } else {

                    transactionRepository.save(new Transaction(applyResponse));
                    AppConstants.setSubscriptionTime(user);
                    User commonUser = commonUtils.getUser(userId);
                    commonUser.setSubscriptionEndTime(user.getSubscriptionEndTime());
                    userRepository.save(user);
                    sender.sendMessage(userId, langService.getMessage(LangFields.YOU_PAID_TEXT, userId));
                    commonUtils.updateUser(user);
                }
            }
        }
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
