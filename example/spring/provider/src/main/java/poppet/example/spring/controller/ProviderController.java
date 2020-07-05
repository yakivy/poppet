package poppet.example.spring.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import poppet.example.spring.service.ProviderGenerator;
import poppet.example.spring.service.UserService;

@Controller
public class ProviderController {
    private UserService userService;
    private String authHeader;
    private String authSecret;

    public ProviderController(
        UserService userService,
        @Value("${auth.header}") String authHeader,
        @Value("${auth.secret}") String authSecret
    ) {
        this.userService = userService;
        this.authHeader = authHeader;
        this.authSecret = authSecret;
    }

    @RequestMapping("/api/service")
    public ResponseEntity<byte[]> apply(RequestEntity<byte[]> request) {
        return ProviderGenerator.apply(userService, authHeader, authSecret).apply(request);
    }
}
