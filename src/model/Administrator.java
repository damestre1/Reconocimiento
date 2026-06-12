package model;

public class Administrator extends Person {

    private final String username;
    private final String password;

    public Administrator(String firstName,
                         String lastName,
                         String email,
                         String phone,
                         String username,
                         String password) {
        super(firstName, lastName, email, phone);
        this.username = username;
        this.password = password;
    }

    public boolean checkPassword(String candidate) {
        return password != null && password.equals(candidate);
    }

    public String getUsername() {
        return username;
    }
}
