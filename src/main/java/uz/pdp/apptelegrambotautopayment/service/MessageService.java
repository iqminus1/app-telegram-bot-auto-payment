package uz.pdp.apptelegrambotautopayment.service;

import org.telegram.telegrambots.meta.api.objects.Message;

public interface MessageService {
    void process(Message message);
}
