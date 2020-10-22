package poppet.example.spring.model;

public class Context {
    private String authSecret;

    public Context(String authSecret) {
        this.authSecret = authSecret;
    }

    public String getAuthSecret() {
        return authSecret;
    }
}
