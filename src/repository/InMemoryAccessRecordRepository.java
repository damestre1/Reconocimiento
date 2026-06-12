package repository;

import model.AccessRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class InMemoryAccessRecordRepository implements AccessRecordRepository {

    private final List<AccessRecord> records =
            Collections.synchronizedList(new ArrayList<>());

    @Override
    public void save(AccessRecord record) {
        records.add(record);
    }

    @Override
    public List<AccessRecord> findAll() {
        synchronized (records) {
            return new ArrayList<>(records);
        }
    }

    @Override
    public List<AccessRecord> findByNationalId(String nationalId) {
        synchronized (records) {
            return records.stream()
                    .filter(r -> nationalId != null
                            && nationalId.equals(r.getNationalId()))
                    .collect(Collectors.toList());
        }
    }
}
