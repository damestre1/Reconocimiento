package repository;

import model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    void save(User user);

    Optional<User> findByNationalId(String nationalId);

    boolean existsByNationalId(String nationalId);

    List<User> findAll();

    void deleteByNationalId(String nationalId);
}
