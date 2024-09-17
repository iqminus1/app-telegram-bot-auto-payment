package uz.pdp.apptelegrambotautopayment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

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
