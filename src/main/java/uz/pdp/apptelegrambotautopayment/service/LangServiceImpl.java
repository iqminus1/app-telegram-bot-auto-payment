package uz.pdp.apptelegrambotautopayment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import uz.pdp.apptelegrambotautopayment.enums.Lang;
import uz.pdp.apptelegrambotautopayment.enums.LangFields;
import uz.pdp.apptelegrambotautopayment.utils.CommonUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LangServiceImpl implements LangService {
    private final MessageSource messageSource;
    private final CommonUtils commonUtils;

    @Override
    public String getMessage(LangFields keyword, Long userId) {
        String lang = commonUtils.getLang(userId);
        try {
            return messageSource.getMessage(keyword.name(), null, new Locale(lang));
        } catch (Exception e) {
            return messageSource.getMessage(keyword.name(), null, new Locale(Lang.RU.name()));
        }
    }

    public String getMessage(LangFields keyword, String lang) {
        try {
            return messageSource.getMessage(keyword.name(), null, new Locale(lang));
        } catch (Exception e) {
            return messageSource.getMessage(keyword.name(), null, new Locale(Lang.RU.name()));
        }
    }

    @Override
    public Lang getLanguageEnum(String text) {
        if (text.equals(getMessage(LangFields.BUTTON_LANGUAGE_UZBEK, "uz"))) {
            return Lang.UZ;
        } else if (text.equals(getMessage(LangFields.BUTTON_LANGUAGE_RUSSIAN, "uz"))) {
            return Lang.RU;
        } else if (text.equals(getMessage(LangFields.BUTTON_LANGUAGE_ENGLISH, "uz"))) {
            return Lang.EN;
        }
        return null;
    }
}
