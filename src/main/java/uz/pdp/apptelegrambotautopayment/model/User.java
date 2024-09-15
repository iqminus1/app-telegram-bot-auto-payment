package uz.pdp.apptelegrambotautopayment.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import uz.pdp.apptelegrambotautopayment.enums.Lang;
import uz.pdp.apptelegrambotautopayment.enums.State;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
@Entity(name = "users")
@Builder
public class User {
    @Id
    private Long id;

    private State state;

    private Lang lang;

    private String cardNumber;

    private String cardExpiry;

    private String cardCvv;

    private boolean subscribed;

    private LocalDateTime subscriptionEndTime;
}
