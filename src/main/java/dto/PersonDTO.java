package dto;

import entities.Address;
import entities.Person;
import java.util.ArrayList;
import java.util.List;

public class PersonDTO {

    private Long id;
    private String fName;
    private String lName;
    private String phone;
    private String street;
    private String city;
    private int zip;

    public PersonDTO(Long id, String fName, String lName, String phone) {
        this.id = id;
        this.fName = fName;
        this.lName = lName;
        this.phone = phone;
    }

    public PersonDTO(Long id, String fName, String lName, String phone, String street, String city, int zip) {
        this.id = id;
        this.fName = fName;
        this.lName = lName;
        this.phone = phone;
        this.street = street;
        this.city = city;
        this.zip = zip;
    }

    public PersonDTO(Person person) {
        this.id = person.getId();
        this.fName = person.getfName();
        this.lName = person.getlName();
        this.phone = person.getPhone();
        if (person.getAddress() != null) {
            this.street = person.getAddress().getStreet();
            this.city = person.getAddress().getCity();
            this.zip = person.getAddress().getZip();
        }
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public int getZip() {
        return zip;
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
