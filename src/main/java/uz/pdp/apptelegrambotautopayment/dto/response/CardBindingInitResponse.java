package uz.pdp.apptelegrambotautopayment.dto.response;

import lombok.Data;

@Data
public class CardBindingInitResponse {
    private String bindingId;
    private String url;
    private String message;
    // Другие поля, которые могут быть возвращены в ответе
}
