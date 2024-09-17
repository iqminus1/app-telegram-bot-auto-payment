
package uz.pdp.apptelegrambotautopayment.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.pdp.apptelegrambotautopayment.utils.AppConstants;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ApplyRequest {
    @Setter
    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("store_id")
    private final Integer STORE_ID = AppConstants.STORE_ID;

    @JsonProperty("otp")
    private final Integer otp = 111111;
}
