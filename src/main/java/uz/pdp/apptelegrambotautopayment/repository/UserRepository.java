package uz.pdp.apptelegrambotautopayment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pdp.apptelegrambotautopayment.model.User;

import java.time.LocalDateTime;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findAllBySubscribedAndSubscriptionEndTimeIsBefore(boolean subscribed, LocalDateTime subscriptionEndTime);

    boolean existsByCardNumber(String cardNumber);
}