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
import java.util.List;

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

}
