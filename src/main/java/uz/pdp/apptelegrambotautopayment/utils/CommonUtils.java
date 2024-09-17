package uz.pdp.apptelegrambotautopayment.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.pdp.apptelegrambotautopayment.enums.Lang;
import uz.pdp.apptelegrambotautopayment.enums.State;
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
    private final UserRepository userRepository;

    public void updateUser(User user) {
        users.put(user.getId(), user);
    }

    public User getUser(Long userId) {
        return users.getOrDefault(userId,
                userRepository.findById(userId).orElseGet(() ->
                        userRepository.save(User.builder().id(userId).state(State.START).lang(Lang.UZ).subscriptionEndTime(LocalDateTime.now()).build())));
    }

    public State getState(Long userId) {
        return users.get(userId).getState();
    }

    public void setState(Long userId, State state) {
        User user = getUser(userId);
        if (user != null) {
            user.setState(state);
            updateUser(user);
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
}
