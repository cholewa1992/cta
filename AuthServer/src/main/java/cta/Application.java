package cta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.Security;

/**
 * Created by wismann on 18/04/2017.
 */
@SpringBootApplication
public class Application {

    static {
        Security.insertProviderAt(new org.bouncycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    public static void main(String[] args) {

        SpringApplication.run(Application.class, args);
    }
}
