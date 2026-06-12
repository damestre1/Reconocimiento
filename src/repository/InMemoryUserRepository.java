package repository;

import model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> users = new ConcurrentHashMap<>();

    @Override
    public void save(User user) {
        users.put(user.getNationalId(), user);
    }

    @Override
    public Optional<User> findByNationalId(String nationalId) {
        return Optional.ofNullable(users.get(nationalId));
    }

    @Override
    public boolean existsByNationalId(String nationalId) {
        return users.containsKey(nationalId);
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public void deleteByNationalId(String nationalId) {
        users.remove(nationalId);
    }
}
