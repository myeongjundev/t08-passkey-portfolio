package dev.myeongjun.passkey;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class T08PasskeyPortfolioApplication {

	public static void main(String[] args) {
		SpringApplication.run(T08PasskeyPortfolioApplication.class, args);
	}

}
