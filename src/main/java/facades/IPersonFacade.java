package facades;

import dto.PersonDTO;
import dto.PersonsDTO;
import exceptions.MissingInputException;
import exceptions.PersonNotFoundException;

public interface IPersonFacade {

    public PersonDTO addPerson(String fName, String lName, String phone) throws MissingInputException;

    public PersonDTO deletePerson(Long id) throws PersonNotFoundException;

    public PersonDTO getPerson(Long id) throws PersonNotFoundException;

    public PersonsDTO getAllPersons();

    public PersonDTO editPerson(PersonDTO p) throws PersonNotFoundException, MissingInputException;

}
