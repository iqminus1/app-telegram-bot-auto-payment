//package uz.pdp.apptelegrambotautopayment.security;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//
//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity
//public class SecurityConfig {
//    @Value("${app.security.username}")
//    private String username;
//    @Value("${app.security.password}")
//    private String password;
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .authorizeHttpRequests(conf ->
//                        conf.requestMatchers(
//                                        "/api/**").permitAll()
//                                .anyRequest().authenticated())
//                .httpBasic(Customizer.withDefaults());
//
//        return http.build();
//    }
//
//    @Bean
//    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
//        AuthenticationManagerBuilder authenticationManagerBuilder =
//                http.getSharedObject(AuthenticationManagerBuilder.class);
//        authenticationManagerBuilder
//                .inMemoryAuthentication()
//                .withUser(username)
//                .password(passwordEncoder().encode(password))
//                .roles("ADMIN");
//        return authenticationManagerBuilder.build();
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//}