package poppet.example.spring.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.scala.ClassTagExtensions;
import com.fasterxml.jackson.module.scala.DefaultScalaModule$;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
import poppet.example.spring.service.UserService;
import poppet.example.spring.consumer.service.UserServiceProvider;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    private static class MyObjectMapper extends ObjectMapper implements ClassTagExtensions {}

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new MyObjectMapper();
        objectMapper.registerModule(DefaultScalaModule$.MODULE$);
        return objectMapper;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public UserService userService(
        RestTemplate restTemplate,
        @Value("${consumer.url}") String url,
        ObjectMapper objectMapper
    ) {
        return new UserServiceProvider(restTemplate, url, objectMapper).get();
    }
}
