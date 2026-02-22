package learn.lal.service;

import learn.lal.model.User;
import learn.lal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.users.nirvaan.password}")
    private String nirvaanPassword;

    @Value("${app.users.devaansh.password}")
    private String devaanshPassword;

    @Value("${app.users.admin.password}")
    private String adminPassword;

    public CustomUserDetailsService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Try to load from MongoDB
        try {
            Optional<User> mongoUserOpt = userRepository.findByUsername(username);
            if (mongoUserOpt.isPresent()) {
                User mongoUser = mongoUserOpt.get();
                // Ensure mongo passwords have an encoder prefix if they don't already
                String pwd = mongoUser.getPassword();
                if (pwd != null && !pwd.startsWith("{")) {
                    pwd = "{noop}" + pwd;
                }
                return org.springframework.security.core.userdetails.User.withUsername(mongoUser.getUsername())
                        .password(pwd)
                        .roles(mongoUser.getRoles().toArray(new String[0]))
                        .build();
            }
        } catch (Exception e) {
            // Log warning if MongoDB is down, and proceed to fallback
            System.err.println("Database fetch failed for user " + username + ", falling back to in-memory config: " + e.getMessage());
        }

        // 2. Fallback to properties-based users
        if ("nirvaan".equals(username)) {
            return org.springframework.security.core.userdetails.User.withUsername("nirvaan")
                    .password("{noop}" + nirvaanPassword)
                    .roles("USER")
                    .build();
        } else if ("devaansh".equals(username)) {
            return org.springframework.security.core.userdetails.User.withUsername("devaansh")
                    .password("{noop}" + devaanshPassword)
                    .roles("USER")
                    .build();
        } else if ("admin".equals(username)) {
            return org.springframework.security.core.userdetails.User.withUsername("admin")
                    .password("{noop}" + adminPassword)
                    .roles("ADMIN")
                    .build();
        }

        throw new UsernameNotFoundException("User not found: " + username);
    }
}
