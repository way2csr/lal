package learn.lal.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import learn.lal.model.Curriculum;
import learn.lal.repository.CurriculumRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final CurriculumRepository curriculumRepository;
    private final ObjectMapper objectMapper;

    public QuestionController(CurriculumRepository curriculumRepository, ObjectMapper objectMapper) {
        this.curriculumRepository = curriculumRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * GET /api/questions/curriculum
     * Returns the pre-defined Level 2 curriculum from resources.
     */
    @GetMapping("/curriculum")
    public Map<String, Object> getCurriculum() throws IOException {
        ClassPathResource resource = new ClassPathResource("questions/level2.json");
        if (!resource.exists()) {
            // Fallback or empty if not found
            return new HashMap<>();
        }
        return objectMapper.readValue(resource.getInputStream(), new TypeReference<Map<String, Object>>() {});
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
