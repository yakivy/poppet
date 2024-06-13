package poppet.example.spring.provider.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import poppet.core.Request;
import poppet.core.Response;
import poppet.example.spring.provider.service.ProviderGenerator;
import scala.Function1;

@Controller
public class ProviderController {
    private final Function1<Request<JsonNode>, Response<JsonNode>> provider;

    public ProviderController(ProviderGenerator providerGenerator) {
        provider = providerGenerator.apply();
    }

    @RequestMapping("/api/service")
    public ResponseEntity<Response<JsonNode>> apply(RequestEntity<Request<JsonNode>> request) {
        return new ResponseEntity<>(provider.apply(request.getBody()), HttpStatus.OK);
    }
}
