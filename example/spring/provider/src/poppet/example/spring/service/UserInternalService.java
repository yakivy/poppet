package poppet.example.spring.service;

import org.springframework.stereotype.Service;
import poppet.example.spring.model.User;

@Service
public class UserInternalService implements UserService {
    @Override
    public User findById(String id) {
        //emulation of business logic
        if ("1".equals(id)) return new User(id, "Antony");
        else throw new IllegalArgumentException("User is not found");
    }
}
