package uz.pdp.apptelegrambotautopayment.dto.request;

import lombok.Data;

@Data
public class TransactionRequest {
    private String amount;
    private String currency;
    private String description;
    // Другие необходимые поля
}
