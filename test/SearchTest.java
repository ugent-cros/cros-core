import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.routes;
import drones.models.BepopDriver;
import models.*;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import play.libs.Json;
import play.mvc.Result;
import utilities.JsonHelper;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.*;

/**
 * Created by matthias on 19/03/2015.
 */
public class SearchTest extends TestSuperclass {

    private static List<Drone> testDrones = new ArrayList<>();
    private static List<Assignment> testAssignments = new ArrayList<>();
    private static List<Basestation> testBasestations = new ArrayList<>();
    private static List<User> testUsers = new ArrayList<>();

    @BeforeClass
    public static void setup() {
        startFakeApplication();
        initialiseDatabase();
    }

    private static void initialiseDatabase() {
        // Add drones to the database
        testDrones.add(new Drone("testdrone1", Drone.Status.AVAILABLE, BepopDriver.BEPOP_TYPE,  "x.x.x.x"));
        testDrones.add(new Drone("testdrone2", Drone.Status.AVAILABLE, BepopDriver.BEPOP_TYPE,  "x.x.x.x"));
        testDrones.add(new Drone("testdrone3", Drone.Status.AVAILABLE, BepopDriver.BEPOP_TYPE,  "x.x.x.x"));
        testDrones.add(new Drone("testdrone4", Drone.Status.AVAILABLE, BepopDriver.BEPOP_TYPE,  "x.x.x.x"));
        Ebean.save(testDrones);
        // Add assignments to the database
        testAssignments.add(new Assignment());
        testAssignments.add(new Assignment());
        Ebean.save(testAssignments);
        testBasestations.add(new Basestation("station1", new Checkpoint()));
        testBasestations.add(new Basestation("station2", new Checkpoint()));
        Ebean.save(testBasestations);
        testUsers.add(new User("test1@smeifj.com", "pass1", "john1", "doe1"));
        testUsers.add(new User("test2@smeifj.com", "pass2", "john2", "doe2"));
        Ebean.save(testUsers);
    }

    @AfterClass
    public static void teardown() {
        stopFakeApplication();
    }

    @Test
    public void getAll_filterOnName_SuccessfullyGetAllDrones() {
        Result result = callAction(routes.ref.DroneController.getAll(), authorizeRequest(fakeRequest(GET, routes.DroneController.getAll().url() + "?name=1"), getAdmin()));

        try {
            JsonNode node = JsonHelper.removeRootElement(contentAsString(result), Drone.class, true);
            if (node.isArray()) {
                assertThat(node.size()).isEqualTo(1);
                Drone testDrone = testDrones.get(0);
                Drone receivedDrone = Json.fromJson(node.get(0), Drone.class);
                assertThat(testDrone.getId()).isEqualTo(receivedDrone.getId());
            } else
                Assert.fail("Returned JSON is not an array");
        } catch(JsonHelper.InvalidJSONException ex) {
            Assert.fail("Invalid json exception: " + ex.getMessage());
        }
    }

    @Test
    public void getAll_filterWithPages_SuccessfullyGetCorrectDrones() {
        int pageSize = 2;
        int page = 1;
        Result result = callAction(routes.ref.DroneController.getAll(), authorizeRequest(fakeRequest(GET, routes.DroneController.getAll().url() + "?pageSize="+ pageSize +"&page=" + page), getAdmin()));

        try {
            JsonNode node = JsonHelper.removeRootElement(contentAsString(result), Drone.class, true);
            if (node.isArray()) {
                assertThat(node.size()).isEqualTo(pageSize);
                for (int i = 0; i < pageSize; ++i) {
                    Drone testDrone = testDrones.get(page*pageSize+i);
                    Drone receivedDrone = Json.fromJson(node.get(i), Drone.class);
                    assertThat(testDrone.getId()).isEqualTo(receivedDrone.getId());
                }
            } else
                Assert.fail("Returned JSON is not an array");
        } catch(JsonHelper.InvalidJSONException ex) {
            Assert.fail("Invalid json exception: " + ex.getMessage());
        }
    }

    @Test
    public void getAll_orderOnId_appliedToDroneController() {
        Result result = callAction(routes.ref.DroneController.getAll(), authorizeRequest(fakeRequest(GET, routes.DroneController.getAll().url() + "?orderBy=id&order=desc"), getAdmin()));

        try {
            JsonNode node = JsonHelper.removeRootElement(contentAsString(result), Drone.class, true);
            if (node.isArray()) {
                Drone receivedDrone = Json.fromJson(node.get(0), Drone.class);
                assertThat(receivedDrone.getId()).isEqualTo(testDrones.get(testDrones.size() - 1).getId());
            } else
                Assert.fail("Returned JSON is not an array");
        } catch(JsonHelper.InvalidJSONException ex) {
            Assert.fail("Invalid json exception: " + ex.getMessage());
        }
    }

    @Test
     public void getAll_orderOnId_appliedToAssignmentController() {
        Result result = callAction(routes.ref.AssignmentController.getAll(), authorizeRequest(fakeRequest(GET, routes.AssignmentController.getAll().url() + "?orderBy=id&order=desc"), getAdmin()));

        try {
            JsonNode node = JsonHelper.removeRootElement(contentAsString(result), Assignment.class, true);
            if (node.isArray()) {
                Assignment received = Json.fromJson(node.get(0), Assignment.class);
                assertThat(received.getId()).isEqualTo(testAssignments.get(testAssignments.size()-1).getId());
            } else
                Assert.fail("Returned JSON is not an array");
        } catch(JsonHelper.InvalidJSONException ex) {
            Assert.fail("Invalid json exception: " + ex.getMessage());
        }
    }

    @Test
    public void getAll_orderOnId_appliedToBasestationController() {
        Result result = callAction(routes.ref.BasestationController.getAll(), authorizeRequest(fakeRequest(GET, routes.BasestationController.getAll().url() + "?orderBy=id&order=desc"), getAdmin()));

        try {
            JsonNode node = JsonHelper.removeRootElement(contentAsString(result), Basestation.class, true);
            if (node.isArray()) {
                Basestation received = Json.fromJson(node.get(0), Basestation.class);
                assertThat(received.getId()).isEqualTo(testBasestations.get(testBasestations.size()-1).getId());
            } else
                Assert.fail("Returned JSON is not an array");
        } catch(JsonHelper.InvalidJSONException ex) {
            Assert.fail("Invalid json exception: " + ex.getMessage());
        }
    }

    @Test
    public void getAll_orderOnId_appliedToUserController() {
        Result result = callAction(routes.ref.UserController.getAll(), authorizeRequest(fakeRequest(GET, routes.UserController.getAll().url() + "?orderBy=id&order=desc"), getAdmin()));

        try {
            JsonNode node = JsonHelper.removeRootElement(contentAsString(result), User.class, true);
            if (node.isArray()) {
                User received = Json.fromJson(node.get(0), User.class);
                assertThat(received.getId()).isEqualTo(testUsers.get(testUsers.size()-1).getId());
            } else
                Assert.fail("Returned JSON is not an array");
        } catch(JsonHelper.InvalidJSONException ex) {
            Assert.fail("Invalid json exception: " + ex.getMessage());
        }
    }

}
