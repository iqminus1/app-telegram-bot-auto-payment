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
public class TransactionRequest {
    @JsonProperty("amount")
    private final Long amount = AppConstants.PRICE * 100;

    @JsonProperty("terminal_id")
    private final String terminalId = null;

    @JsonProperty("store_id")
    private final Integer storeId = AppConstants.STORE_ID;

    @JsonProperty("lang")
    private final String lang = "ru";

    @Setter
    @JsonProperty("account")
    private Long userId;
}
