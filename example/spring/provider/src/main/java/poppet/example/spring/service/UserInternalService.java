package poppet.example.spring.service;

import org.springframework.stereotype.Service;
import poppet.example.spring.model.User;

@Service
public class UserInternalService implements UserService {
    @Override
    public User findById(String id) {
        return new User(id, "Antony");
    }
}
