package app;

import vision.CameraService;
import service.PatientService;
import model.Patient;

import java.util.Scanner;

public class App {

    private CameraService camera = new CameraService();
    private PatientService service = new PatientService();

    public void start() {

        Scanner sc = new Scanner(System.in);

        System.out.print("NOMBRE COMPLETO: ");
        String name = sc.nextLine();

        System.out.print("EDAD : ");
        int age = sc.nextInt();
        sc.nextLine();

        System.out.print("TIENES ALGUNA ENFERMEDAD: ");
        String diseases = sc.nextLine();

        System.out.print("TOMAS ALGUNOS MEDICAMENTOS ?: ");
        String medications = sc.nextLine();

        String imagePath = camera.captureFace(name);

        if (imagePath == null) {
            System.out.println("Falló la captura de rostro");
            return;
        }

        Patient patient = new Patient(name, age, diseases, medications, imagePath);

        service.register(patient);

        System.out.println("Paciente registrado exitosamente");

        camera.close();
    }
}