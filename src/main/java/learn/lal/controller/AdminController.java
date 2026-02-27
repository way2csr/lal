package learn.lal.controller;

import learn.lal.repository.UserRepository;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;

    public AdminController(UserRepository userRepository, PasswordEncoder passwordEncoder, MongoTemplate mongoTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mongoTemplate = mongoTemplate;
    }

    // GET /api/admin/users - list all users (password omitted)
    // Uses username as the unique "id" key to avoid BSON ObjectId serialization issues
    @GetMapping("/users")
    public List<Map<String, Object>> listUsers() {
        List<Document> rawDocs = mongoTemplate.getCollection("users").find().into(new ArrayList<>());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Document doc : rawDocs) {
            String username = doc.getString("username");
            if (username == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", username); // use username as the stable identifier
            m.put("username", username);
            m.put("roles", doc.getList("roles", String.class));
            m.put("email", doc.getString("email"));
            m.put("phoneNumber", doc.getString("phoneNumber"));
            m.put("firstName", doc.getString("firstName"));
            m.put("lastName", doc.getString("lastName"));
            m.put("age", doc.getInteger("age"));
            m.put("fatherName", doc.getString("fatherName"));
            m.put("motherName", doc.getString("motherName"));
            result.add(m);
        }
        return result;
    }

    // PUT /api/admin/users/{username} - update user info / optionally reset password
    @PutMapping("/users/{username}")
    public Map<String, String> updateUser(@PathVariable String username, @RequestBody Map<String, Object> body) {
        Query query = Query.query(Criteria.where("username").is(username));
        Update update = new Update();
        if (body.containsKey("firstName"))   update.set("firstName",   body.get("firstName"));
        if (body.containsKey("lastName"))    update.set("lastName",    body.get("lastName"));
        if (body.containsKey("email"))       update.set("email",       body.get("email"));
        if (body.containsKey("phoneNumber")) update.set("phoneNumber", body.get("phoneNumber"));
        if (body.containsKey("fatherName"))  update.set("fatherName",  body.get("fatherName"));
        if (body.containsKey("motherName"))  update.set("motherName",  body.get("motherName"));
        if (body.containsKey("age") && body.get("age") != null) {
            update.set("age", ((Number) body.get("age")).intValue());
        }
        String newPassword = (String) body.get("newPassword");
        if (newPassword != null && !newPassword.isBlank()) {
            update.set("password", passwordEncoder.encode(newPassword));
        }
        mongoTemplate.updateFirst(query, update, "users");

        Map<String, String> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("message", "User updated successfully");
        return resp;
    }

    // DELETE /api/admin/users/{username} - delete a user
    @DeleteMapping("/users/{username}")
    public Map<String, String> deleteUser(@PathVariable String username) {
        Query query = Query.query(Criteria.where("username").is(username));
        mongoTemplate.remove(query, "users");
        Map<String, String> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("message", "User deleted successfully");
        return resp;
    }
}
