package facades;

import dto.PersonDTO;
import dto.PersonsDTO;
import entities.Address;
import entities.Person;
import exceptions.MissingInputException;
import exceptions.PersonNotFoundException;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

/**
 *
 * Rename Class to a relevant name Add add relevant facade methods
 */
public class PersonFacade implements IPersonFacade {

    private static PersonFacade instance;
    private static EntityManagerFactory emf;

    //Private Constructor to ensure Singleton
    private PersonFacade() {
    }

    /**
     *
     * @param _emf
     * @return an instance of this facade class.
     */
    public static PersonFacade getPersonFacade(EntityManagerFactory _emf) {
        if (instance == null) {
            emf = _emf;
            instance = new PersonFacade();
        }
        return instance;
    }

    private EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    //TODO Remove/Change this before use
    public long getPersonCount() {
        EntityManager em = emf.createEntityManager();
        try {
            long renameMeCount = (long) em.createQuery("SELECT COUNT(p) FROM Person p").getSingleResult();
            return renameMeCount;
        } finally {
            em.close();
        }

    }

//    @Override
//    public PersonDTO addPerson(String fName, String lName, String phone) throws MissingInputException {
//        boolean fNameMissing = (fName == null || fName.isEmpty());
//        boolean lNameMissing = (lName == null || lName.isEmpty());
//        if (fNameMissing || lNameMissing) {
//            throw new MissingInputException("First Name and/or Last Name is missing");
//        }
//        Person person = new Person(fName, lName, phone);
//        EntityManager em = emf.createEntityManager();
//        try {
//            em.getTransaction().begin();
//            em.persist(person);
//            em.getTransaction().commit();
//            PersonDTO result = new PersonDTO(person);
//            return result;
//        } finally {
//            em.close();
//        }
//    }

    @Override
    public PersonDTO deletePerson(Long id) throws PersonNotFoundException {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Person p = em.find(Person.class, id);
            em.remove(p);
            em.getTransaction().commit();
            return new PersonDTO(p);
        } catch (IllegalArgumentException e) {
            throw new PersonNotFoundException("Could not delete, provided id does not exist");
        } finally {
            em.close();
        }

    }

    @Override
    public PersonDTO getPerson(Long id) throws PersonNotFoundException {
        EntityManager em = emf.createEntityManager();
        try {
            Person p = em.find(Person.class, id);
            return new PersonDTO(p);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new PersonNotFoundException("No person with provided id found");
        } finally {
            em.close();
        }
    }

    @Override
    public PersonsDTO getAllPersons() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Person> tq = em.createQuery("SELECT p FROM Person p", Person.class);
            List<Person> queryList = tq.getResultList();
            PersonsDTO resultList = new PersonsDTO(queryList);
            return resultList;
        } finally {
            em.close();
        }
    }

    @Override
    public PersonDTO editPerson(PersonDTO p) throws PersonNotFoundException, MissingInputException {
        CheckValidInput(p.getfName(), p.getlName(), p.getStreet(), p.getCity(), p.getZip());
        EntityManager em = emf.createEntityManager();
        try {
            Person person = em.find(Person.class, p.getId());
            em.getTransaction().begin();
            person.setLastEdited(new Date());
            person.setPhone(p.getPhone());
            person.setlName(p.getlName());
            person.setfName(p.getfName());
            person.setAddress(FindOrMakeAddress(p.getStreet(), p.getCity(), p.getZip(), em));
            em.getTransaction().commit();
            p = new PersonDTO(person);
            return p;
        } catch (Exception e) {
            throw new PersonNotFoundException("Could not edit, provided id does not exist");
        } finally {
            em.close();
        }
    }

    @Override
    public PersonDTO addPerson(String fName, String lName, String phone, String street, String city, Integer zip) throws MissingInputException {
        CheckValidInput(fName, lName, street, city, zip);
        EntityManager em = emf.createEntityManager();
        try {
            Person person = new Person(fName, lName, phone);
            Address address = FindOrMakeAddress(street, city, zip, em);
            person.addAddress(address);
            em.getTransaction().begin();
            em.persist(person);
            em.getTransaction().commit();
            PersonDTO result = new PersonDTO(person);
            return result;
        } finally {
            em.close();
        }
    }

    private Address FindOrMakeAddress(String street, String city, Integer zip, EntityManager em) {
        try {
            TypedQuery<Address> tq = em.createQuery("SELECT a FROM Address a WHERE a.street = :street AND a.city = :city AND a.zip = :zip", Address.class);
            tq.setParameter("street", street);
            tq.setParameter("city", city);
            tq.setParameter("zip", zip);
            return tq.getSingleResult();
        } catch (NoResultException e) {
            return new Address(street, city, zip);
        }
    }

    private void CheckValidInput(String fName, String lName, String street, String city, Integer zip) throws MissingInputException {
        boolean nameMissing = (fName == null || fName.isEmpty() || lName == null || lName.isEmpty());
        boolean addressMissing = (street == null || street.isEmpty() || city == null || city.isEmpty() || zip == null);
        if (nameMissing) {
            throw new MissingInputException("First Name and/or Last Name is missing");
        } else if (addressMissing) {
            throw new MissingInputException("Address Information is missing");
        }
    }

}
