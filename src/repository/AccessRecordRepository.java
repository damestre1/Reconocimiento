package repository;

import model.AccessRecord;

import java.util.List;

public interface AccessRecordRepository {

    void save(AccessRecord record);

    List<AccessRecord> findAll();

    List<AccessRecord> findByNationalId(String nationalId);
}
