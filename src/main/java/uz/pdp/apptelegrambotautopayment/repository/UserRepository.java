package uz.pdp.apptelegrambotautopayment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pdp.apptelegrambotautopayment.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
}