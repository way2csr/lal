package learn.lal.repository;

import learn.lal.model.Curriculum;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurriculumRepository extends MongoRepository<Curriculum, String> {
    Optional<Curriculum> findByName(String name);
}
