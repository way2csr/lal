package learn.lal.controller;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;

@Controller
public class LoginController {

    @PostMapping("/api/guest/login")
    @ResponseBody
    public String guestLogin() {
        // Create an anonymous/guest user seamlessly
        User guestUser = new User(
            "Guest",
            "",
            Collections.singleton(() -> "ROLE_USER")
        );

        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(guestUser, null, guestUser.getAuthorities());
        
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return "SUCCESS";
    }
}
