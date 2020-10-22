package poppet.example.spring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import poppet.example.spring.service.ConsumerGenerator;
import poppet.example.spring.service.UserService;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public UserService userService(
        RestTemplate restTemplate,
        @Value("${consumer.url}") String url,
        @Value("${auth.secret}") String authSecret
    ) {
        return ConsumerGenerator.userService(restTemplate, url, authSecret);
    }
}
