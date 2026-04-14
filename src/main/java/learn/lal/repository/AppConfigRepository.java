package learn.lal.repository;

import learn.lal.model.AppConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AppConfigRepository extends MongoRepository<AppConfig, String> {
}
