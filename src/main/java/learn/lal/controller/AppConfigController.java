package learn.lal.controller;

import learn.lal.model.AppConfig;
import learn.lal.repository.AppConfigRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/config")
public class AppConfigController {

    private final AppConfigRepository repo;

    public AppConfigController(AppConfigRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/{key}")
    public Map<String, Object> getConfig(@PathVariable String key) {
        Optional<AppConfig> opt = repo.findById(key);
        if (opt.isPresent()) {
            AppConfig cfg = opt.get();
            Map<String, Object> result = new HashMap<>();
            result.put("key", cfg.getKey());
            result.put("value", cfg.getValue());
            result.put("updatedBy", cfg.getUpdatedBy());
            result.put("updatedAt", cfg.getUpdatedAt() != null ? cfg.getUpdatedAt().toString() : null);
            return result;
        }
        return Map.of("key", key, "value", Map.of());
    }

    @GetMapping
    public List<AppConfig> getAllConfigs() {
        return repo.findAll();
    }

    @PutMapping("/{key}")
    public Map<String, Object> saveConfig(@PathVariable String key, @RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            return Map.of("error", "Only admins can update configuration");
        }

        AppConfig cfg = repo.findById(key).orElse(new AppConfig());
        cfg.setKey(key);
        cfg.setValue(body);
        cfg.setUpdatedBy(auth.getName());
        cfg.setUpdatedAt(Instant.now());
        repo.save(cfg);

        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("key", key);
        result.put("value", cfg.getValue());
        result.put("updatedBy", cfg.getUpdatedBy());
        result.put("updatedAt", cfg.getUpdatedAt().toString());
        return result;
    }
}
