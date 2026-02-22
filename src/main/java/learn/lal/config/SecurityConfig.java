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

    @Value("${app.users.nirvana.password}")
    private String nirvanaPassword;

    @Value("${app.users.devansh.password}")
    private String devanshPassword;

    @Value("${app.users.admin.password}")
    private String adminPassword;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/login.html", "/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/learn.html", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login.html?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf.disable()); // Disabled so frontend JS shutdown button works without needing CSRF tokens

        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

        UserDetails nirvana = User.withUsername("nirvana")
            .password(encoder.encode(nirvanaPassword))
            .roles("USER")
            .build();

        UserDetails devansh = User.withUsername("devansh")
            .password(encoder.encode(devanshPassword))
            .roles("USER")
            .build();

        UserDetails admin = User.withUsername("admin")
            .password(encoder.encode(adminPassword))
            .roles("ADMIN")
            .build();

        return new InMemoryUserDetailsManager(nirvana, devansh, admin);
    }
}
