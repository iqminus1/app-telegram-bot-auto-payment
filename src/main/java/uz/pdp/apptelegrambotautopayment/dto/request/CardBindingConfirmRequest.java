package uz.pdp.apptelegrambotautopayment.dto.request;

import lombok.Data;

@Data
public class CardBindingConfirmRequest {
    private String bindingId;
    private String confirmationCode;
    // Другие необходимые поля
}
