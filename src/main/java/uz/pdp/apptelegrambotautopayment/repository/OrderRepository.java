package uz.pdp.apptelegrambotautopayment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.pdp.apptelegrambotautopayment.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}