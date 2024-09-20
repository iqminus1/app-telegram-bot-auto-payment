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
public class CardRemovalResponse {
    @JsonProperty("card_id")
    private Long cardId;

    @JsonProperty("pan")
    private String pan;

    @JsonProperty("expiry")
    private String expiry;

    @JsonProperty("card_holder")
    private String cardHolder;

    @JsonProperty("balance")
    private Long balance;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("card_token")
    private String cardToken;
    String status;

    private String message;

    private String errorCode;

    private String errorMessage;
}
