import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.routes;
import models.Drone;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import utilities.JsonHelper;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.*;

/**
 * Created by Benjamin on 4/03/2015.
 */
public class DroneControllerTest extends TestSuperclass {

    private static List<Drone> testDrones = new ArrayList<>();

    @BeforeClass
    public static void setup() {
        startFakeApplication();
        initialiseDatabase();
    }

    private static void initialiseDatabase() {
        // Add drones to the database
        testDrones.add(new Drone("testdrone1", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT, "x.x.x.x"));
        testDrones.add(new Drone("testdrone2", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT, "x.x.x.x"));
        testDrones.add(new Drone("testdrone3", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT, "x.x.x.x"));
        testDrones.add(new Drone("testdrone4", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT, "x.x.x.x"));
        Ebean.save(testDrones);
    }

    @AfterClass
    public static void teardown() {
        stopFakeApplication();
    }

    @Test
    public void getAll_AuthorizedRequest_SuccessfullyGetAllDrones() {
        Result result = callAction(routes.ref.DroneController.getAll(), authorizeRequest(fakeRequest(), getAdmin()));

        try {
            JsonNode node = JsonHelper.removeRootElement(contentAsString(result), Drone.class, true);
            if (node.isArray()) {
                for (int i = 0; i < testDrones.size(); ++i) {
                    Drone testDrone = testDrones.get(i);
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
    public void get_AuthorizedRequestWithValidId_SuccessfullyGetDrone() {
        Drone droneToGet = testDrones.get(testDrones.size()-1);
        Result result = callAction(routes.ref.DroneController.get(droneToGet.getId()),
                authorizeRequest(fakeRequest(), getAdmin()));

        try {
            JsonNode node = JsonHelper.removeRootElement(contentAsString(result), Drone.class, false);
            Drone d = Json.fromJson(node, Drone.class);
            assertThat(d).isEqualTo(droneToGet);
        } catch(JsonHelper.InvalidJSONException ex) {
            Assert.fail("Invalid json exception: " + ex.getMessage());
        }
    }

    @Test
    public void get_AuthorizedRequestWithInvalidId_NotFoundReturned() {
        Result result = callAction(routes.ref.DroneController.get(testDrones.size()+1000),
                authorizeRequest(fakeRequest(), getAdmin()));
        assertThat(status(result)).isEqualTo(Http.Status.NOT_FOUND);
    }

    @Test
    public void create_AuthorizedRequest_DroneCreated() {
        Drone droneToBeAdded =
                new Drone("newDrone", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT,"ipAddress");
        JsonNode node = JsonHelper.createJsonNode(droneToBeAdded, Drone.class);

        Result result = callAction(routes.ref.DroneController.create(),
                authorizeRequest(fakeRequest().withJsonBody(node), getAdmin()));

        try {
            JsonNode receivedNode = JsonHelper.removeRootElement(contentAsString(result), Drone.class, false);
            Drone d = Json.fromJson(receivedNode, Drone.class);
            droneToBeAdded.setId(d.getId()); // bypass id check, because droneToBeAdded has no id
            assertThat(d).isEqualTo(droneToBeAdded);

            Drone fetchedDrone = Drone.FIND.byId(d.getId());
            assertThat(fetchedDrone).isEqualTo(droneToBeAdded);
        } catch(JsonHelper.InvalidJSONException ex) {
            Assert.fail("Invalid json exception: " + ex.getMessage());
        }
    }

    @Test
    public void update_AuthorizedRequestWithValidId_DroneUpdated() {
        Drone d = new Drone("test1", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT,"address1");
        d.save();
        d.setName("test2");
        d.setAddress("address2");
        JsonNode node = JsonHelper.createJsonNode(d, Drone.class);
        Result result = callAction(routes.ref.DroneController.update(d.getId()),
                authorizeRequest(fakeRequest().withJsonBody(node), getAdmin()));

        try {
            JsonNode receivedNode = JsonHelper.removeRootElement(contentAsString(result), Drone.class, false);
            Drone receivedDrone = Json.fromJson(receivedNode, Drone.class);
            assertThat(receivedDrone).isEqualTo(d);

            Drone fetchedDrone = Drone.FIND.byId(d.getId());
            assertThat(fetchedDrone).isEqualTo(d);
        } catch(JsonHelper.InvalidJSONException ex) {
            Assert.fail("Invalid json exception: " + ex.getMessage());
        }
    }

    @Test
     public void delete_AuthorizedRequestWithValidId_DroneDeleted() {
        Drone droneToBeRemoved =
                new Drone("remove this drone", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT, "x.x.x.x");
        droneToBeRemoved.save();

        callAction(routes.ref.DroneController.delete(droneToBeRemoved.getId()),
                authorizeRequest(fakeRequest(), getAdmin()));

        long amount = Drone.FIND.all().stream().filter(d -> d.getId().equals(droneToBeRemoved.getId())).count();
        assertThat(amount).isEqualTo(0);
    }

    @Test
    public void testConnection_AuthorizedRequest_CorrectConnectionReceived() {
        for(Drone drone : testDrones) {
            Result r = callAction(routes.ref.DroneController.testConnection(drone.getId()),
                    authorizeRequest(fakeRequest(), getAdmin()));
            try {
                JsonNode node = JsonHelper.removeRootElement(contentAsString(r), Drone.class, false);

                boolean status = node.get("connection").asBoolean();
                assertThat(status).isEqualTo(drone.testConnection());
            } catch(JsonHelper.InvalidJSONException ex) {
                Assert.fail("Invalid json exception: " + ex.getMessage());
                break;
            }
        }
    }

    @Test
    public void battery_AuthorizedRequest_CorrectBatteryStatusReceived() {
        for(Drone drone : testDrones) {
            Result r = callAction(routes.ref.DroneController.battery(drone.getId()),
                    authorizeRequest(fakeRequest(), getAdmin()));
            try {
                JsonNode node = JsonHelper.removeRootElement(contentAsString(r), Drone.class, false);

                int status = node.get("battery").intValue();
                assertThat(status).isEqualTo(drone.getBatteryPercentage());
            } catch(JsonHelper.InvalidJSONException ex) {
                Assert.fail("Invalid json exception: " + ex.getMessage());
                break;
            }
        }
    }

    @Test
    public void cameraCapture_AuthorizedRequest_CorrectCaptureReceived() {
        for(Drone drone : testDrones) {
            Result r = callAction(routes.ref.DroneController.cameraCapture(drone.getId()),
                    authorizeRequest(fakeRequest(), getAdmin()));
            try {
                JsonNode node = JsonHelper.removeRootElement(contentAsString(r), Drone.class, false);

                String capture = node.get("cameraCapture").asText();
                assertThat(capture).isEqualTo(drone.getCameraCapture());
            } catch(JsonHelper.InvalidJSONException ex) {
                Assert.fail("Invalid json exception: " + ex.getMessage());
                break;
            }
        }
    }

}
