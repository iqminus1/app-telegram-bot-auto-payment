package uz.pdp.apptelegrambotautopayment.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
@Entity(name = "orders")
public class Order {
    @Id
    private Long userId;

    private LocalDateTime expireAt;
}
