package poppet.example.spring.service;

import poppet.example.spring.model.User;

public interface UserService {
    public User findById(String id);
}
