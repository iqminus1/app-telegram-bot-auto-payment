package uz.pdp.apptelegrambotautopayment.utils;

import uz.pdp.apptelegrambotautopayment.model.User;

import java.time.LocalDateTime;

public interface AppConstants {
    String BOT_TOKEN = "6027918055:AAHfPXcPeBad31_qqqrndyqc5fjpTcqAuG0";
    String BOT_USERNAME = "upload_your_work_bot";
    String START = "/start";
    Long PRICE = 1000L;

    //Auth Atmos
    String CLIENT_ID = "your_client_id";
    String CLIENT_SECRET = "your_client_secret";

    //ATMOS API URLs
    String ATMOS_AUTH_URL = "https://partner.atmos.uz/token";
    String ATMOS_BIND_CARD_INIT_URL = "https://partner.atmos.uz/partner/bind-card/init";
    String ATMOS_BIND_CARD_CONFIRM_URL = "https://partner.atmos.uz/partner/bind-card/confirm";
    String ATMOS_CREATE_TRANSACTION_URL = "https://partner.atmos.uz/merchant/pay/create";
    String ATMOS_PRE_APPLY_URL = "https://partner.atmos.uz/merchant/pay/pre-apply";
    String ATMOS_APPLY_URL = "https://partner.atmos.uz/merchant/pay/apply-ofd";
    String ATMOS_REMOVE_CARD_URL = "https://partner.atmos.uz/partner/remove-card";

    Integer STORE_ID = 7977;
    long SUBSCRIPTION_MONTH = 1;
    String ERROR_TEXT = "STPIMS-ERR-";
    int ERROR_LENGTH = ERROR_TEXT.length();
    String SET_ADMIN_CODE = "2456FA55fJ1235GKNBNMKAU";

    static User setSubscriptionTime(User user) {
        if (user.getSubscriptionEndTime().isBefore(LocalDateTime.now())) {
            user.setSubscriptionEndTime(LocalDateTime.now().plusMinutes(SUBSCRIPTION_MONTH));
        } else
            user.setSubscriptionEndTime(user.getSubscriptionEndTime().plusMinutes(SUBSCRIPTION_MONTH));
        return user;
    }

}
