import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.routes;
import models.Basestation;
import models.Checkpoint;
import models.Drone;
import org.junit.*;
import play.libs.Json;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.WithApplication;

import javax.validation.constraints.AssertTrue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.CREATED;
import static play.test.Helpers.*;
import static play.test.Helpers.callAction;

/**
 * Created by Eveline on 8/03/2015.
 */
public class BasestationTest extends TestSuperclass {

    private static List<Basestation> testObjects = new ArrayList<>();

    @BeforeClass
    public static void setup() {
        startFakeApplication();
        initializeDatabase();
    }

    private static void initializeDatabase(){
        testObjects.add(new Basestation("Bar 1", new Checkpoint(88.0,88.0,0.0)));
        testObjects.add(new Basestation("Bar 2", new Checkpoint(77.0,77.0,0.0)));
        Ebean.save(testObjects);
    }

    @AfterClass
    public static void tearDown() {
        stopFakeApplication();
    }

    @Test
    public void addBasestation_byAdmin_AddsBasestationToDB(){
        Map<String, String> data = new HashMap<>();
        String name = Bar 3;
        double lattitude = 55.57;
        double longitude = 55.57;
        double altitude = 1.0;
        data.put("name", name);
        data.put("lattitude", Double.toString(lattitude));
        data.put("longitude", Double.toString(longitude));
        data.put("altitude", Double.toString(altitude));

        FakeRequest addRequest = new FakeRequest("POST", "/basestations/").withFormUrlEncodedBody(data);;

        Result result = callAction(controllers.routes.ref.BasestationController.add(), authorizeRequest(addRequest, getAdmin()));
        assertThat(status(result)).isEqualTo(CREATED);

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(contentAsString(result)).get("Basestation");
            long id = node.get("id").asLong();
        } catch (IOException e) {
            Assert.fail("Cast failed: invalid JSON string\nError message: " + e);
        }

        Basestation createdBs = Basestation.find.byId(id);
        assertThat(createdBs).isNotNull();
        assertThat(createdBs.name).isEqualTo(name);
        assertThat(createdBs.checkpoint.lattitude).isEqualTo(lattitude);
        assertThat(createdBs.checkpoint.longitude).isEqualTo(longitude);
        assertThat(createdBs.checkpoint.altitude).isEqualTo(altitude);
        createdBs.delete();
    }

    @Test
    public void addBasestation_byUnauthorizedUser_ReturnsUnauthorized(){
        Map<String, String> data = new HashMap<>();
        String name = Bar 3;
        double lattitude = 55.57;
        double longitude = 55.57;
        double altitude = 1.0;
        data.put("name", name);
        data.put("lattitude", Double.toString(lattitude));
        data.put("longitude", Double.toString(longitude));
        data.put("altitude", Double.toString(altitude));

        FakeRequest addRequest = new FakeRequest("POST", "/basestations/").withFormUrlEncodedBody(data);;

        Result result = callAction(controllers.routes.ref.BasestationController.add(), addRequest);
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(controllers.routes.ref.BasestationController.add(), authorizeRequest(addRequest, getUser()));
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(controllers.routes.ref.BasestationController.add(), authorizeRequest(addRequest, getReadOnlyAdmin()));
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);
    }

    @Test
     public void getBasestation_ByAdmin_returnsObject(){
        FakeRequest getRequest = new FakeRequest("GET", "/basestations/1");
        Result result = callAction(controllers.routes.ref.BasestationController.get((long)1), authorizeRequest(getRequest, getAdmin()));
        String jsonString = contentAsString(result);
        System.out.println(jsonString);

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(jsonString).get("Basestation");
            assertThat(node.get("id").asLong()).isEqualTo(1);
        } catch (IOException e) {
            Assert.fail("Cast failed: invalid JSON string\nError message: " + e);
        }
    }

    @Test
    public void getBasestation_ByReadOnlyAdmin_returnsObject(){
        FakeRequest getRequest = new FakeRequest("GET", "/basestations/1");
        Result result = callAction(controllers.routes.ref.BasestationController.get((long)1), authorizeRequest(getRequest, getReadOnlyAdmin()));
        String jsonString = contentAsString(result);
        System.out.println(jsonString);

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(jsonString).get("Basestation");
            assertThat(node.get("id").asLong()).isEqualTo(1);
        } catch (IOException e) {
            Assert.fail("Cast failed: invalid JSON string\nError message: " + e);
        }
    }

    @Test
    public void getBasestation_UnauthorizedUser_returnsUnauthorized(){
        FakeRequest getRequest = new FakeRequest("GET", "/basestations/1");

        Result result = callAction(controllers.routes.ref.BasestationController.get((long)1), getRequest);
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(controllers.routes.ref.BasestationController.get((long)1), authorizeRequest(getRequest, getUser()));
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

    }

    @Test
    public void getAllBasestations_byAuthorizedUser_ReturnsList(){
        FakeRequest getRequest = new FakeRequest("GET", "/basestations/");
        Result result = callAction(controllers.routes.ref.BasestationController.getAll(), authorizeRequest(getRequest, getAdmin()));
        String jsonString = contentAsString(result);
        System.out.println(jsonString);
        //TODO
        /*ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(jsonString).get("Basestation");
            assertThat(node.get("id").asLong()).isEqualTo(1);
        } catch (IOException e) {
            Assert.fail("Cast failed: invalid JSON string\nError message: " + e);
        }*/
    }

    @Test
    public void updateBasestation_ByAdmin_updatesObject(){
        Basestation ref = Basestation.find.byId((long)1);
        Map<String, String> data = new HashMap<>();
        data.put("name", ref.name + "- edited");
        data.put("checkpoint_id",  Long.toString(ref.checkpoint.id));
        data.put("lattitude", Double.toString(ref.checkpoint.lattitude));
        data.put("longitude", Double.toString(ref.checkpoint.longitude));
        data.put("altitude",  Double.toString(ref.checkpoint.altitude));

        FakeRequest putRequest = new FakeRequest("PUT", "/basestations/1").withFormUrlEncodedBody(data);
        Result result = callAction(controllers.routes.ref.BasestationController.update((long) 1), authorizeRequest(putRequest, getAdmin()));
        assertThat(status(result)).isEqualTo(OK);

        Basestation updated = Basestation.find.byId((long)1);
        assertThat(updated.name).isEqualTo(ref.name + "- edited");
    }

    @Test
    public void updateBasestation_ByUnauthorizedUser_returnsUnauthorized() {
        Basestation ref = Basestation.find.byId((long) 1);
        Map<String, String> data = new HashMap<>();
        data.put("name", ref.name + "- edited");
        data.put("checkpoint_id", Long.toString(ref.checkpoint.id));
        data.put("lattitude", Double.toString(ref.checkpoint.lattitude));
        data.put("longitude", Double.toString(ref.checkpoint.longitude));
        data.put("altitude", Double.toString(ref.checkpoint.altitude));

        FakeRequest putRequest = new FakeRequest("PUT", "/basestations/1").withFormUrlEncodedBody(data);
        Result result = callAction(controllers.routes.ref.BasestationController.update((long) 1), putRequest);
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(controllers.routes.ref.BasestationController.update((long) 1), authorizeRequest(putRequest, getReadOnlyAdmin()));
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(controllers.routes.ref.BasestationController.update((long) 1), authorizeRequest(putRequest, getUser()));
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);
    }

    @Test
    public void deleteBasestation_byAdmin_deletesObject(){
        FakeRequest deleteRequest = new FakeRequest("DELETE", "/basestations/1");
        Result result = callAction(controllers.routes.ref.BasestationController.delete((long) 1), authorizeRequest(deleteRequest, getAdmin()));
        assertThat(status(result)).isEqualTo(OK);

        FakeRequest getRequest = new FakeRequest("GET", "/basestations/1");
        result = callAction(controllers.routes.ref.BasestationController.get((long)1), authorizeRequest(getRequest, getAdmin()));
        assertThat(status(result)).isEqualTo(NOT_FOUND);
    }

    @Test
    public void deleteBasestation_byUnauthorizedUser_returnsUnauthorized(){
        FakeRequest deleteRequest = new FakeRequest("DELETE", "/basestations/1");
        Result result = callAction(controllers.routes.ref.BasestationController.delete((long) 1), deleteRequest);
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(controllers.routes.ref.BasestationController.delete((long) 1), authorizeRequest(deleteRequest, getReadOnlyAdmin()));
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);

        result = callAction(controllers.routes.ref.BasestationController.delete((long) 1), authorizeRequest(deleteRequest, getUser()));
        assertThat(status(result)).isEqualTo(UNAUTHORIZED);
    }


}
