package uz.pdp.apptelegrambotautopayment.utils;

import org.springframework.stereotype.Component;
import uz.pdp.apptelegrambotautopayment.model.User;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class Temp {
    private final ConcurrentMap<Long, User> users = new ConcurrentHashMap<>();

    public User getUser(Long userId) {
        return users.get(userId);
    }

    public void setUser(User user) {
        users.put(user.getId(), user);
    }

    public void removeUser(Long userId) {
        users.remove(userId);
    }
}
