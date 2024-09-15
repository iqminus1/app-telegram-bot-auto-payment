package uz.pdp.apptelegrambotautopayment.service;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface ProcessService {
    void process(Update update);
}
