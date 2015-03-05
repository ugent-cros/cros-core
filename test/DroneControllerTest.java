import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import controllers.routes;
import models.Drone;
import org.junit.*;

import play.mvc.Result;
import scala.tools.nsc.typechecker.Analyzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;

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
        Result result = callAction(routes.ref.DroneController.getAll(), authorizedRequest);
        String jsonString = contentAsString(result);
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(jsonString).get("drone");
            if(node.isArray()) {
                for(int i = 0; i < node.size(); ++i) {
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
        Result result = callAction(routes.ref.DroneController.get(testDrones.size()-1), authorizedRequest);
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
        Result result = callAction(routes.ref.DroneController.get(testDrones.size()+1000), authorizedRequest);
        assertThat(contentAsString(result)).isEqualTo("");
    }

    @Test
    public void addDrone_DatabaseFilledWithDrones_ReturnedDroneIsCorrect() {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("name", "super drone");
        parameters.put("address", "192.168.0.15");
        parameters.put("communicationType", "DEFAULT");
        Result result = callAction(routes.ref.DroneController.add(), authorizedRequest.withFormUrlEncodedBody(parameters));
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(contentAsString(result)).get("drone");
            assertThat(node.get("name").asText()).isEqualTo("super drone");
            assertThat(node.get("address").asText()).isEqualTo("192.168.0.15");
            assertThat(node.get("communicationType").asText()).isEqualTo("DEFAULT");

            Result result2 = callAction(routes.ref.DroneController.get(node.get("id").asInt()), authorizedRequest);
            assertThat(contentAsString(result2)).isEqualTo(contentAsString(result));

            // remove drone afterwards
            Drone d = Drone.find.byId(node.get("id").asLong());
            d.delete();
        } catch (IOException e) {
            Assert.fail("Cast failed: invalid JSON string\nError message: " + e);
        }
    }

    @Test
    public void deleteDrone_DatabaseFilledWithDrones_DroneIsDeleted() {
        Drone droneToBeRemoved = new Drone("remove this drone", Drone.Status.AVAILABLE, Drone.CommunicationType.DEFAULT, "x.x.x.x");
        droneToBeRemoved.save();

        callAction(routes.ref.DroneController.delete(droneToBeRemoved.id), authorizedRequest);

        long amount = Drone.find.all().stream().filter(d -> d.id == droneToBeRemoved.id).count();
        assertThat(amount).isEqualTo(0);
    }

    @Test
    public void testConnection_DatabaseFilledWithDrones_CorrectConnectionReceived() {
        ObjectMapper mapper = new ObjectMapper();
        for(Drone drone : testDrones) {
            Result r = callAction(routes.ref.DroneController.testConnection(drone.id), authorizedRequest);
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
            Result r = callAction(routes.ref.DroneController.battery(drone.id), authorizedRequest);
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
            Result r = callAction(routes.ref.DroneController.cameraCapture(drone.id), authorizedRequest);
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
