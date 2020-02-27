package dto;

import entities.Person;
import java.util.ArrayList;
import java.util.List;

public class PersonsDTO {

    private List<PersonDTO> all;

    public PersonsDTO(List<Person> personList) {
        this.all = new ArrayList();
        for (Person person : personList) {
            this.all.add(new PersonDTO(person));
        }
    }

    public PersonsDTO() {
        this.all = new ArrayList();
    }

    public List<PersonDTO> getAll() {
        return all;
    }

    public void setAll(List<PersonDTO> personList) {
        this.all = personList;
    }

}
