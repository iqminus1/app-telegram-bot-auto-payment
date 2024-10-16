package uz.pdp.apptelegrambotautopayment.model;

import jakarta.persistence.*;
import lombok.*;
import uz.pdp.apptelegrambotautopayment.enums.Status;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
@Entity
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sendUserId;

    private String path;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDateTime activeAt;

    private Integer tariff;
}
