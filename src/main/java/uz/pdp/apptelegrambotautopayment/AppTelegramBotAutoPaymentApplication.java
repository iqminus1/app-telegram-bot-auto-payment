package uz.pdp.apptelegrambotautopayment;

import org.apache.commons.codec.CharEncoding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.ResourceBundle;

@SpringBootApplication
public class AppTelegramBotAutoPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppTelegramBotAutoPaymentApplication.class, args);
    }

    @Bean
    public ResourceBundleMessageSource messageSourceResourceBundle() {
        ResourceBundleMessageSource resourceBundleMessageSource = new ResourceBundleMessageSource();
        resourceBundleMessageSource.setBasename("classpath:messages");
        resourceBundleMessageSource.setFallbackToSystemLocale(false);
        resourceBundleMessageSource.setDefaultEncoding(CharEncoding.UTF_8);
        return resourceBundleMessageSource;
    }
}
