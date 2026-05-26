package service;

import dao.PatientDAO;
import model.Patient;

public class PatientService {

    private PatientDAO dao = new PatientDAO();

    public void register(Patient patient) {
        dao.save(patient);
    }
}