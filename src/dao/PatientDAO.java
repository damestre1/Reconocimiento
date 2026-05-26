package dao;

import model.Patient;

public class PatientDAO {

    public void save(Patient patient) {
        System.out.println("el paciente se ha fuardado correctamente en la  base de datos...");
        System.out.println("NOMBRE : " + patient.getName());
    }
}