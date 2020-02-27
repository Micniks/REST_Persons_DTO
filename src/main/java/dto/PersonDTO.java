package dto;

import entities.Person;
import java.util.ArrayList;
import java.util.List;

public class PersonDTO {

    private Long id;
    private String fName;
    private String lName;
    private String phone;

    public PersonDTO(Long id, String fName, String lName, String phone) {
        this.id = id;
        this.fName = fName;
        this.lName = lName;
        this.phone = phone;
    }

    public PersonDTO(Person person) {
        this.id = person.getId();
        this.fName = person.getfName();
        this.lName = person.getlName();
        this.phone = person.getPhone();
    }

    public Long getId() {
        return id;
    }

    public String getfName() {
        return fName;
    }

    public String getlName() {
        return lName;
    }

    public String getPhone() {
        return phone;
    }

}
