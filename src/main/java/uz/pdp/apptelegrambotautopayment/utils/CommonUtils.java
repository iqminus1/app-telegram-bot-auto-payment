package uz.pdp.apptelegrambotautopayment.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.pdp.apptelegrambotautopayment.enums.Lang;
import uz.pdp.apptelegrambotautopayment.enums.State;
import uz.pdp.apptelegrambotautopayment.model.User;
import uz.pdp.apptelegrambotautopayment.repository.UserRepository;

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
        return users.getOrDefault(userId, userRepository.findById(userId).orElse(userRepository.save(new User(userId, State.START, Lang.RU))));
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
}
