package uz.pdp.apptelegrambotautopayment.utils;

import org.telegram.telegrambots.meta.api.objects.Chat;
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

    boolean IS_PAYMENT = true;

    boolean IS_TRANSFER = true;

    boolean IS_CARD = true;

    String CARD_NUMBER = "9860 0000 0000 0000";
    String CARD_NAME = "Qodirov Abdulaziz";
    String FILE_PATH = "C:/Users/User/IdeaProjects/app-telegram-bot-auto-payment/files/";
    String ACCEPT_SCREENSHOT_DATA = "acceptScreenshot:";
    String REJECT_SCREENSHOT_DATA = "rejectScreenshot:";
    String PHOTO_PATH = "C:\\Users\\User\\IdeaProjects\\app-telegram-bot-auto-payment\\files/first_photo.jpg";
    String OFERTA_LINK = "https://daryo.uz/";

    static User setSubscriptionTime(User user) {
        return setSubscriptionTime(user, 1);
    }

    static User setSubscriptionTime(User user, Integer month) {
        if (user.getSubscriptionEndTime().isBefore(LocalDateTime.now())) {
            user.setSubscriptionEndTime(LocalDateTime.now().plusMonths(month));
        } else
            user.setSubscriptionEndTime(user.getSubscriptionEndTime().plusMonths(month));
        return user;
    }

    static String getChatToString(Chat chat) {
        StringBuilder sb = new StringBuilder();
        sb.append("#").append(chat.getId());
        if (chat.getUserName() != null) {
            sb.append(" @").append(chat.getUserName());
        }
        if (chat.getFirstName() != null) {
            sb.append(" ").append(chat.getFirstName());
        }
        return sb.toString();
    }

}
