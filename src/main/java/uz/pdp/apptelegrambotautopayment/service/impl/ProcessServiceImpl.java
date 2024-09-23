package uz.pdp.apptelegrambotautopayment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.pdp.apptelegrambotautopayment.service.JoinChatService;
import uz.pdp.apptelegrambotautopayment.service.MessageService;
import uz.pdp.apptelegrambotautopayment.service.MyChatMemberService;
import uz.pdp.apptelegrambotautopayment.service.ProcessService;

@Service
@RequiredArgsConstructor
public class ProcessServiceImpl implements ProcessService {
    private final MessageService messageService;
    private final JoinChatService joinChatService;
    private final MyChatMemberService myChatMemberService;

    @Override
    public void process(Update update) {
        if (update.hasMessage()) {
            messageService.process(update.getMessage());
        } else if (update.hasChatJoinRequest()) {
            joinChatService.process(update.getChatJoinRequest());
        } else if (update.hasMyChatMember()) {
            myChatMemberService.process(update.getMyChatMember());
        }
    }
}
