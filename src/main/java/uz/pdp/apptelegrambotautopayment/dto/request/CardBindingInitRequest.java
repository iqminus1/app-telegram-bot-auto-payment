package uz.pdp.apptelegrambotautopayment.dto.request;

import lombok.Data;

@Data
public class CardBindingInitRequest {
    private String cardNumber;
    private String cardHolder;
    private String expiryDate;
    // Другие необходимые поля
}
