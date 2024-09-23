package uz.pdp.apptelegrambotautopayment.utils;

import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class Temp {
    private final ConcurrentMap<Long, LinkedList<Object>> objects = new ConcurrentHashMap<>();

    public void clear(Long userId) {
        objects.remove(userId);
    }

    public List<Object> getObjects(Long userId) {
        if (objects.containsKey(userId)) {
            return objects.get(userId);
        }
        return new LinkedList<>();
    }

    public void putObject(Long userId, Object object) {
        objects.computeIfAbsent(userId, k -> new LinkedList<>()).add(object);
    }
}
