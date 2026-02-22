package learn.lal.controller;

import learn.lal.model.HistoryRecord;
import learn.lal.repository.HistoryRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryRepository historyRepository;

    public HistoryController(HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @PostMapping
    public HistoryRecord saveHistory(@RequestBody HistoryRecord record) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            record.setUsername(auth.getName());
        } else {
            record.setUsername("Guest");
        }
        record.setTimestamp(LocalDateTime.now());
        return historyRepository.save(record);
    }

    @GetMapping
    public List<HistoryRecord> getHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return historyRepository.findByUsernameOrderByTimestampDesc(auth.getName());
        }
        return List.of();
    }

    @DeleteMapping("/{id}")
    public void deleteHistory(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            historyRepository.deleteByIdAndUsername(id, auth.getName());
        }
    }
}
