package uz.pdp.apptelegrambotautopayment.dto.response;

import lombok.Data;

@Data
public class CardRemovalResponse {
    private String status;
    private String message;
    // Другие поля, которые могут быть возвращены в ответе
}
