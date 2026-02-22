package learn.lal.repository;

import learn.lal.model.HistoryRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface HistoryRepository extends MongoRepository<HistoryRecord, String> {
    List<HistoryRecord> findByUsernameOrderByTimestampDesc(String username);
}
