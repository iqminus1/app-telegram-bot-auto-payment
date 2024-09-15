package uz.pdp.apptelegrambotautopayment.service;

import uz.pdp.apptelegrambotautopayment.enums.Lang;
import uz.pdp.apptelegrambotautopayment.enums.LangFields;

public interface LangService {
    String getMessage(LangFields keyword, Long userId);

    String getMessage(LangFields keyword, String text);

    Lang getLanguageEnum(String text);
}
