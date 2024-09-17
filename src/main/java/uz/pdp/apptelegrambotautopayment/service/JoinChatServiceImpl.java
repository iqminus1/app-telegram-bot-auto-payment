package uz.pdp.apptelegrambotautopayment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.ChatJoinRequest;
import uz.pdp.apptelegrambotautopayment.enums.LangFields;
import uz.pdp.apptelegrambotautopayment.model.Group;
import uz.pdp.apptelegrambotautopayment.model.Order;
import uz.pdp.apptelegrambotautopayment.repository.GroupRepository;
import uz.pdp.apptelegrambotautopayment.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JoinChatServiceImpl implements JoinChatService {

    private final OrderRepository orderRepository;
    private final Sender sender;
    private final LangService langService;
    private final GroupRepository groupRepository;


    @Override
    public void process(ChatJoinRequest chatJoinRequest) {
        Long groupId = chatJoinRequest.getChat().getId();
        Optional<Group> groupOptional = groupRepository.findByGroupId(groupId);
        if (groupOptional.isEmpty()) {
            return;
        }
        Long userId = chatJoinRequest.getUser().getId();
        String name = chatJoinRequest.getChat().getTitle();
        Optional<Order> orderOptional = orderRepository.findById(userId);

        if (orderOptional.isPresent()) {
            Order order = orderOptional.get();
            if (order.getExpireAt().isBefore(LocalDateTime.now())) {
                sender.sendMessage(userId, langService.getMessage(LangFields.PAID_GROUP_TEXT, chatJoinRequest.getUser().getLanguageCode()).formatted(name));
            } else {
                sender.deleteInviteLink(groupId, chatJoinRequest.getInviteLink().getInviteLink());
                sender.acceptJoinRequest(userId, groupId);
            }
        } else {
            sender.openChat(userId, groupId);
            sender.sendMessage(userId, langService.getMessage(LangFields.PAID_GROUP_TEXT, chatJoinRequest.getUser().getLanguageCode()).formatted(name));
        }
    }

}
