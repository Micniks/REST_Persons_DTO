/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facades;

import dto.PersonDTO;
import dto.PersonsDTO;
import entities.Person;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.EMF_Creator;

/**
 *
 * @author Michael N. Korsgaard
 */
public class PersonFacadeTest {

    private static EntityManagerFactory EMF;
    private static IPersonFacade PF;
    private static Person p1, p2, p3;
    private static List<Person> personList = new ArrayList();
    private static int personAmount;
    private static Long highestId;
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
        p1 = new Person("Jack", "Daniels", "78451293");
        p2 = new Person("Captain", "Morgan", "97643185");
        p3 = new Person("Michael", "Jackson", "19374628");
        personList = new ArrayList(Arrays.asList(new Person[]{p1, p2, p3}));
        personAmount = personList.size();

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

        highestId = 0L;
        for (Person person : personList) {
            if (person.getId() > highestId) {
                highestId = person.getId();
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
            em.createQuery("DELETE FROM Person").executeUpdate();
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    private List<Person> pullAllFromDB() {
        //Getting the new list from the database
        em = EMF.createEntityManager();
        List<Person> dbList = null;
        try {
            TypedQuery<Person> tq = em.createQuery("SELECT p FROM Person p", Person.class);
            dbList = tq.getResultList();
        } catch (Exception e) {
            fail("Could not retrive list from DB after adding Person to DB");
        } finally {
            em.close();
        }
        return dbList;
    }

    @Test
    public void testAddPerson() {
        String fName = "Jesus";
        String lName = "Christ";
        String phone = "13974268";

        PersonDTO result = PF.addPerson(fName, lName, phone);

        //Changing expectations
        highestId++;
        personAmount++;

        //Simple Asserts
        assertEquals(highestId, result.getId());
        assertTrue(fName.equals(result.getfName()));
        assertTrue(lName.equals(result.getlName()));
        assertTrue(phone.equals(result.getPhone()));

        //Asserts on the DB
        List<Person> dbList = pullAllFromDB();
        assertEquals(personAmount, dbList.size());
        boolean matchingIdFound = false;
        for (Person person : dbList) {
            if (Objects.equals(person.getId(), result.getId())) {
                assertTrue(result.getfName().equals(person.getfName()));
                assertTrue(result.getlName().equals(person.getlName()));
                assertTrue(result.getPhone().equals(person.getPhone()));
                matchingIdFound = true;
                break;
            }
        }
        assertTrue(matchingIdFound);

    }

    @Test
    public void testDeletePerson() {
        Long id = p1.getId();
        PersonDTO result = PF.deletePerson(id);

        //Simple Asserts
        assertEquals(id, result.getId());
        assertTrue(p1.getfName().equals(result.getfName()));
        assertTrue(p1.getlName().equals(result.getlName()));
        assertTrue(p1.getPhone().equals(result.getPhone()));

        //Changing expectations
        personAmount--;

        //Asserts on the DB
        List<Person> dbList = pullAllFromDB();
        assertEquals(personAmount, dbList.size());
        for (Person person : dbList) {
            if (Objects.equals(person.getId(), result.getId())) {
                fail("Deleted Person was still in DB");
            }
        }
    }

    @Test
    public void testEditPerson() {
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

        //Asserts on the DB
        List<Person> dbList = pullAllFromDB();
        assertEquals(personAmount, dbList.size());
        boolean matchingIdFound = false;
        for (Person person : dbList) {
            if (Objects.equals(result.getId(), person.getId())) {
                assertTrue(result.getfName().equals(person.getfName()));
                assertTrue(result.getlName().equals(person.getlName()));
                assertTrue(result.getPhone().equals(person.getPhone()));
                assertEquals(weekAgo.getDate(), person.getCreated().getDate());
                assertEquals(today.getDate(), person.getLastEdited().getDate());
                matchingIdFound = true;
                break;
            }
        }
        assertTrue(matchingIdFound);
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
    public void testGetPerson() {
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

        assertThrows(NullPointerException.class, () -> {
            PersonDTO result = PF.getPerson(id);
        });

    }

}
