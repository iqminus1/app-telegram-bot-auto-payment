package uz.pdp.apptelegrambotautopayment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.pdp.apptelegrambotautopayment.model.Photo;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {
}