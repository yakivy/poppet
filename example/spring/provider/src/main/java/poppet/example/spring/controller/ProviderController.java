package poppet.example.spring.controller;

import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import poppet.example.spring.service.ProviderGenerator;
import poppet.example.spring.service.UserService;

@Controller
public class ProviderController {
    private UserService userService;

    public ProviderController(UserService userService) {
        this.userService = userService;
    }

    @RequestMapping("/api/service")
    public ResponseEntity<byte[]> apply(RequestEntity<byte[]> request) {
        return ProviderGenerator.apply(userService).apply(request);
    }
}
