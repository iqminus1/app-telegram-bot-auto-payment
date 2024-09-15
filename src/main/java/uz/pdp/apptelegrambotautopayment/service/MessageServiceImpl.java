package uz.pdp.apptelegrambotautopayment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import uz.pdp.apptelegrambotautopayment.enums.LangFields;
import uz.pdp.apptelegrambotautopayment.enums.State;
import uz.pdp.apptelegrambotautopayment.model.User;
import uz.pdp.apptelegrambotautopayment.utils.AppConstants;
import uz.pdp.apptelegrambotautopayment.utils.CommonUtils;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    private final CommonUtils commonUtils;
    private final LangService langService;
    private final Sender sender;

    @Override
    public void process(Message message) {
        if (message.hasText()) {
            String text = message.getText();
            if (text.equals(AppConstants.START)) {
                start(message);
                return;
            }
            Long userId = message.getFrom().getId();
        }
    }

    private void start(Message message) {
        Long userId = message.getFrom().getId();
        User user = commonUtils.getUser(userId);
        user.setState(State.START);
        sender.sendMessage(userId, langService.getMessage(LangFields.HELLO, userId));
    }
}
