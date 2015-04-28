import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import models.Basestation;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.FakeRequest;
import utilities.JsonHelper;
import controllers.*;

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
        testBasestations.add(new Basestation("Bar 1", 88.0,88.0,88.0));
        testBasestations.add(new Basestation("Bar 2", 77.0,77.0,77.0));
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
        try {
            JsonNode node = JsonHelper.removeRootElement(jsonString, Basestation.class, true);
            if (node.isArray()) {
                for (int i = 0; i < testBasestations.size(); ++i) {
                    Basestation testBasestation = testBasestations.get(i);
                    Basestation receivedBasestation = Json.fromJson(node.get(i), Basestation.class);
                    assertThat(testBasestation.getId()).isEqualTo(receivedBasestation.getId());
                }
            } else
                Assert.fail("Returned JSON is not an array");
        } catch(JsonHelper.InvalidJSONException ex) {
            Assert.fail("Invalid json exception: " + ex.getMessage());
        }
    }

    @Test
    public void get_AuthorizedRequestWithValidId_SuccessfullyGetBasestation(){
        int index = testBasestations.size()-1;
        Basestation testBasestation = testBasestations.get(index);
        Result result = callAction(routes.ref.BasestationController.get(testBasestation.getId()),
                authorizeRequest(new FakeRequest(), getAdmin()));

        String jsonString = contentAsString(result);
        try {
            JsonNode node = JsonHelper.removeRootElement(jsonString, Basestation.class, false);
            Basestation receivedBasestation = Json.fromJson(node, Basestation.class);
            assertThat(testBasestation).isEqualTo(receivedBasestation);
        } catch(JsonHelper.InvalidJSONException ex) {
            Assert.fail("Invalid json exception: " + ex.getMessage());
        }
    }

    @Test
    public void get_AuthorizedRequestWithInvalidId_NotFoundReturned() {
        Result result = callAction(routes.ref.BasestationController.get(testBasestations.size() + 1000),
                authorizeRequest(fakeRequest(), getAdmin()));
        assertThat(status(result)).isEqualTo(Http.Status.NOT_FOUND);
    }

    @Test
    public void create_AuthorizedRequest_BasestationCreated() {
        Basestation basestationToBeAdded = new Basestation("A new testing basestation", 23.0, 23.0, 0.0);
        JsonNode nodeWithRoot = JsonHelper.createJsonNode(basestationToBeAdded, Basestation.class);

        Result result = callAction(routes.ref.BasestationController.create(),
                authorizeRequest(fakeRequest().withJsonBody(nodeWithRoot), getAdmin()));

        String jsonString = contentAsString(result);
        try {
            JsonNode node = JsonHelper.removeRootElement(jsonString, Basestation.class, false);
            Basestation receivedBasestation = Json.fromJson(node, Basestation.class);

            // bypass id check because basestationToBeAdded isn't saved
            basestationToBeAdded.setId(receivedBasestation.getId());
            assertThat(basestationToBeAdded).isEqualTo(receivedBasestation);

            Basestation fetchedBasestation = Basestation.FIND.byId(receivedBasestation.getId());
            assertThat(fetchedBasestation).isEqualTo(receivedBasestation);
        } catch(JsonHelper.InvalidJSONException ex) {
            Assert.fail("Invalid json exception: " + ex.getMessage());
        }
    }

    @Test
    public void update_AuthorizedRequestWithValidId_BasestationUpdated() {
        Basestation basestation = new Basestation("Another basestation", 3.0, 2.0, 1.0);
        basestation.save();
        basestation.setName("Changed name");
        JsonNode nodeWithRoot = JsonHelper.createJsonNode(basestation, Basestation.class);
        Result result = callAction(routes.ref.BasestationController.update(basestation.getId()),
                authorizeRequest(fakeRequest().withJsonBody(nodeWithRoot), getAdmin()));

        String jsonString = contentAsString(result);
        try {
            JsonNode node = JsonHelper.removeRootElement(jsonString, Basestation.class, false);
            Basestation receivedBasestation = Json.fromJson(node, Basestation.class);
            assertThat(basestation.getId()).isEqualTo(receivedBasestation.getId());

            Basestation fetchedBasestation = Basestation.FIND.byId(receivedBasestation.getId());
            assertThat(fetchedBasestation).isEqualTo(receivedBasestation);
        } catch(JsonHelper.InvalidJSONException ex) {
            Assert.fail("Invalid json exception: " + ex.getMessage());
        }
    }
    @Test
    public void delete_AuthorizedRequestWithValidId_BasestationDeleted() {
        Basestation basestationToBeRemoved = new Basestation("remove this drone", 7.0,8.0,9.0);
        basestationToBeRemoved.save();

        callAction(routes.ref.BasestationController.delete(basestationToBeRemoved.getId()),
                authorizeRequest(fakeRequest(), getAdmin()));

        long amount = Basestation.FIND.all().stream().filter(d -> d.getId().equals(basestationToBeRemoved.getId())).count();
        assertThat(amount).isEqualTo(0);
    }

    @Test
    public void total_BasestationsInDatabase_TotalIsCorrect() {
        int correctTotal = Basestation.FIND.all().size();
        Result r = callAction(routes.ref.BasestationController.getTotal(), authorizeRequest(fakeRequest(), getAdmin()));
        try {
            JsonNode responseNode = JsonHelper.removeRootElement(contentAsString(r), Basestation.class, false);
            assertThat(correctTotal).isEqualTo(responseNode.get("total").asInt());
        } catch (JsonHelper.InvalidJSONException e) {
            e.printStackTrace();
            Assert.fail("Invalid json exception" + e.getMessage());
        }
    }
}
