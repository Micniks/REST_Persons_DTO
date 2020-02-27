package rest;

import com.google.gson.Gson;
import dto.PersonDTO;
import dto.PersonsDTO;
import entities.Person;
import utils.EMF_Creator;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.parsing.Parser;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import static org.hamcrest.Matchers.equalTo;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.EMF_Creator.DbSelector;
import utils.EMF_Creator.Strategy;

//Uncomment the line below, to temporarily disable this test
//@Disabled
public class PersonResourceTest {

    private static final int SERVER_PORT = 7777;
    private static final String SERVER_URL = "http://localhost/api";
    private static Person p1, p2;
    private static List<Person> personList = new ArrayList();
    private static Long highestId;

    static final URI BASE_URI = UriBuilder.fromUri(SERVER_URL).port(SERVER_PORT).build();
    private static HttpServer httpServer;
    private static EntityManagerFactory emf;

    static HttpServer startServer() {
        ResourceConfig rc = ResourceConfig.forApplication(new ApplicationConfig());
        return GrizzlyHttpServerFactory.createHttpServer(BASE_URI, rc);
    }

    @BeforeAll
    public static void setUpClass() {
        //This method must be called before you request the EntityManagerFactory
        EMF_Creator.startREST_TestWithDB();
        emf = EMF_Creator.createEntityManagerFactory(DbSelector.TEST, Strategy.CREATE);

        httpServer = startServer();
        //Setup RestAssured
        RestAssured.baseURI = SERVER_URL;
        RestAssured.port = SERVER_PORT;
        RestAssured.defaultParser = Parser.JSON;
    }

    @AfterAll
    public static void closeTestServer() {
        //System.in.read();
        //Don't forget this, if you called its counterpart in @BeforeAll
        EMF_Creator.endREST_TestWithDB();
        httpServer.shutdownNow();
    }

    // Setup the DataBase (used by the test-server and this test) in a known state BEFORE EACH TEST
    //TODO -- Make sure to change the EntityClass used below to use YOUR OWN (renamed) Entity class
    @BeforeEach
    public void setUp() {
        EntityManager em = emf.createEntityManager();
        p1 = new Person("Jack", "Daniels", "26 68 84 42");
        p2 = new Person("Captain", "Morgan", "66 66 66 66");
        personList.clear();
        personList.add(p1);
        personList.add(p2);
        try {
            em.getTransaction().begin();
            em.createNamedQuery("Person.deleteAllRows").executeUpdate();
            em.persist(p1);
            em.persist(p2);
            em.getTransaction().commit();
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

    @Test
    public void testServerIsUp() {
        System.out.println("Testing is server UP");
        given().when().get("/person").then().statusCode(200);
    }

    //This test assumes the database contains two rows
    @Test
    public void testDummyMsg() throws Exception {
        given()
                .contentType("application/json")
                .get("/person/").then()
                .assertThat()
                .statusCode(HttpStatus.OK_200.getStatusCode())
                .body("msg", equalTo("Hello World"));
    }

    @Test
    public void testCount() throws Exception {
        given()
                .contentType("application/json")
                .get("/person/count").then()
                .assertThat()
                .statusCode(HttpStatus.OK_200.getStatusCode())
                .body("count", equalTo(2));
    }

    @Test
    public void testGetPersonFromID() throws Exception {
        Person expectedPerson = p1;

        given()
                .contentType("application/json")
                .get("/person/id/" + expectedPerson.getId()).then()
                .assertThat()
                .statusCode(HttpStatus.OK_200.getStatusCode())
                .body("fName", equalTo(expectedPerson.getfName()))
                .body("lName", equalTo(expectedPerson.getlName()))
                .body("phone", equalTo(expectedPerson.getPhone()))
                .body("id", equalTo(Math.toIntExact(expectedPerson.getId())));
    }

    @Test
    public void testGetAll() throws Exception {
        PersonsDTO dbList = given()
                .contentType("application/json")
                .get("/person/all").then()
                .assertThat()
                .statusCode(HttpStatus.OK_200.getStatusCode())
                .extract().body().as(PersonsDTO.class);

        for (Person person : personList) {
            boolean matchingIdFound = false;
            for (PersonDTO dbPerson : dbList.getAll()) {
                if (Objects.equals(person.getId(), dbPerson.getId())) {
                    assertTrue(dbPerson.getfName().equals(person.getfName()));
                    assertTrue(dbPerson.getlName().equals(person.getlName()));
                    assertTrue(dbPerson.getPhone().equals(person.getPhone()));
                    matchingIdFound = true;
                    break;
                }
            }
            assertTrue(matchingIdFound);
        }
    }

    @Test
    public void testAddPerson() throws Exception {
        String newFirstName = "Peter";
        String newLastName = "Jackson";
        String newPhone = "96396336";
        highestId++;
        PersonDTO expectedPersonDTO = new PersonDTO(null, newFirstName, newLastName, newPhone);
        given()
                .contentType("application/json")
                .body(expectedPersonDTO)
                .when()
                .post("/person/add")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK_200.getStatusCode())
                .body("fName", equalTo(expectedPersonDTO.getfName()))
                .body("lName", equalTo(expectedPersonDTO.getlName()))
                .body("phone", equalTo(expectedPersonDTO.getPhone()))
                .body("id", equalTo(Math.toIntExact(highestId)));

        given()
                .contentType("application/json")
                .get("/person/count").then()
                .assertThat()
                .statusCode(HttpStatus.OK_200.getStatusCode())
                .body("count", equalTo(3));

    }

    @Test
    public void testEditPerson() throws Exception {
        Person expectedPerson = p2;
        String newFirstName = "Peter";
        String newLastName = "Jackson";
        String newPhone = "96396336";
        PersonDTO expectedPersonDTO = new PersonDTO(expectedPerson.getId(), newFirstName, newLastName, newPhone);
        given()
                .contentType("application/json")
                .body(expectedPersonDTO)
                .when()
                .post("/person/edit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK_200.getStatusCode())
                .body("fName", equalTo(expectedPersonDTO.getfName()))
                .body("lName", equalTo(expectedPersonDTO.getlName()))
                .body("phone", equalTo(expectedPersonDTO.getPhone()))
                .body("id", equalTo(Math.toIntExact(expectedPersonDTO.getId())));

    }

    @Test
    public void testDeletePerson() throws Exception {
        Person expectedPerson = p2;

        given()
                .contentType("application/json")
                .body(new PersonDTO(expectedPerson))
                .when()
                .post("/person/delete")
                .then()
                .assertThat()
                .statusCode(HttpStatus.OK_200.getStatusCode())
                .body("fName", equalTo(expectedPerson.getfName()))
                .body("lName", equalTo(expectedPerson.getlName()))
                .body("phone", equalTo(expectedPerson.getPhone()))
                .body("id", equalTo(Math.toIntExact(expectedPerson.getId())));

        given()
                .contentType("application/json")
                .get("/person/count").then()
                .assertThat()
                .statusCode(HttpStatus.OK_200.getStatusCode())
                .body("count", equalTo(1));
    }
}
