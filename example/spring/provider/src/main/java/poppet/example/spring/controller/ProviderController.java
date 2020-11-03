package poppet.example.spring.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import poppet.example.spring.service.ProviderGenerator;
import poppet.example.spring.service.UserService;
import scala.Function1;

@Controller
public class ProviderController {
    private final Function1<RequestEntity<JsonNode>, ResponseEntity<JsonNode>> provider;

    public ProviderController(
        UserService userService,
        @Value("${auth.secret}") String authSecret
    ) {
        provider = ProviderGenerator.apply(userService, authSecret);
    }

    @RequestMapping("/api/service")
    public ResponseEntity<JsonNode> apply(RequestEntity<JsonNode> request) {
        return provider.apply(request);
    }
}
