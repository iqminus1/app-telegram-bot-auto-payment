package uz.pdp.apptelegrambotautopayment.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.pdp.apptelegrambotautopayment.dto.TransactionDto;
import uz.pdp.apptelegrambotautopayment.model.Group;
import uz.pdp.apptelegrambotautopayment.model.Photo;
import uz.pdp.apptelegrambotautopayment.model.Transaction;
import uz.pdp.apptelegrambotautopayment.model.User;
import uz.pdp.apptelegrambotautopayment.repository.GroupRepository;
import uz.pdp.apptelegrambotautopayment.repository.PhotoRepository;
import uz.pdp.apptelegrambotautopayment.repository.TransactionRepository;
import uz.pdp.apptelegrambotautopayment.repository.UserRepository;
import uz.pdp.apptelegrambotautopayment.service.telegram.Sender;
import uz.pdp.apptelegrambotautopayment.utils.AppConstants;
import uz.pdp.apptelegrambotautopayment.utils.CommonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/transaction")
public class TransactionController {
    private final TransactionRepository transactionRepository;
    private final Sender sender;
    private final GroupRepository groupRepository;
    private final CommonUtils commonUtils;
    private final UserRepository userRepository;
    private final PhotoRepository photoRepository;

    @GetMapping("/read-all")
    public ResponseEntity<?> readAll() {
        return ResponseEntity.ok(transactionRepository.findAll());
    }

    @GetMapping("/read/{id}")
    public ResponseEntity<?> read(@PathVariable Long id) {
        return ResponseEntity.ok(transactionRepository.findById(id).orElseThrow());
    }

    @GetMapping("/read-all-by-user-id/{userId}")
    public ResponseEntity<?> readAllByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(transactionRepository.findAllByUserId(userId));
    }

    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthly(@RequestParam int year, @RequestParam int month) {
        LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endDate = startDate.plusMonths(1);
        List<Transaction> result = transactionRepository.findAllByYearAndMonth(startDate, endDate);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/photo/{id}")
    public void read(@PathVariable Long id, HttpServletResponse resp) {
        Photo photo = photoRepository.findById(id).orElseThrow();
        try {
            Path path = Path.of(photo.getPath());
            Files.copy(path, resp.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/create")

    public ResponseEntity<?> create(@RequestBody TransactionDto dto) {
        Transaction transaction = new Transaction();
        updateEntity(transaction, dto);
        if (transaction.getPayAt() == null)
            transaction.setPayAt(LocalDateTime.now());
        List<Group> groups = groupRepository.findAll();
        if (groups.size() != 1) {
            return ResponseEntity.status(400).body("Method don`t work");
        }
        Group group = groups.get(0);
        String link = sender.getLink(group.getGroupId());
        sender.sendMessage(dto.getUserId(), link);
        User user = commonUtils.getUser(dto.getUserId());
        AppConstants.setSubscriptionTime(user);
        userRepository.save(user);
        transactionRepository.save(transaction);
        return ResponseEntity.ok("Transaction saved");
    }


    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestParam TransactionDto dto) {
        Transaction transaction = transactionRepository.findById(id).orElseThrow();
        updateEntity(transaction, dto);
        if (transaction.getPayAt() == null)
            transaction.setPayAt(LocalDateTime.now());
        transactionRepository.save(transaction);
        return ResponseEntity.ok("successfully updated");
    }


    private void updateEntity(Transaction transaction, TransactionDto dto) {
        transaction.setAmount(dto.getAmount());
        transaction.setMethod(dto.getMethod());
        transaction.setUserId(dto.getUserId());
        transaction.setSuccessTransId(dto.getSuccessTransId());
        transaction.setTransId(dto.getTransId());
        transaction.setPayAt(dto.getPayAt());
    }
}
