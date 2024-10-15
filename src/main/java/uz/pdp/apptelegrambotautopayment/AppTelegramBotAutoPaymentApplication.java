package uz.pdp.apptelegrambotautopayment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.codec.CharEncoding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.text.DecimalFormat;

@EnableScheduling
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

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }


    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public DecimalFormat decimalFormat() {
        return new DecimalFormat("###,###,###");
    }
}
