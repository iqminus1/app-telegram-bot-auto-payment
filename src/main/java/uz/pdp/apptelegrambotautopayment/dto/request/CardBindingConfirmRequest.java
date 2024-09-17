package uz.pdp.apptelegrambotautopayment.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CardBindingConfirmRequest {
    @JsonProperty("otp")
    private String otp;

    @JsonProperty("transaction_id")
    private String transactionId;
}

