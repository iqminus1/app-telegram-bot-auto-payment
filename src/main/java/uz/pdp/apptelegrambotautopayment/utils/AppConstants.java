package uz.pdp.apptelegrambotautopayment.utils;

public interface AppConstants {
    String BOT_TOKEN = "7320858493:AAFtIOr8bofMTKFuMjegu8SVuxxrdTMYagI";
    String BOT_USERNAME = "manager_groups_v1_bot";
    String ATMOS_API_KEY = "*****";
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

}
