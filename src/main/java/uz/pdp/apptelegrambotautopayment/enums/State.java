package uz.pdp.apptelegrambotautopayment.enums;

public enum State {
    START,
    SELECT_LANGUAGE,
    SENDING_CARD_NUMBER,
    SENDING_CARD_EXPIRE,
    SENDING_CARD_CODE,
    SENDING_CONTACT_NUMBER, PAY_CARD_NUMBER, SENDING_PAY_SCREENSHOT,
}
