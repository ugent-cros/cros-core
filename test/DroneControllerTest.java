import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.routes;
import models.Drone;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;

import java.io.IOException;
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
        String jsonString = contentAsString(result);
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(jsonString).get("drone");
            if(node.isArray()) {
                for(int i = 0; i < testDrones.size(); ++i) {
                    assertThat(node.get(i).get("id").asLong()).isEqualTo(testDrones.get(i).id);
                    assertThat(node.get(i).get("name").asText()).isEqualTo(testDrones.get(i).name);
                }
            }
            else
                Assert.fail("Returned JSON is not an array");
        } catch (IOException e) {
            Assert.fail("Cast failed: invalid JSON string\nError message: " + e);
        }
    }

    @Test
    public void getDrone_DatabaseFilledWithDrones_SuccessfullyGetDrone() {
        Result result = callAction(routes.ref.DroneController.get(testDrones.size()-1), authorizeRequest(fakeRequest(), getAdmin()));
        String jsonString = contentAsString(result);
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(jsonString).get("drone");
            assertThat(node.get("id").asLong()).isEqualTo(testDrones.size()-1);
        } catch (IOException e) {
            Assert.fail("Cast failed: invalid JSON string\nError message: " + e);
        }
    }

    @Test
    public void getDrone_DatabaseFilledWithDrones_DroneIdNotAvailable() {
        Result result = callAction(routes.ref.DroneController.get(testDrones.size()+1000), authorizeRequest(fakeRequest(), getAdmin()));
        assertThat(status(result)).isEqualTo(Http.Status.NOT_FOUND);
    }

    @Test
    public void addDrone_DatabaseFilledWithDrones_ReturnedDroneIsCorrect() {
        Drone droneToBeAdded = new Drone("newDrone", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT,"ipAddress");
        System.out.println(Json.toJson(droneToBeAdded).asText());
        JsonNode node = Json.toJson(droneToBeAdded).get("drone");

        Result result = callAction(routes.ref.DroneController.add(), authorizeRequest(fakeRequest().withJsonBody(node), getAdmin()));
        try {
            Drone d = new ObjectMapper().readValue(contentAsString(result), Drone.class);
            System.out.println(d);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(Json.toJson(contentAsString(result)));
        Drone receivedDrone = Json.fromJson(Json.toJson(contentAsString(result)), Drone.class);

        assertThat(droneToBeAdded.id).equals(receivedDrone.id);
        assertThat(droneToBeAdded.address).equals(receivedDrone.address);
        assertThat(droneToBeAdded.communicationType).equals(receivedDrone.communicationType);
        assertThat(droneToBeAdded.name).equals(receivedDrone.name);

        Drone fetchedDrone = Drone.find.where().eq("name", droneToBeAdded.name).findUnique();

        assertThat(droneToBeAdded.id).equals(fetchedDrone.id);
        assertThat(droneToBeAdded.address).equals(fetchedDrone.address);
        assertThat(droneToBeAdded.communicationType).equals(fetchedDrone.communicationType);
        assertThat(droneToBeAdded.name).equals(fetchedDrone.name);
    }

    @Test
    public void updateDrone_DatabaseFilledWithDrones_UpdatesAreCorrect() {
        Drone d = new Drone("test1", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT,"address1");
        d.save();
        d.name = "test2";
        d.name = "address2";
        JsonNode node = Json.toJson(d).get("drone");
        Result result = callAction(routes.ref.DroneController.update(d.id), authorizeRequest(fakeRequest().withJsonBody(node), getAdmin()));

        Drone receivedDrone = Json.fromJson(Json.toJson(contentAsString(result)), Drone.class);
        assertThat(d.id).equals(receivedDrone.id);
        assertThat(d.address).equals(receivedDrone.address);
        assertThat(d.communicationType).equals(receivedDrone.communicationType);
        assertThat(d.name).equals(receivedDrone.name);

        Drone fetchedDrone = Drone.find.byId(d.id);

        assertThat(d.id).equals(fetchedDrone.id);
        assertThat(d.address).equals(fetchedDrone.address);
        assertThat(d.communicationType).equals(fetchedDrone.communicationType);
        assertThat(d.name).equals(fetchedDrone.name);
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
        ObjectMapper mapper = new ObjectMapper();
        for(Drone drone : testDrones) {
            Result r = callAction(routes.ref.DroneController.testConnection(drone.id), authorizeRequest(fakeRequest(), getAdmin()));
            try {
                boolean status = mapper.readTree(contentAsString(r)).get("Boolean").asBoolean();
                assertThat(status).isEqualTo(drone.testConnection());
            } catch (IOException e) {
                Assert.fail("Cast failed: invalid JSON string\nError message: " + e);
            }
        }
    }

    @Test
    public void battery_DatabaseFilledWithDrones_CorrectBatteryStatusReceived() {
        ObjectMapper mapper = new ObjectMapper();
        for(Drone drone : testDrones) {
            Result r = callAction(routes.ref.DroneController.battery(drone.id), authorizeRequest(fakeRequest(), getAdmin()));
            try {
                int status = mapper.readTree(contentAsString(r)).get("Integer").intValue();
                assertThat(status).isEqualTo(drone.getBatteryStatus());
            } catch (IOException e) {
                Assert.fail("Cast failed: invalid JSON string\nError message: " + e);
            }
        }
    }

    @Test
    public void cameraCapture_DatabaseFilledWithDrones_CorrectCaptureReceived() {
        for(Drone drone : testDrones) {
            Result r = callAction(routes.ref.DroneController.cameraCapture(drone.id), authorizeRequest(fakeRequest(), getAdmin()));
            ObjectMapper mapper = new ObjectMapper();
            try {
                String capture = mapper.readTree(contentAsString(r)).get("String").asText();
                assertThat(capture).isEqualTo(drone.getCameraCapture());
            } catch (IOException e) {
                Assert.fail("Cast failed: invalid JSON string\nError message: " + e);
            }
        }
    }

}
