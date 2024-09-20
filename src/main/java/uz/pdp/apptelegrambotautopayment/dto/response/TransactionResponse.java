package uz.pdp.apptelegrambotautopayment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TransactionResponse {
    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("amount")
    private Long amount;

    @JsonProperty("account")
    private Long userId;

    private String errorCode;

    private String errorMessage;
}
