package uz.pdp.apptelegrambotautopayment.service;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

public interface CallbackService {
    void process(CallbackQuery callbackQuery);
}
