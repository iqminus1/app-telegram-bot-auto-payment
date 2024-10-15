package uz.pdp.apptelegrambotautopayment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.pdp.apptelegrambotautopayment.enums.PaymentMethod;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserDto {
    private Long id;
    private String contactNumber;
    private int admin;
    private boolean subscribed;
    private LocalDateTime subscriptionEndTime;
    private PaymentMethod method;
}
