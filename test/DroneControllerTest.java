import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.routes;
import models.Assignment;
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
    public static void initialiseDroneControllerTest() {
        // Start application
        startFakeApplication();
        // Add drones to the database
        testDrones.add(new Drone("testdrone1", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT, "x.x.x.x"));
        testDrones.add(new Drone("testdrone2", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT, "x.x.x.x"));
        testDrones.add(new Drone("testdrone3", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT, "x.x.x.x"));
        testDrones.add(new Drone("testdrone4", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT, "x.x.x.x"));
        Ebean.save(testDrones);
    }

    @AfterClass
    public static void finishDroneControllerTest() {
        // End application
        stopFakeApplication();
    }

    @Test
    public void getAll_DatabaseFilledWithDrones_SuccessfullyGetAllDrones() {
        Result result = callAction(routes.ref.DroneController.getAll(), authorizeRequest(fakeRequest(), getAdmin()));

        JsonNode node = JsonHelper.removeRootElement(contentAsString(result), Drone.class);
        if (node.isArray()) {
            for (int i = 0; i < testDrones.size(); ++i) {
                Drone testDrone = testDrones.get(i);
                Assignment receivedDrone = Json.fromJson(node.get(i), Assignment.class);
                assertThat(testDrone.id).isEqualTo(receivedDrone.id);
            }
        } else
            Assert.fail("Returned JSON is not an array");
    }

    @Test
    public void getDrone_DatabaseFilledWithDrones_SuccessfullyGetDrone() {
        Drone droneToGet = testDrones.get(testDrones.size()-1);
        Result result = callAction(routes.ref.DroneController.get(droneToGet.id), authorizeRequest(fakeRequest(), getAdmin()));

        JsonNode node = JsonHelper.removeRootElement(contentAsString(result), Drone.class);
        Drone d = Json.fromJson(node, Drone.class);
        assertThat(d).isEqualTo(droneToGet);
    }

    @Test
    public void getDrone_DatabaseFilledWithDrones_DroneIdNotAvailable() {
        Result result = callAction(routes.ref.DroneController.get(testDrones.size()+1000), authorizeRequest(fakeRequest(), getAdmin()));
        assertThat(status(result)).isEqualTo(Http.Status.NOT_FOUND);
    }

    @Test
    public void addDrone_DatabaseFilledWithDrones_ReturnedDroneIsCorrect() {
        Drone droneToBeAdded = new Drone("newDrone", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT,"ipAddress");
        JsonNode node = JsonHelper.addRootElement(Json.toJson(droneToBeAdded), Drone.class);

        Result result = callAction(routes.ref.DroneController.create(), authorizeRequest(fakeRequest().withJsonBody(node), getAdmin()));
        JsonNode receivedNode = JsonHelper.removeRootElement(contentAsString(result), Drone.class);
        Drone d = Json.fromJson(receivedNode, Drone.class);
        droneToBeAdded.id = d.id; // bypass id check, because droneToBeAdded has no id
        assertThat(d).isEqualTo(droneToBeAdded);

        Drone fetchedDrone = Drone.find.byId(d.id);
        assertThat(fetchedDrone).isEqualTo(droneToBeAdded);
    }

    @Test
    public void updateDrone_DatabaseFilledWithDrones_UpdatesAreCorrect() {
        Drone d = new Drone("test1", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT,"address1");
        d.save();
        d.name = "test2";
        d.name = "address2";
        JsonNode node = JsonHelper.addRootElement(Json.toJson(d), Drone.class);
        Result result = callAction(routes.ref.DroneController.update(d.id), authorizeRequest(fakeRequest().withJsonBody(node), getAdmin()));

        JsonNode receivedNode = JsonHelper.removeRootElement(contentAsString(result), Drone.class);
        Drone receivedDrone = Json.fromJson(receivedNode, Drone.class);
        assertThat(receivedDrone).isEqualTo(d);

        Drone fetchedDrone = Drone.find.byId(d.id);
        assertThat(fetchedDrone).isEqualTo(d);
    }

    @Test
     public void deleteDrone_DatabaseFilledWithDrones_DroneIsDeleted() {
        Drone droneToBeRemoved = new Drone("remove this drone", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT, "x.x.x.x");
        droneToBeRemoved.save();

        callAction(routes.ref.DroneController.delete(droneToBeRemoved.id), authorizeRequest(fakeRequest(), getAdmin()));

        long amount = Drone.find.all().stream().filter(d -> d.id == droneToBeRemoved.id).count();
        assertThat(amount).isEqualTo(0);
    }

    @Test
    public void testConnection_DatabaseFilledWithDrones_CorrectConnectionReceived() {
        for(Drone drone : testDrones) {
            Result r = callAction(routes.ref.DroneController.testConnection(drone.id), authorizeRequest(fakeRequest(), getAdmin()));
            JsonNode node = JsonHelper.removeRootElement(contentAsString(r), Drone.class);

            boolean status = node.get("connection").asBoolean();
            assertThat(status).isEqualTo(drone.testConnection());
        }
    }

    @Test
    public void battery_DatabaseFilledWithDrones_CorrectBatteryStatusReceived() {
        for(Drone drone : testDrones) {
            Result r = callAction(routes.ref.DroneController.battery(drone.id), authorizeRequest(fakeRequest(), getAdmin()));
            JsonNode node = JsonHelper.removeRootElement(contentAsString(r), Drone.class);

            int status = node.get("battery").intValue();
            assertThat(status).isEqualTo(drone.getBatteryStatus());
        }
    }

    @Test
    public void cameraCapture_DatabaseFilledWithDrones_CorrectCaptureReceived() {
        for(Drone drone : testDrones) {
            Result r = callAction(routes.ref.DroneController.cameraCapture(drone.id), authorizeRequest(fakeRequest(), getAdmin()));
            JsonNode node = JsonHelper.removeRootElement(contentAsString(r), Drone.class);

            String capture = node.get("cameraCapture").asText();
            assertThat(capture).isEqualTo(drone.getCameraCapture());
        }
    }

}
