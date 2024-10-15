package uz.pdp.apptelegrambotautopayment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.pdp.apptelegrambotautopayment.enums.PaymentMethod;
import uz.pdp.apptelegrambotautopayment.model.Transaction;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByUserIdOrderByPayAtDesc(Long userId);

    List<Transaction> findAllByMethod(PaymentMethod method);

    List<Transaction> findAllByUserId(Long userId);

    @Query("SELECT t FROM Transaction t WHERE t.payAt >= :startDate AND t.payAt < :endDate")
    List<Transaction> findAllByYearAndMonth(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}