package uz.pdp.apptelegrambotautopayment.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ApplyResponse {
    @JsonProperty("success_trans_id")
    private String successTransId;

    @JsonProperty("trans_id")
    private String transId;

    @JsonProperty("account")
    private Long userId;

    @JsonProperty("amount")
    private Long amount;
}
