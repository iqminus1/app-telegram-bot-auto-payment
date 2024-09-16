package uz.pdp.apptelegrambotautopayment.dto.response;

import lombok.Data;

@Data
public class PreApplyResponse {
    private String preApplyId;
    private String status;
    private String message;
    // Другие поля, которые могут быть возвращены в ответе
}
