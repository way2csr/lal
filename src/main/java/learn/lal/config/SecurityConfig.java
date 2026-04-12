package learn.lal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.users.nirvaan.password:123}")
    private String nirvaanPassword;

    @Value("${app.users.devaansh.password:123}")
    private String devaanshPassword;

    @Value("${app.users.admin.password:admin}")
    private String adminPassword;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // Public: login + registration
                .requestMatchers(
                    "/login.html",
                    "/api/guest/login",
                    "/api/user/register"
                ).permitAll()

                // Public: auth-status endpoint (every page calls this to check session)
                .requestMatchers("/api/auth/status").permitAll()

                // Public: static assets
                .requestMatchers("/css/**", "/js/**", "/images/**", "/error").permitAll()

                // Public: navigation hub + academy landing + all static course pages
                // (practice tools are pure HTML/JS, no sensitive data)
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/home.html",
                    "/abacus.html",
                    "/vedic-math.html",
                    "/phonetics.html",
                    "/calligraphy.html",
                    "/rubiks-cube.html",
                    "/smart-write.html",
                    "/reading-practice.html"
                ).permitAll()

                // Admin APIs
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Everything else (learn.html, AI APIs, history, etc.) needs auth
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/index.html", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login.html?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
