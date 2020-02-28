package entities;

import exceptions.MissingInputException;
import facades.IPersonFacade;
import facades.PersonFacade;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import utils.EMF_Creator;

public class MakeDummies {

    private static final EntityManagerFactory EMF = EMF_Creator.createEntityManagerFactory(
            "pu",
            "jdbc:mysql://localhost:3307/Flow2Week1",
            "dev",
            "ax2",
            EMF_Creator.Strategy.CREATE);
    private static final IPersonFacade FACADE = PersonFacade.getPersonFacade(EMF);

    public static void main(String[] args) throws MissingInputException {

        EntityManager em = EMF.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM Person").executeUpdate();
            em.getTransaction().commit();
        } finally {
            em.close();
        }

        FACADE.addPerson("Jack", "Daniels", "78451293", "Hidden Street", "Hidden City", 2000);
        FACADE.addPerson("Captain", "Morgan", "97643185", "Hidden Street", "Hidden City", 2000);
        FACADE.addPerson("Michael", "Jackson", "19374628", "Hidden Street", "Hidden City", 2000);

    }
}
