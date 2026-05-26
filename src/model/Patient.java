package model;

public class Patient {

    private String name;
    private int age;
    private String diseases;
    private String medications;
    private String imagePath;

    public Patient(String name, int age, String diseases, String medications, String imagePath) {
        this.name = name;
        this.age = age;
        this.diseases = diseases;
        this.medications = medications;
        this.imagePath = imagePath;
    }

    public String getName() { return name; }
    public int getAge() { return age; }
    public String getDiseases() { return diseases; }
    public String getMedications() { return medications; }
    public String getImagePath() { return imagePath; }
}