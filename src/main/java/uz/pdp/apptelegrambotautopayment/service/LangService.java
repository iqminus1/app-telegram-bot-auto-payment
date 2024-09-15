package uz.pdp.apptelegrambotautopayment.service;

import uz.pdp.apptelegrambotautopayment.enums.LangFields;

public interface LangService {
    String getMessage(LangFields langFields, Long userId);
}
