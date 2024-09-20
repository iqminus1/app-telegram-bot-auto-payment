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
public class ApplyResponse {
    @JsonProperty("success_trans_id")
    private String successTransId;

    @JsonProperty("trans_id")
    private String transId;

    @JsonProperty("account")
    private Long userId;

    @JsonProperty("amount")
    private Long amount;

    private String errorCode;

    private String errorMessage;
}
