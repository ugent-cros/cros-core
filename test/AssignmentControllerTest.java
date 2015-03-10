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
        Checkpoint chckpnt1 = new Checkpoint(1,2,3);
        Checkpoint chckpnt2 = new Checkpoint(2,4,6);
        Checkpoint chckpnt3 = new Checkpoint(3,6,9);
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
        JsonNode node = JsonHelper.removeRootElement(jsonString, Assignment.class);
        if (node.isArray()) {
            for (int i = 0; i < testAssignments.size(); ++i) {
                Assignment testAssignment = testAssignments.get(i);
                Assignment receivedAssignment = Json.fromJson(node.get(i), Assignment.class);
                assertThat(testAssignment.id).isEqualTo(receivedAssignment.id);
            }
        } else
            Assert.fail("Returned JSON is not an array");
    }

    @Test
    public void get_AuthorizedRequestWithValidId_SuccessfullyGetAssignment() {
        int index = testAssignments.size()-1;
        Assignment testAssignment = testAssignments.get(index);
        Result result = callAction(routes.ref.AssignmentController.get(testAssignment.id),
                authorizeRequest(fakeRequest(), getAdmin()));

        String jsonString = contentAsString(result);
        JsonNode node = JsonHelper.removeRootElement(jsonString, Assignment.class);
        Assignment receivedAssignment = Json.fromJson(node, Assignment.class);
        assertThat(testAssignment).isEqualTo(receivedAssignment);
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
        route.add(new Checkpoint(5,6,7));
        Assignment assignmentToBeAdded = new Assignment(route, null);
        JsonNode node = JsonHelper.addRootElement(Json.toJson(assignmentToBeAdded), Assignment.class);

        Result result = callAction(routes.ref.AssignmentController.create(),
                authorizeRequest(fakeRequest().withJsonBody(node), getAdmin()));
        JsonNode receivedNode = JsonHelper.removeRootElement(contentAsString(result), Assignment.class);

        Assignment assignment = Json.fromJson(receivedNode, Assignment.class);
        // bypass id, creator check and checkpoint id check
        assignmentToBeAdded.id = assignment.id;
        assignmentToBeAdded.creator = assignment.creator;
        assignmentToBeAdded.route.get(0).id = assignment.route.get(0).id;
        assertThat(assignment).isEqualTo(assignmentToBeAdded);

        Assignment fetchedAssignment = Assignment.find.byId(assignment.id);
        assertThat(fetchedAssignment).isEqualTo(assignmentToBeAdded);
    }

    @Test
    public void delete_AuthorizedRequestWithValidId_AssignmentDeleted() {
        List<Checkpoint> route = new ArrayList<>();
        route.add(new Checkpoint(5,6,7));
        Assignment assignmentToBeRemoved = new Assignment(route, null);
        assignmentToBeRemoved.save();

        callAction(routes.ref.AssignmentController.delete(assignmentToBeRemoved.id),
                authorizeRequest(fakeRequest(), getAdmin()));

        long amount = Assignment.find.all().stream().filter(assignment ->
                assignment.id.equals(assignmentToBeRemoved.id)).count();
        assertThat(amount).isEqualTo(0);
    }
}
