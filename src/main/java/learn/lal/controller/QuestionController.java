package learn.lal.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import learn.lal.model.Curriculum;
import learn.lal.repository.CurriculumRepository;
import learn.lal.model.CurriculumManifestEntry;
import learn.lal.service.CurriculumRegistryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final CurriculumRepository curriculumRepository;
    private final ObjectMapper objectMapper;
    private final CurriculumRegistryService curriculumRegistryService;

    public QuestionController(CurriculumRepository curriculumRepository, ObjectMapper objectMapper,
            CurriculumRegistryService curriculumRegistryService) {
        this.curriculumRepository = curriculumRepository;
        this.objectMapper = objectMapper;
        this.curriculumRegistryService = curriculumRegistryService;
    }

    /**
     * Active curricula for the picker (not soft-deleted).
     */
    @GetMapping("/curricula")
    public Map<String, Object> listActiveCurricula() throws IOException {
        List<Map<String, Object>> items = new ArrayList<>();
        for (CurriculumManifestEntry e : curriculumRegistryService.listActive()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("displayName", e.getDisplayName());
            m.put("level", e.getLevel() != null ? e.getLevel() : "");
            items.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        return out;
    }

    /**
     * Question map for one curriculum. Pass {@code curriculumId}; if omitted, uses the first active curriculum.
     */
    @GetMapping("/curriculum")
    public Map<String, Object> getCurriculum(@RequestParam(value = "curriculumId", required = false) String curriculumId)
            throws IOException {
        String id = (curriculumId == null || curriculumId.isBlank())
                ? curriculumRegistryService.resolveDefaultActiveId()
                : curriculumId;
        return curriculumRegistryService.readCurriculumData(id);
    }

    /**
     * POST /api/questions/upload
     * Handles JSON file upload, stores in DB, and returns the parsed content.
     */
    @PostMapping("/upload")
    public Curriculum uploadCurriculum(@RequestParam("file") MultipartFile file, @RequestParam(value = "name", defaultValue = "Custom Set") String name) throws IOException {
        Map<String, Object> data = objectMapper.readValue(file.getInputStream(), new TypeReference<Map<String, Object>>() {});
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "Guest";

        Curriculum curriculum = new Curriculum();
        curriculum.setName(name);
        curriculum.setUploadedBy(username);
        curriculum.setUploadedAt(LocalDateTime.now());
        curriculum.setData(data);

        return curriculumRepository.save(curriculum);
    }

    /**
     * GET /api/questions/list
     * Lists all uploaded curriculums (metadata only).
     */
    @GetMapping("/list")
    public Iterable<Curriculum> listCurriculums() {
        return curriculumRepository.findAll();
    }
}
