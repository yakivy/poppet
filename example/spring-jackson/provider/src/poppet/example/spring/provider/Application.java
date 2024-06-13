package poppet.example.spring.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.scala.ClassTagExtensions;
import com.fasterxml.jackson.module.scala.DefaultScalaModule$;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    private static class MyObjectMapper extends ObjectMapper implements ClassTagExtensions {}

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new MyObjectMapper();
        objectMapper.registerModule(DefaultScalaModule$.MODULE$);
        return objectMapper;
    }
}
