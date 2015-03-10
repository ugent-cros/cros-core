import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.routes;
import models.Basestation;
import models.Checkpoint;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.FakeRequest;
import utilities.JsonHelper;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.*;

/**
 * Created by Eveline on 8/03/2015.
 */
public class BasestationTest extends TestSuperclass {

    private static List<Basestation> testBasestations = new ArrayList<>();

    @BeforeClass
    public static void setup() {
        startFakeApplication();
        initialiseDatabase();
    }

    private static void initialiseDatabase() {
        Checkpoint cp1 = new Checkpoint(88.0,88.0,0.0);
        cp1.save();
        Checkpoint cp2 = new Checkpoint(77.0,77.0,0.0);
        cp2.save();
        testBasestations.add(new Basestation("Bar 1", cp1));
        testBasestations.add(new Basestation("Bar 2", cp2));
        Ebean.save(testBasestations);
    }

    @AfterClass
    public static void tearDown() {
        stopFakeApplication();
    }

    @Test
    public void getAll_AuthorizedRequest_SuccessfullyGetAllBasestations() {
        Result result = callAction(routes.ref.BasestationController.getAll(),
                authorizeRequest(new FakeRequest(), getAdmin()));

        String jsonString = contentAsString(result);
        JsonNode node = JsonHelper.removeRootElement(jsonString, Basestation.class);
        if (node.isArray()) {
            for (int i = 0; i < testBasestations.size(); ++i) {
                Basestation testBasestation = testBasestations.get(i);
                Basestation receivedBasestation = Json.fromJson(node.get(i), Basestation.class);
                assertThat(testBasestation.id).isEqualTo(receivedBasestation.id);
            }
        } else
            Assert.fail("Returned JSON is not an array");
    }

    @Test
    public void get_AuthorizedRequestWithValidId_SuccessfullyGetBasestation(){
        int index = testBasestations.size()-1;
        Basestation testBasestation = testBasestations.get(index);
        Result result = callAction(routes.ref.BasestationController.get(testBasestation.id),
                authorizeRequest(new FakeRequest(), getAdmin()));

        String jsonString = contentAsString(result);
        JsonNode node = JsonHelper.removeRootElement(jsonString, Basestation.class);
        Basestation receivedBasestation = Json.fromJson(node, Basestation.class);
        assertThat(testBasestation).isEqualTo(receivedBasestation);
    }

    @Test
    public void get_AuthorizedRequestWithInvalidId_NotFoundReturned() {
        Result result = callAction(routes.ref.BasestationController.get(testBasestations.size() + 1000),
                authorizeRequest(fakeRequest(), getAdmin()));
        assertThat(status(result)).isEqualTo(Http.Status.NOT_FOUND);
    }

    @Test
    public void create_AuthorizedRequest_BasestationCreated() {
        Basestation basestationToBeAdded = new Basestation("A new testing basestation", new Checkpoint(23, 23, 0));
        JsonNode nodeWithRoot = JsonHelper.addRootElement(Json.toJson(basestationToBeAdded), Basestation.class);

        Result result = callAction(routes.ref.BasestationController.create(),
                authorizeRequest(fakeRequest().withJsonBody(nodeWithRoot), getAdmin()));

        String jsonString = contentAsString(result);
        JsonNode node = JsonHelper.removeRootElement(jsonString, Basestation.class);
        Basestation receivedBasestation = Json.fromJson(node, Basestation.class);

        // bypass id check because basestationToBeAdded isn't saved
        basestationToBeAdded.id = receivedBasestation.id;
        basestationToBeAdded.checkpoint.id = receivedBasestation.checkpoint.id;
        assertThat(basestationToBeAdded).isEqualTo(receivedBasestation);

        Basestation fetchedBasestation = Basestation.find.byId(receivedBasestation.id);
        assertThat(fetchedBasestation).isEqualTo(receivedBasestation);
    }

    @Test
    public void update_AuthorizedRequestWithValidId_BasestationUpdated() {
        Basestation basestation = new Basestation("Another basestation", new Checkpoint(3, 2, 1));
        basestation.save();
        basestation.name = "Changed name";
        JsonNode nodeWithRoot = JsonHelper.addRootElement(Json.toJson(basestation), Basestation.class);
        Result result = callAction(routes.ref.BasestationController.update(basestation.id),
                authorizeRequest(fakeRequest().withJsonBody(nodeWithRoot), getAdmin()));

        String jsonString = contentAsString(result);
        JsonNode node = JsonHelper.removeRootElement(jsonString, Basestation.class);
        Basestation receivedBasestation = Json.fromJson(node, Basestation.class);
        assertThat(basestation.id).isEqualTo(receivedBasestation.id);
        assertThat(basestation.checkpoint).isEqualTo(receivedBasestation.checkpoint);

        Basestation fetchedBasestation = Basestation.find.byId(receivedBasestation.id);
        assertThat(fetchedBasestation).isEqualTo(receivedBasestation);
    }
    @Test
    public void delete_AuthorizedRequestWithValidId_BasestationDeleted() {
        Basestation basestationToBeRemoved = new Basestation("remove this drone", new Checkpoint(7,8,9));
        basestationToBeRemoved.save();

        callAction(routes.ref.BasestationController.delete(basestationToBeRemoved.id),
                authorizeRequest(fakeRequest(), getAdmin()));

        long amount = Basestation.find.all().stream().filter(d -> d.id.equals(basestationToBeRemoved.id)).count();
        assertThat(amount).isEqualTo(0);
    }
}
