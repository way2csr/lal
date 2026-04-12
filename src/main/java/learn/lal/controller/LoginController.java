package learn.lal.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Controller
public class LoginController {

    /**
     * Guest login: creates a real server-side session so subsequent page
     * requests are recognised as authenticated by Spring Security.
     */
    @PostMapping("/api/guest/login")
    @ResponseBody
    public Map<String, String> guestLogin(HttpServletRequest request,
                                          HttpServletResponse response) {

        User guestUser = new User(
                "Guest",
                "",
                Collections.singleton(() -> "ROLE_USER")
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        guestUser, null, guestUser.getAuthorities());

        // Build a new SecurityContext and store it in the HTTP session
        // so every subsequent request in this browser tab stays authenticated.
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = request.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );

        Map<String, String> result = new HashMap<>();
        result.put("status", "SUCCESS");
        result.put("username", "Guest");
        return result;
    }

    /**
     * Returns the currently authenticated user's info as JSON.
     * Called by every page on load to decide whether to show Login or a
     * username / logout button.
     */
    @GetMapping("/api/auth/status")
    @ResponseBody
    public Map<String, Object> authStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());

        Map<String, Object> result = new HashMap<>();
        result.put("loggedIn", loggedIn);
        result.put("username", loggedIn ? auth.getName() : null);
        result.put("isAdmin", loggedIn && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        return result;
    }
}
