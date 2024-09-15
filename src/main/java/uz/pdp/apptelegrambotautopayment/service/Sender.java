package uz.pdp.apptelegrambotautopayment.service;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.pdp.apptelegrambotautopayment.utils.AppConstants;

@Component
public class Sender extends DefaultAbsSender {
    public Sender() {
        super(new DefaultBotOptions(), AppConstants.BOT_TOKEN);
    }

    public void sendMessage(Long userId,String text){
        try {
            execute(new SendMessage(userId.toString(),text));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
