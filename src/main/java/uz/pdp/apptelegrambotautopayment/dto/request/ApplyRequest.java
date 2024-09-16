
package uz.pdp.apptelegrambotautopayment.dto.request;

import lombok.Data;

@Data
public class ApplyRequest {
    private String transactionId;
    private String amount;
    // Другие необходимые поля
}
