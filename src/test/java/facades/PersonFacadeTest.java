/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facades;

import dto.PersonDTO;
import dto.PersonsDTO;
import entities.Address;
import entities.Person;
import exceptions.MissingInputException;
import exceptions.PersonNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.EMF_Creator;

/**
 *
 * @author Michael N. Korsgaard
 */
//@Disabled
public class PersonFacadeTest {

    private static EntityManagerFactory EMF;
    private static IPersonFacade PF;
    private static Person p1, p2, p3;
    private static List<Person> personList = new ArrayList();
    private static Address a1, a2;
    private static List<Address> addressList = new ArrayList();
    private static int personAmount, addressAmount;
    private static Long highestPersonId, highestAddressId;
    private static EntityManager em;
    private static Date today, weekAgo;

    @BeforeAll
    public static void setUpClass() {
        //This method must be called before you request the EntityManagerFactory
        EMF_Creator.startREST_TestWithDB();
        EMF = EMF_Creator.createEntityManagerFactory(EMF_Creator.DbSelector.TEST, EMF_Creator.Strategy.CREATE);
        PF = PersonFacade.getPersonFacade(EMF);
    }

    @BeforeEach
    public void setup() {
        cleanUp();
        a1 = new Address("Drunk Street", "Copenhagen", 2800);
        a2 = new Address("Hidden Street", "Odensee", 9050);
        p1 = new Person("Jack", "Daniels", "78451293", a1);
        p2 = new Person("Captain", "Morgan", "97643185", a1);
        p3 = new Person("Michael", "Jackson", "19374628", a2);
        personList = new ArrayList(Arrays.asList(new Person[]{p1, p2, p3}));
        addressList = new ArrayList(Arrays.asList(new Address[]{a1, a2}));
        personAmount = personList.size();
        addressAmount = addressList.size();

        today = Date.from(Instant.now());
        weekAgo = Date.from(Instant.now().minus(7, ChronoUnit.DAYS));
        for (Person person : personList) {
            person.setCreated(weekAgo);
            person.setLastEdited(weekAgo);
        }

        em = EMF.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(p1);
            em.persist(p2);
            em.persist(p3);
            em.getTransaction().commit();
        } catch (Exception e) {
            fail("Could not complete setup: " + e.getMessage());
        } finally {
            em.close();
        }

        highestPersonId = 0L;
        for (Person person : personList) {
            if (person.getId() > highestPersonId) {
                highestPersonId = person.getId();
            }
        }

        highestAddressId = 0L;
        for (Address address : addressList) {
            if (address.getId() > highestAddressId) {
                highestAddressId = address.getId();
            }
        }
    }

    @AfterAll
    public static void tearDown() {
        cleanUp();
    }

    public static void cleanUp() {
        for (Person person : personList) {
            person = null;
        }
        personList.clear();
        personAmount = 0;
        em = EMF.createEntityManager();
        try {
            em.getTransaction().begin();
            Query q1 = em.createNamedQuery("Person.deleteAllRows");
            Query q2 = em.createNamedQuery("Address.deleteAllRows");
            q1.executeUpdate();
            q2.executeUpdate();
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    private List<Person> pullAllPersonsFromDB() {
        //Getting the new list from the database
        em = EMF.createEntityManager();
        List<Person> dbList = null;
        try {
            TypedQuery<Person> tq = em.createQuery("SELECT p FROM Person p", Person.class);
            dbList = tq.getResultList();
        } catch (Exception e) {
            fail("Could not retrive list of Persons from DB");
        } finally {
            em.close();
        }
        return dbList;
    }
    
    private List<Address> pullAllAddressesFromDB() {
        //Getting the new list from the database
        em = EMF.createEntityManager();
        List<Address> dbList = null;
        try {
            TypedQuery<Address> tq = em.createQuery("SELECT a FROM Address a", Address.class);
            dbList = tq.getResultList();
        } catch (Exception e) {
            fail("Could not retrive list of Addresses from DB");
        } finally {
            em.close();
        }
        return dbList;
    }

    @Test
    public void testAddPerson() throws MissingInputException {
        String fName = "Jesus";
        String lName = "Christ";
        String phone = "13974268";
        String street = "Hidden Street";
        String city = "Bornholm";
        int zip = 3000;

        PersonDTO result = PF.addPerson(fName, lName, phone, street, city, zip);

        //Changing expectations
        highestPersonId++;
        highestAddressId++;
        personAmount++;

        //Simple Asserts
        assertEquals(highestPersonId, result.getId());
        assertTrue(fName.equals(result.getfName()));
        assertTrue(lName.equals(result.getlName()));
        assertTrue(phone.equals(result.getPhone()));
        assertTrue(street.equals(result.getStreet()));
        assertTrue(city.equals(result.getCity()));
        assertEquals(zip, result.getZip());

        //Asserts on the DB
        List<Person> dbList = pullAllPersonsFromDB();
        assertEquals(personAmount, dbList.size());
        boolean matchingIdFound = false;
        for (Person person : dbList) {
            if (Objects.equals(person.getId(), result.getId())) {
                assertTrue(result.getfName().equals(person.getfName()));
                assertTrue(result.getlName().equals(person.getlName()));
                assertTrue(result.getPhone().equals(person.getPhone()));
                assertTrue(result.getStreet().equals(person.getAddress().getStreet()));
                assertTrue(result.getCity().equals(person.getAddress().getCity()));
                assertEquals(result.getZip(), person.getAddress().getZip());
                assertEquals(highestAddressId, person.getAddress().getId());
                matchingIdFound = true;
                break;
            }
        }
        assertTrue(matchingIdFound);
        
        //Assume 1 new address has been added to the db
        addressAmount++;
        List<Address> dblistAddress = pullAllAddressesFromDB();
        assertEquals(addressAmount, dblistAddress.size());
    }
    
    @Test
    public void testAddPerson_ExsistingAddress() throws MissingInputException {
        String fName = "Jesus";
        String lName = "Christ";
        String phone = "13974268";
        Address expectedAddress = a1;
        String street = expectedAddress.getStreet();
        String city = expectedAddress.getCity();
        int zip = expectedAddress.getZip();

        PersonDTO result = PF.addPerson(fName, lName, phone, street, city, zip);

        //Changing expectations
        highestPersonId++;
        highestAddressId++;
        personAmount++;

        //Simple Asserts
        assertEquals(highestPersonId, result.getId());
        assertTrue(fName.equals(result.getfName()));
        assertTrue(lName.equals(result.getlName()));
        assertTrue(phone.equals(result.getPhone()));
        assertTrue(street.equals(result.getStreet()));
        assertTrue(city.equals(result.getCity()));
        assertEquals(zip, result.getZip());

        //Asserts on the DB
        List<Person> dbList = pullAllPersonsFromDB();
        assertEquals(personAmount, dbList.size());
        boolean matchingIdFound = false;
        for (Person person : dbList) {
            if (Objects.equals(person.getId(), result.getId())) {
                assertTrue(result.getfName().equals(person.getfName()));
                assertTrue(result.getlName().equals(person.getlName()));
                assertTrue(result.getPhone().equals(person.getPhone()));
                assertTrue(result.getStreet().equals(person.getAddress().getStreet()));
                assertTrue(result.getCity().equals(person.getAddress().getCity()));
                assertEquals(result.getZip(), person.getAddress().getZip());
                assertEquals(expectedAddress.getId(), person.getAddress().getId());
                matchingIdFound = true;
                break;
            }
        }
        assertTrue(matchingIdFound);
        
        //Assume no new address has been added to the db
        List<Address> dblistAddress = pullAllAddressesFromDB();
        assertEquals(addressAmount, dblistAddress.size());
    }

    @Test
    public void testNegative_AddPerson_MissingInput_FirstName_Null() {
        String fName = null;
        String lName = "Christ";
        String phone = "13974268";
        String street = "Hidden Street";
        String city = "Bornholm";
        int zip = 3000;

        assertThrows(MissingInputException.class, () -> {
            PersonDTO result = PF.addPerson(fName, lName, phone, street, city, zip);
        });
    }

    @Test
    public void testNegative_AddPerson_MissingInput_FirstName_Empty() {
        String fName = "";
        String lName = "Christ";
        String phone = "13974268";
        String street = "Hidden Street";
        String city = "Bornholm";
        int zip = 3000;

        assertThrows(MissingInputException.class, () -> {
            PersonDTO result = PF.addPerson(fName, lName, phone, street, city, zip);
        });
    }

    @Test
    public void testNegative_AddPerson_MissingInput_LastName_Null() {
        String fName = "Jesus";
        String lName = null;
        String phone = "13974268";
        String street = "Hidden Street";
        String city = "Bornholm";
        int zip = 3000;

        assertThrows(MissingInputException.class, () -> {
            PersonDTO result = PF.addPerson(fName, lName, phone, street, city, zip);
        });
    }

    @Test
    public void testNegative_AddPerson_MissingInput_LastName_Empty() {
        String fName = "Jesus";
        String lName = "";
        String phone = "13974268";
        String street = "Hidden Street";
        String city = "Bornholm";
        int zip = 3000;

        assertThrows(MissingInputException.class, () -> {
            PersonDTO result = PF.addPerson(fName, lName, phone, street, city, zip);
        });
    }

    @Test
    public void testDeletePerson() throws PersonNotFoundException {
        Person expectedPerson = p1;
        Long id = expectedPerson.getId();
        PersonDTO result = PF.deletePerson(id);

        //Simple Asserts
        assertEquals(id, result.getId());
        assertTrue(expectedPerson.getfName().equals(result.getfName()));
        assertTrue(expectedPerson.getlName().equals(result.getlName()));
        assertTrue(expectedPerson.getPhone().equals(result.getPhone()));

        //Changing expectations
        personAmount--;

        //Asserts on the DB
        List<Person> dbList = pullAllPersonsFromDB();
        assertEquals(personAmount, dbList.size());
        for (Person person : dbList) {
            if (Objects.equals(person.getId(), result.getId())) {
                fail("Deleted Person was still in DB");
            }
        }
    }

    @Test
    public void testNegative_DeletePerson_NotFound() {
        Long searchID = highestPersonId + 1;

        assertThrows(PersonNotFoundException.class, () -> {
            PersonDTO result = PF.deletePerson(searchID);
        });
    }

    @Test
    public void testEditPerson() throws PersonNotFoundException, MissingInputException {
        Person expectedPerson = p1;

        expectedPerson.setPhone("85858585");
        expectedPerson.setfName("Mr.");
        expectedPerson.setlName("T");

        PersonDTO editPerson = new PersonDTO(expectedPerson);
        PersonDTO result = PF.editPerson(editPerson);

        //Simple Asserts
        assertEquals(expectedPerson.getId(), result.getId());
        assertTrue(expectedPerson.getfName().equals(result.getfName()));
        assertTrue(expectedPerson.getlName().equals(result.getlName()));
        assertTrue(expectedPerson.getPhone().equals(result.getPhone()));
        assertTrue(expectedPerson.getAddress().getStreet().equals(result.getStreet()));
        assertTrue(expectedPerson.getAddress().getCity().equals(result.getCity()));
        assertEquals(expectedPerson.getAddress().getZip(), result.getZip());

        //Asserts on the DB
        List<Person> dbList = pullAllPersonsFromDB();
        assertEquals(personAmount, dbList.size());
        boolean matchingIdFound = false;
        for (Person person : dbList) {
            if (Objects.equals(result.getId(), person.getId())) {
                assertTrue(result.getfName().equals(person.getfName()));
                assertTrue(result.getlName().equals(person.getlName()));
                assertTrue(result.getPhone().equals(person.getPhone()));
                assertTrue(result.getStreet().equals(person.getAddress().getStreet()));
                assertTrue(result.getCity().equals(person.getAddress().getCity()));
                assertEquals(result.getZip(), person.getAddress().getZip());
                assertEquals(weekAgo.getDate(), person.getCreated().getDate());
                assertEquals(today.getDate(), person.getLastEdited().getDate());
                matchingIdFound = true;
                break;
            }
        }
        assertTrue(matchingIdFound);
        
        //Assume no new address has been added to the db
        List<Address> dblistAddress = pullAllAddressesFromDB();
        assertEquals(addressAmount, dblistAddress.size());
        
    }

    @Test
    public void testEditPerson_NewAddess() throws PersonNotFoundException, MissingInputException {
        Person expectedPerson = p1;

        expectedPerson.setPhone("85858585");
        expectedPerson.setfName("Mr.");
        expectedPerson.setlName("T");
        expectedPerson.getAddress().setStreet("Dragon Street");

        PersonDTO editPerson = new PersonDTO(expectedPerson);
        PersonDTO result = PF.editPerson(editPerson);

        //Simple Asserts
        assertEquals(expectedPerson.getId(), result.getId());
        assertEquals(expectedPerson.getId(), result.getId());
        assertTrue(expectedPerson.getfName().equals(result.getfName()));
        assertTrue(expectedPerson.getlName().equals(result.getlName()));
        assertTrue(expectedPerson.getPhone().equals(result.getPhone()));
        assertTrue(expectedPerson.getAddress().getStreet().equals(result.getStreet()));
        assertTrue(expectedPerson.getAddress().getCity().equals(result.getCity()));
        assertEquals(expectedPerson.getAddress().getZip(), result.getZip());

        //Asserts on the DB
        List<Person> dbList = pullAllPersonsFromDB();
        assertEquals(personAmount, dbList.size());
        boolean matchingIdFound = false;
        for (Person person : dbList) {
            if (Objects.equals(result.getId(), person.getId())) {
                assertTrue(result.getfName().equals(person.getfName()));
                assertTrue(result.getlName().equals(person.getlName()));
                assertTrue(result.getPhone().equals(person.getPhone()));
                assertTrue(result.getStreet().equals(person.getAddress().getStreet()));
                assertTrue(result.getCity().equals(person.getAddress().getCity()));
                assertEquals(result.getZip(), person.getAddress().getZip());
                assertEquals(weekAgo.getDate(), person.getCreated().getDate());
                assertEquals(today.getDate(), person.getLastEdited().getDate());
                matchingIdFound = true;
                break;
            }
        }
        assertTrue(matchingIdFound);
        
        //Assume 1 new address has been added to the db
        addressAmount++;
        List<Address> dblistAddress = pullAllAddressesFromDB();
        assertEquals(addressAmount, dblistAddress.size());
    }

    @Test
    public void testNegative_EditPerson_NotFound() {
        Long searchID = highestPersonId + 1;
        Person expectedPerson = p1;

        expectedPerson.setPhone("85858585");
        expectedPerson.setfName("Mr.");
        expectedPerson.setlName("T");

        expectedPerson.setId(searchID);
        PersonDTO editPerson = new PersonDTO(expectedPerson);
        assertThrows(PersonNotFoundException.class, () -> {
            PersonDTO result = PF.editPerson(editPerson);
        });
    }

    @Test
    public void testNegative_EditPerson_MissingInput_FirstName_Null() {
        Person expectedPerson = p1;

        expectedPerson.setPhone("85858585");
        expectedPerson.setfName(null);
        expectedPerson.setlName("Thomasen");

        PersonDTO editPerson = new PersonDTO(expectedPerson);
        assertThrows(MissingInputException.class, () -> {
            PersonDTO result = PF.editPerson(editPerson);
        });
    }

    @Test
    public void testNegative_EditPerson_MissingInput_FirstName_Empty() {
        Person expectedPerson = p1;

        expectedPerson.setPhone("85858585");
        expectedPerson.setfName("");
        expectedPerson.setlName("Thomasen");

        PersonDTO editPerson = new PersonDTO(expectedPerson);
        assertThrows(MissingInputException.class, () -> {
            PersonDTO result = PF.editPerson(editPerson);
        });
    }

    @Test
    public void testNegative_EditPerson_MissingInput_LastName_Null() {
        Person expectedPerson = p1;

        expectedPerson.setPhone("85858585");
        expectedPerson.setfName("Alex");
        expectedPerson.setlName(null);

        PersonDTO editPerson = new PersonDTO(expectedPerson);
        assertThrows(MissingInputException.class, () -> {
            PersonDTO result = PF.editPerson(editPerson);
        });
    }

    @Test
    public void testNegative_EditPerson_MissingInput_LastName_Empty() {
        Person expectedPerson = p1;

        expectedPerson.setPhone("85858585");
        expectedPerson.setfName("Alex");
        expectedPerson.setlName("");

        PersonDTO editPerson = new PersonDTO(expectedPerson);
        assertThrows(MissingInputException.class, () -> {
            PersonDTO result = PF.editPerson(editPerson);
        });
    }

    @Test
    public void testGetAllPersons() {
        PersonsDTO result = PF.getAllPersons();

        assertEquals(personAmount, result.getAll().size());

        for (PersonDTO personDTO : result.getAll()) {
            boolean matchingIdFound = false;
            for (Person person : personList) {
                if (Objects.equals(personDTO.getId(), person.getId())) {
                    assertTrue(personDTO.getfName().equals(person.getfName()));
                    assertTrue(personDTO.getlName().equals(person.getlName()));
                    assertTrue(personDTO.getPhone().equals(person.getPhone()));
                    matchingIdFound = true;
                    break;
                }
            }
            assertTrue(matchingIdFound);
        }
    }

    @Test
    public void testNegative_GetAllPersons_EmptyResult() {
        cleanUp();
        PersonsDTO result = PF.getAllPersons();

        int expectedSize = 0;
        assertEquals(expectedSize, result.getAll().size());
    }

    @Test
    public void testGetPerson() throws PersonNotFoundException {
        Person expectedPerson = p1;
        Long id = expectedPerson.getId();
        PersonDTO result = PF.getPerson(id);

        assertEquals(expectedPerson.getId(), result.getId());
        assertTrue(expectedPerson.getfName().equals(result.getfName()));
        assertTrue(expectedPerson.getlName().equals(result.getlName()));
        assertTrue(expectedPerson.getPhone().equals(result.getPhone()));
    }

    @Test
    public void testNegative_GetPerson_NoResult() {
        Person expectedPerson = p1;
        Long id = expectedPerson.getId();
        cleanUp();

        assertThrows(PersonNotFoundException.class, () -> {
            PersonDTO result = PF.getPerson(id);
        });

    }

}
