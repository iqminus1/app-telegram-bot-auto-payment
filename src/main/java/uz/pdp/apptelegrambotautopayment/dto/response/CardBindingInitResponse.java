package uz.pdp.apptelegrambotautopayment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CardBindingInitResponse {
    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("phone")
    private String phone;
}
