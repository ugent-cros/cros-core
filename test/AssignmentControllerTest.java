import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.routes;
import models.Assignment;
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
 * Created by Benjamin on 5/03/2015.
 */
public class AssignmentControllerTest extends TestSuperclass {
    private static List<Assignment> testAssignments = new ArrayList<>();

    @BeforeClass
    public static void setup() {
        startFakeApplication();
        initialiseDatabase();
    }

    private static void initialiseDatabase() {
        // Create some checkpoints and add them to a list
        Checkpoint chckpnt1 = new Checkpoint(2,1,3);
        Checkpoint chckpnt2 = new Checkpoint(4,2,6);
        Checkpoint chckpnt3 = new Checkpoint(6,3,9);
        List<Checkpoint> list1 = new ArrayList<>();
        List<Checkpoint> list2 = new ArrayList<>();
        list1.add(chckpnt1);
        list2.add(chckpnt2);
        list2.add(chckpnt3);
        // Add assignments to the database
        testAssignments.add(new Assignment(list1, getUser()));
        testAssignments.add(new Assignment(list2, getUser()));
        Ebean.save(testAssignments);
    }

    @AfterClass
    public static void teardown() {
        stopFakeApplication();
    }

    @Test
    public void getAll_AuthorizedRequest_SuccessfullyGetAllAssignments() {
        Result result = callAction(routes.ref.AssignmentController.getAll(),
                authorizeRequest(new FakeRequest(), getAdmin()));

        String jsonString = contentAsString(result);
        try {
            JsonNode node = JsonHelper.removeRootElement(jsonString, Assignment.class, true);
            if (node.isArray()) {
                for (int i = 0; i < testAssignments.size(); ++i) {
                    Assignment testAssignment = testAssignments.get(i);
                    Assignment receivedAssignment = Json.fromJson(node.get(i), Assignment.class);
                    assertThat(testAssignment.getId()).isEqualTo(receivedAssignment.getId());
                }
            } else
                Assert.fail("Returned JSON is not an array");
        } catch(JsonHelper.InvalidJSONException ex) {
            Assert.fail("Invalid json exception: " + ex.getMessage());
        }
    }

    @Test
    public void get_AuthorizedRequestWithValidId_SuccessfullyGetAssignment() {
        int index = testAssignments.size()-1;
        Assignment testAssignment = testAssignments.get(index);
        Result result = callAction(routes.ref.AssignmentController.get(testAssignment.getId()),
                authorizeRequest(fakeRequest(), getAdmin()));

        String jsonString = contentAsString(result);
        try {
            JsonNode node = JsonHelper.removeRootElement(jsonString, Assignment.class, false);
            Assignment receivedAssignment = Json.fromJson(node, Assignment.class);
            assertThat(testAssignment).isEqualTo(receivedAssignment);
        } catch(JsonHelper.InvalidJSONException ex) {
            Assert.fail("Invalid json exception: " + ex.getMessage());
        }
    }

    @Test
    public void get_AuthorizedRequestWithInvalidId_NotFoundReturned() {
        Result result = callAction(routes.ref.AssignmentController.get(testAssignments.size() + 1000),
                authorizeRequest(fakeRequest(), getAdmin()));
        assertThat(status(result)).isEqualTo(Http.Status.NOT_FOUND);
    }

    @Test
    public void create_AuthorizedRequest_AssignmentCreated() {
        List<Checkpoint> route = new ArrayList<>();
        route.add(new Checkpoint(6,5,7));
        Assignment assignmentToBeAdded = new Assignment(route, null);
        JsonNode node = JsonHelper.createJsonNode(assignmentToBeAdded, Assignment.class);

        Result result = callAction(routes.ref.AssignmentController.create(),
                authorizeRequest(fakeRequest().withJsonBody(node), getAdmin()));

        JsonNode receivedNode;
        try {
            receivedNode = JsonHelper.removeRootElement(contentAsString(result), Assignment.class, false);
            Assignment assignment = Json.fromJson(receivedNode, Assignment.class);

            // bypass id, creator check and checkpoint id check
            assignmentToBeAdded.setId(assignment.getId());
            assignmentToBeAdded.setCreator(assignment.getCreator());
            assignmentToBeAdded.getRoute().get(0).setId(assignment.getRoute().get(0).getId());
            assertThat(assignment).isEqualTo(assignmentToBeAdded);

            Assignment fetchedAssignment = Assignment.FIND.byId(assignment.getId());
            assertThat(fetchedAssignment).isEqualTo(assignmentToBeAdded);
        } catch(JsonHelper.InvalidJSONException ex) {
            Assert.fail("Invalid json exception: " + ex.getMessage());
        }
    }

    @Test
    public void delete_AuthorizedRequestWithValidId_AssignmentDeleted() {
        List<Checkpoint> route = new ArrayList<>();
        Checkpoint checkpointToBeRemoved = new Checkpoint(6,5,7);
        route.add(checkpointToBeRemoved);
        Assignment assignmentToBeRemoved = new Assignment(route, null);
        assignmentToBeRemoved.save();

        callAction(routes.ref.AssignmentController.delete(assignmentToBeRemoved.getId()),
                authorizeRequest(fakeRequest(), getAdmin()));

        // Test if basestation is deleted
        long amount = Assignment.FIND.all().stream().filter(assignment ->
                assignment.getId().equals(assignmentToBeRemoved.getId())).count();
        assertThat(amount).isEqualTo(0);
        // Test if associated checkpoint is deleted
        // TODO: uncomment this if both Checkpoint and BaseStation inherit from Location
        /*amount = Checkpoint.FIND.all().stream().filter(checkpoint ->
                checkpoint.getId().equals(checkpointToBeRemoved.getId())).count();
        assertThat(amount).isEqualTo(0);*/
    }

    @Test
    public void total_AssignmentsInDatabase_TotalIsCorrect() {
        int correctTotal = Assignment.FIND.all().size();
        Result r = callAction(routes.ref.AssignmentController.getTotal(), authorizeRequest(fakeRequest(), getAdmin()));
        try {
            JsonNode responseNode = JsonHelper.removeRootElement(contentAsString(r), Assignment.class, false);
            assertThat(correctTotal).isEqualTo(responseNode.get("total").asInt());
        } catch (JsonHelper.InvalidJSONException e) {
            e.printStackTrace();
            Assert.fail("Invalid json exception" + e.getMessage());
        }
    }
}
