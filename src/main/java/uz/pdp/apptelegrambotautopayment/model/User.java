package uz.pdp.apptelegrambotautopayment.model;

import jakarta.persistence.*;
import lombok.*;
import uz.pdp.apptelegrambotautopayment.enums.Lang;
import uz.pdp.apptelegrambotautopayment.enums.PaymentMethod;
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

    @Enumerated(EnumType.STRING)
    private State state;

    private Lang lang;

//    @Transient
    private String cardNumber;

//    @Transient
    private String cardExpiry;

    private String transactionId;

    private Long cardId;

    private String cardToken;

    private String cardPhone;

    private String contactNumber;

    private boolean subscribed;

    private LocalDateTime subscriptionEndTime;

    private boolean payment;

    private int admin;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    private Boolean agreed;
}
