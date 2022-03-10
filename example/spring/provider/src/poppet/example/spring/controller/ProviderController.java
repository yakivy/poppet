package poppet.example.spring.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import poppet.example.spring.service.ProviderGenerator;
import poppet.example.spring.service.UserService;
import scala.Function1;

@Controller
public class ProviderController {
    private final Function1<JsonNode, JsonNode> provider;

    public ProviderController(UserService userService) {
        provider = ProviderGenerator.apply(userService);
    }

    @RequestMapping("/api/service")
    public ResponseEntity<JsonNode> apply(RequestEntity<JsonNode> request) {
        return new ResponseEntity<>(provider.apply(request.getBody()), HttpStatus.OK);
    }
}
