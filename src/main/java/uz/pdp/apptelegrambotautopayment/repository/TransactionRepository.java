package uz.pdp.apptelegrambotautopayment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.pdp.apptelegrambotautopayment.enums.PaymentMethod;
import uz.pdp.apptelegrambotautopayment.model.Transaction;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByUserIdOrderByPayAtDesc(Long userId);

    boolean existsByUserId(Long userId);

    List<Transaction> findAllByMethod(PaymentMethod method);

    List<Transaction> findAllByUserId(Long userId);

    @Query("SELECT t FROM Transaction t WHERE FUNCTION('DATE_FORMAT', t.payAt, '%m/%Y') = :monthYear")
    List<Transaction> findAllByYearMonth(@Param("monthYear") String monthYear);
}