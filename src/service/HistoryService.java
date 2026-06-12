package service;

import model.AccessMethod;
import model.AccessRecord;
import model.AccessResult;
import repository.AccessRecordRepository;
import util.CodeGenerator;

import java.util.List;

public class HistoryService {

    private final AccessRecordRepository accessRecordRepository;

    public HistoryService(AccessRecordRepository accessRecordRepository) {
        this.accessRecordRepository = accessRecordRepository;
    }

    public AccessRecord record(String nationalId,
                               AccessMethod method,
                               AccessResult result,
                               String detail) {

        AccessRecord record = new AccessRecord(
                CodeGenerator.accessRecordId(),
                nationalId,
                method,
                result,
                detail
        );

        accessRecordRepository.save(record);
        return record;
    }

    public List<AccessRecord> getAll() {
        return accessRecordRepository.findAll();
    }

    public List<AccessRecord> getByUser(String nationalId) {
        return accessRecordRepository.findByNationalId(nationalId);
    }
}
