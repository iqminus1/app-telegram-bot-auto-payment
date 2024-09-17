package uz.pdp.apptelegrambotautopayment.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import uz.pdp.apptelegrambotautopayment.dto.response.ApplyResponse;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String successTransId;

    private String transId;

    private Long userId;

    private Long amount;

    public Transaction(ApplyResponse applyResponse) {
        this.amount = applyResponse.getAmount();
        this.userId = applyResponse.getUserId();
        this.transId = applyResponse.getTransId();
        this.successTransId = applyResponse.getSuccessTransId();
    }
}
