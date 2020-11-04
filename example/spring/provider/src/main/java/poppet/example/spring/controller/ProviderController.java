package poppet.example.spring.controller;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
    private final String authSecret;
    private final Function1<JsonNode, JsonNode> provider;

    public ProviderController(
        @Value("${auth.secret}") String authSecret,
        UserService userService
    ) {
        this.authSecret = authSecret;
        this.provider = ProviderGenerator.apply(userService, authSecret);
    }

    private RequestEntity<JsonNode> checkAuth(RequestEntity<JsonNode> request) {
        List<String> headerValues = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (headerValues != null && headerValues.contains(authSecret)) return request;
        else throw new IllegalArgumentException("Wrong secret!");
    }

    @RequestMapping("/api/service")
    public ResponseEntity<JsonNode> apply(RequestEntity<JsonNode> request) {
        return new ResponseEntity<>(provider.apply(checkAuth(request).getBody()), HttpStatus.OK);
    }
}
