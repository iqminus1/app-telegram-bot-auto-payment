package uz.pdp.apptelegrambotautopayment.dto.response;

import lombok.Data;

@Data
public class ApplyResponse {
    private String applyId;
    private String status;
    private String message;
    // Другие поля, которые могут быть возвращены в ответе
}
