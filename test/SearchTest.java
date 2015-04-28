import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import drones.models.BepopDriver;
import models.Assignment;
import models.Basestation;
import models.Drone;
import models.User;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import play.libs.Json;
import play.mvc.Result;
import utilities.JsonHelper;
import controllers.*;

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
        testBasestations.add(new Basestation("station1", 0.0,0.0,0.0));
        testBasestations.add(new Basestation("station2", 1.0,2.0,3.0));
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
    public void getAll_noFilter_TotalOnlyAvailableWhenSetOnDrones() {
        Result resultWithTotal = callAction(routes.ref.DroneController.getAll(), authorizeRequest(fakeRequest(GET, routes.DroneController.getAll().url() + "?total=true"), getAdmin()));
        Result resultWithoutTotal = callAction(routes.ref.DroneController.getAll(), authorizeRequest(fakeRequest(GET, routes.DroneController.getAll().url() + "?total=false"), getAdmin()));
        Result resultWithoutTotal2 = callAction(routes.ref.DroneController.getAll(), authorizeRequest(fakeRequest(GET, routes.DroneController.getAll().url()), getAdmin()));

        try {
            ObjectNode nodeWithoutTotal = (ObjectNode) JsonHelper.removeRootElement(contentAsString(resultWithoutTotal), Drone.class, false);
            assertThat(nodeWithoutTotal.get("total")).isEqualTo(null);
            ObjectNode nodeWithoutTotal2 = (ObjectNode) JsonHelper.removeRootElement(contentAsString(resultWithoutTotal2), Drone.class, false);
            assertThat(nodeWithoutTotal2.get("total")).isEqualTo(null);
            ObjectNode nodeWithTotal = (ObjectNode) JsonHelper.removeRootElement(contentAsString(resultWithTotal), Drone.class, false);
            assertThat(nodeWithTotal.get("total").asInt()).isEqualTo(Drone.FIND.all().size());
        } catch (JsonHelper.InvalidJSONException e) {
            Assert.fail("Invalid json exception: " + e.getMessage());
        }
    }

    @Test
    public void getAll_noFilter_TotalOnlyAvailableWhenSetOnAssignemnts() {
        Result resultWithTotal = callAction(routes.ref.AssignmentController.getAll(), authorizeRequest(fakeRequest(GET, routes.AssignmentController.getAll().url() + "?total=true"), getAdmin()));
        Result resultWithoutTotal = callAction(routes.ref.AssignmentController.getAll(), authorizeRequest(fakeRequest(GET, routes.AssignmentController.getAll().url() + "?total=false"), getAdmin()));
        Result resultWithoutTotal2 = callAction(routes.ref.AssignmentController.getAll(), authorizeRequest(fakeRequest(GET, routes.AssignmentController.getAll().url()), getAdmin()));

        try {
            ObjectNode nodeWithoutTotal = (ObjectNode) JsonHelper.removeRootElement(contentAsString(resultWithoutTotal), Assignment.class, false);
            assertThat(nodeWithoutTotal.get("total")).isEqualTo(null);
            ObjectNode nodeWithoutTotal2 = (ObjectNode) JsonHelper.removeRootElement(contentAsString(resultWithoutTotal2), Assignment.class, false);
            assertThat(nodeWithoutTotal2.get("total")).isEqualTo(null);
            ObjectNode nodeWithTotal = (ObjectNode) JsonHelper.removeRootElement(contentAsString(resultWithTotal), Assignment.class, false);
            assertThat(nodeWithTotal.get("total").asInt()).isEqualTo(Assignment.FIND.all().size());
        } catch (JsonHelper.InvalidJSONException e) {
            Assert.fail("Invalid json exception: " + e.getMessage());
        }
    }

    @Test
    public void getAll_noFilter_TotalOnlyAvailableWhenSetOnBasestations() {
        Result resultWithTotal = callAction(routes.ref.BasestationController.getAll(), authorizeRequest(fakeRequest(GET, routes.BasestationController.getAll().url() + "?total=true"), getAdmin()));
        Result resultWithoutTotal = callAction(routes.ref.BasestationController.getAll(), authorizeRequest(fakeRequest(GET, routes.BasestationController.getAll().url() + "?total=false"), getAdmin()));
        Result resultWithoutTotal2 = callAction(routes.ref.BasestationController.getAll(), authorizeRequest(fakeRequest(GET, routes.BasestationController.getAll().url()), getAdmin()));

        try {
            ObjectNode nodeWithoutTotal = (ObjectNode) JsonHelper.removeRootElement(contentAsString(resultWithoutTotal), Basestation.class, false);
            assertThat(nodeWithoutTotal.get("total")).isEqualTo(null);
            ObjectNode nodeWithoutTotal2 = (ObjectNode) JsonHelper.removeRootElement(contentAsString(resultWithoutTotal2), Basestation.class, false);
            assertThat(nodeWithoutTotal2.get("total")).isEqualTo(null);
            ObjectNode nodeWithTotal = (ObjectNode) JsonHelper.removeRootElement(contentAsString(resultWithTotal), Basestation.class, false);
            assertThat(nodeWithTotal.get("total").asInt()).isEqualTo(Basestation.FIND.all().size());
        } catch (JsonHelper.InvalidJSONException e) {
            Assert.fail("Invalid json exception: " + e.getMessage());
        }
    }

    @Test
    public void getAll_noFilter_TotalOnlyAvailableWhenSetOnUsers() {
        Result resultWithTotal = callAction(routes.ref.UserController.getAll(), authorizeRequest(fakeRequest(GET, routes.UserController.getAll().url() + "?total=true"), getAdmin()));
        Result resultWithoutTotal = callAction(routes.ref.UserController.getAll(), authorizeRequest(fakeRequest(GET, routes.UserController.getAll().url() + "?total=false"), getAdmin()));
        Result resultWithoutTotal2 = callAction(routes.ref.UserController.getAll(), authorizeRequest(fakeRequest(GET, routes.UserController.getAll().url()), getAdmin()));

        try {
            ObjectNode nodeWithoutTotal = (ObjectNode) JsonHelper.removeRootElement(contentAsString(resultWithoutTotal), User.class, false);
            assertThat(nodeWithoutTotal.get("total")).isEqualTo(null);
            ObjectNode nodeWithoutTotal2 = (ObjectNode) JsonHelper.removeRootElement(contentAsString(resultWithoutTotal2), User.class, false);
            assertThat(nodeWithoutTotal2.get("total")).isEqualTo(null);
            ObjectNode nodeWithTotal = (ObjectNode) JsonHelper.removeRootElement(contentAsString(resultWithTotal), User.class, false);
            assertThat(nodeWithTotal.get("total").asInt()).isEqualTo(User.FIND.all().size());
        } catch (JsonHelper.InvalidJSONException e) {
            Assert.fail("Invalid json exception: " + e.getMessage());
        }
    }

    @Test
    public void getAll_filterOnNonExistingName_TotalIsZero() {
        Result result = callAction(routes.ref.DroneController.getAll(), authorizeRequest(fakeRequest(GET, routes.DroneController.getAll().url() + "?total=true&name=unknown"), getAdmin()));

        try {
            ObjectNode resultNode = (ObjectNode) JsonHelper.removeRootElement(contentAsString(result), Drone.class, false);
            assertThat(resultNode.get("total").asInt()).isEqualTo(0);
        } catch (JsonHelper.InvalidJSONException e) {
            Assert.fail("Invalid json exception: " + e.getMessage());
        }
    }

    @Test
    public void getAll_filterOnName_TotalIsCorrect() {
        Result result = callAction(routes.ref.DroneController.getAll(), authorizeRequest(fakeRequest(GET, routes.DroneController.getAll().url() + "?total=true&name=1"), getAdmin()));

        try {
            ObjectNode resultNode = (ObjectNode) JsonHelper.removeRootElement(contentAsString(result), Drone.class, false);
            assertThat(resultNode.get("total").asInt()).isEqualTo(1);
        } catch (JsonHelper.InvalidJSONException e) {
            Assert.fail("Invalid json exception: " + e.getMessage());
        }
    }

    @Test
    public void getAll_filterOnPage_TotalIsNotFiltered() {
        Result result = callAction(routes.ref.DroneController.getAll(), authorizeRequest(fakeRequest(GET, routes.DroneController.getAll().url() + "?total=true&page=0&pageSize=1"), getAdmin()));

        try {
            ObjectNode resultNode = (ObjectNode) JsonHelper.removeRootElement(contentAsString(result), Drone.class, false);
            assertThat(resultNode.get("total").asInt()).isEqualTo(Drone.FIND.all().size());
        } catch (JsonHelper.InvalidJSONException e) {
            Assert.fail("Invalid json exception: " + e.getMessage());
        }
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
