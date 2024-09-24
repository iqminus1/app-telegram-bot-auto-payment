package uz.pdp.apptelegrambotautopayment.utils;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.pdp.apptelegrambotautopayment.enums.Lang;
import uz.pdp.apptelegrambotautopayment.enums.State;
import uz.pdp.apptelegrambotautopayment.model.Transaction;
import uz.pdp.apptelegrambotautopayment.model.User;
import uz.pdp.apptelegrambotautopayment.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
public class CommonUtils {
    private final ConcurrentMap<Long, User> users = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Transaction> transfers = new ConcurrentHashMap<>();
    private final UserRepository userRepository;

    public User getUser(Long userId) {
        return users.computeIfAbsent(userId, k ->
                userRepository.findById(userId).orElseGet(() ->
                        userRepository.save(User.builder()
                                .id(userId)
                                .state(State.START)
                                .lang(Lang.UZ)
                                .subscriptionEndTime(LocalDateTime.now())
                                .build())));
    }

    public State getState(Long userId) {
        return getUser(userId).getState();
    }

    public void setState(Long userId, State state) {
        User user = getUser(userId);
        if (user != null) {
            user.setState(state);
        }
    }

    public String getLang(Long userId) {
        return getUser(userId).getLang().toString();
    }

    public void setLang(Long userId, Lang lang) {
        User user = getUser(userId);
        user.setLang(lang);
        users.put(userId, user);
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void saveUsers() {
        List<User> list = users.values().stream().toList();
        userRepository.saveAll(list);
        users.clear();
    }


    public Transaction getTransaction(Long userId) {
        return transfers.computeIfAbsent(userId, k -> new Transaction());
    }

    public void removeTransaction(Long userid) {
        transfers.remove(userid);
    }

    @PreDestroy
    public void saveUtils() {
        userRepository.saveAll(users.values());
        users.clear();
    }
}
