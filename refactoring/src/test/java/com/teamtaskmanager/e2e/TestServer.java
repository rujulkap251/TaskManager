package com.teamtaskmanager.e2e;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.teamtaskmanager.Server;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 * Starts the server and makes a few basic tests on the API
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Server.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class TestServer {

    @Value("${local.server.port}")
    private int serverPort;

    /**
     * Clear down the database before every test
     *
     * @throws UnirestException
     */
    @Before
    public void setup() throws UnirestException {

        assertEquals(
                "Couldn't purge tasks",
                HttpStatus.SC_OK,
                Unirest.delete("http://localhost:" + serverPort + "/clear").asString().getStatus()
        );
    }

    /**
     * Confirm that when we submit a task json that a task is created  available using the location URL response
     *
     * @throws UnirestException
     */
    @Test
    public void createTask() throws UnirestException {
        String user = "Arch Stanton";
        String task = "do it to it";
        String date = "2525-08-26";

        HttpResponse<String> result = givenTaskCreated(user, task, date);
        assertEquals("Create failed", HttpStatus.SC_OK, result.getStatus());

        String pathToNewTask = result.getHeaders().getFirst("Location");
        HttpResponse<JsonNode> getResponse = Unirest.get(pathToNewTask).asJson();
        assertEquals("URL didn't work", HttpStatus.SC_OK, getResponse.getStatus());

        thenTaskJsonHasValues(getResponse.getBody().getObject(), user, task, date, false);

    }

    @Test
    public void getTaskThatIsNotThere() throws UnirestException {
        assertEquals(
                "Task does not exist, API should say so",
                HttpStatus.SC_NOT_FOUND,
                Unirest.get("http://localhost:" + serverPort + "/tasks/{id}")
                        .routeParam("id", "-12345")
                        .asJson()
                        .getStatus()
        );
    }

    /**
     * Confirm that the complete endpoint marks the task as completed
     *
     * @throws UnirestException
     */
    @Test
    public void completeTask() throws UnirestException {

        String user = "Arch Stanton";
        String task = "do it to it";
        String date = "2525-08-26";

        HttpResponse<String> result = givenTaskCreated(user, task, date);
        assertEquals("Create failed", HttpStatus.SC_OK, result.getStatus());

        HttpResponse<String> completion = givenTaskCompleted(id(result));
        assertEquals("Completion failed", HttpStatus.SC_OK, completion.getStatus());

        thenTaskExists(user, task, date, id(result), true);

    }

    @Test
    public void completeTaskThatIsNotThere() throws UnirestException {
        assertEquals(
                "Task does not exist, API should say so",
                HttpStatus.SC_NOT_FOUND,
                givenTaskCompleted("-12345")
                        .getStatus()
        );
    }

    /**
     * Confirm that the complete endpoint marks the task as completed
     *
     * @throws UnirestException
     */
    @Test
    public void deleteTask() throws UnirestException {

        String user = "Arch Stanton";
        String task = "do it to it";
        String date = "2525-08-26";

        HttpResponse<String> result = givenTaskCreated(user, task, date);
        assertEquals("Create failed", HttpStatus.SC_OK, result.getStatus());
        String id = id(result);

        assertEquals("Delete failed",
                HttpStatus.SC_OK,
                Unirest.delete("http://localhost:" + serverPort + "/tasks/{id}").routeParam("id", id).asString().getStatus()
        );

        assertEquals("Delete should fail on a second call",
                HttpStatus.SC_NOT_FOUND,
                Unirest.delete("http://localhost:" + serverPort + "/tasks/{id}").routeParam("id", id).asString().getStatus()
        );

        assertEquals("Get should have failed",
                HttpStatus.SC_NOT_FOUND,
                Unirest.get("http://localhost:" + serverPort + "/tasks/{id}").routeParam("id", id).asString().getStatus()
        );
    }

    /**
     * Test that when a task is created in the past that the request is rejected
     *
     * @throws UnirestException
     */
    @Test
    public void createTaskInThePast() throws UnirestException {

        String user = "Arch Stanton";
        String task = "do it to it";
        String date = "1976-08-26";

        HttpResponse<String> result = givenTaskCreated(user, task, date);
        assertEquals("Create should have failed", HttpStatus.SC_BAD_REQUEST, result.getStatus());
        assertEquals("Wrong error", "Due can't be in the past", result.getHeaders().getFirst("Error"));

    }

    /**
     * Test that when a task is created with no user that the request is rejected
     *
     * @throws UnirestException
     */
    @Test
    public void createTaskWithNoUser() throws UnirestException {

        String user = null;
        String task = "do it to it";
        String date = "7510-08-26";

        HttpResponse<String> result = givenTaskCreated(user, task, date);
        assertEquals("Create should have failed", HttpStatus.SC_BAD_REQUEST, result.getStatus());
        assertEquals("Wrong error", "User can't be empty", result.getHeaders().getFirst("Error"));

    }

    /**
     * Test that when a task is created with no due date that the request is rejected
     *
     * @throws UnirestException
     */
    @Test
    public void createTaskWithNoDue() throws UnirestException {

        String user = "Arch Stanton";
        String task = "do it to it";
        String date = null;

        HttpResponse<String> result = givenTaskCreated(user, task, date);
        assertEquals("Create should have failed", HttpStatus.SC_BAD_REQUEST, result.getStatus());
        assertEquals("Wrong error", "Due can't be empty", result.getHeaders().getFirst("Error"));

    }

    /**
     * Test that when a task is created with no task that the request is rejected
     *
     * @throws UnirestException
     */
    @Test
    public void createTaskWithNoTask() throws UnirestException {

        String user = "Arch Stanton";
        String task = null;
        String date = "7510-08-26";

        HttpResponse<String> result = givenTaskCreated(user, task, date);
        assertEquals("Create should have failed", HttpStatus.SC_BAD_REQUEST, result.getStatus());
        assertEquals("Wrong error", "Task can't be empty", result.getHeaders().getFirst("Error"));

    }

    /**
     * Check that we can filter tasks by name, and dateAfter
     *
     * @throws UnirestException
     */
    @Test
    public void filterByName() throws UnirestException {

        String A = id(givenTaskCreated("Arch", "t1", "2525-08-26"));
        String B = id(givenTaskCreated("Bob", "t1", "2525-08-26"));
        String C = id(givenTaskCreated("Cynthia", "t2", "2525-08-26"));
        String D = id(givenTaskCreated("Arch", "t3", "2525-09-27"));
        String E = id(givenTaskCreated("Elsie", "t4", "2525-08-26"));

        // this last task is marked completed and shouldn't be show by default
        String id = id(givenTaskCreated("Arch", "t3", "2525-10-27"));
        givenTaskCompleted(id);

        HttpResponse<JsonNode> response = Unirest.get("http://localhost:" + serverPort + "/tasks")
                .queryString("user", "Arch")
                .queryString("dateAfter", "2525-09-01")
                .asJson();

        assertEquals("Query failed", HttpStatus.SC_OK, response.getStatus());
        assertEquals("Wrong results", asList(D), getResultIds(response));

    }

    private List<String> getResultIds(HttpResponse<JsonNode> response) {
        List<String> observedIds = new ArrayList<>();
        for (int i = 0; i != response.getBody().getArray().length(); i++) {
            observedIds.add(String.valueOf(response.getBody().getArray().getJSONObject(i).getLong("id")));
        }
        return observedIds;
    }

    @Test
    public void includeCompleted() throws UnirestException {

        String A = id(givenTaskCreated("Arch", "t1", "2525-08-26"));
        String B = id(givenTaskCreated("Bob", "t1", "2525-08-26"));
        String C = id(givenTaskCreated("Cynthia", "t2", "2525-08-26"));
        String D = id(givenTaskCreated("Arch", "t3", "2525-09-27"));
        String E = id(givenTaskCreated("Elsie", "t4", "2525-08-26"));

        // this last task is marked completed and shouldn't be shown by default
        // but we will ask for it!
        String F = id(givenTaskCreated("Arch", "t5", "2525-10-27"));
        givenTaskCompleted(F);

        HttpResponse<JsonNode> response = Unirest.get("http://localhost:" + serverPort + "/tasks")
                .queryString("user", "Arch")
                .queryString("dateAfter", "2525-09-01")
                .queryString("includeCompleted", Boolean.TRUE)
                .asJson();

        assertEquals("Query failed", HttpStatus.SC_OK, response.getStatus());
        assertEquals("Wrong results", asList(D, F), getResultIds(response));

    }

    /**
     * Test that we can sort by dates descending
     *
     * @throws UnirestException
     */
    @Test
    public void defaultSortsById() throws UnirestException {

        String A = id(givenTaskCreated("Arch", "t1", "2525-08-26"));
        String B = id(givenTaskCreated("Bob", "t1", "2525-08-26"));
        String C = id(givenTaskCreated("Cynthia", "t2", "2525-08-26"));
        String D = id(givenTaskCreated("Arch", "t3", "2525-09-27"));
        String E = id(givenTaskCreated("Elsie", "t4", "2525-08-26"));

        HttpResponse<JsonNode> response = Unirest.get("http://localhost:" + serverPort + "/tasks")
                .asJson();

        assertEquals("Query failed", HttpStatus.SC_OK, response.getStatus());
        assertEquals("Wrong results", asList(A, B, C, D, E), getResultIds(response));

    }

    /**
     * Test that we can sort by dates descending
     *
     * @throws UnirestException
     */
    @Test
    public void sortsById() throws UnirestException {

        String A = id(givenTaskCreated("Arch", "t1", "2525-08-26"));
        String B = id(givenTaskCreated("Bob", "t1", "2525-08-26"));
        String C = id(givenTaskCreated("Cynthia", "t2", "2525-08-26"));
        String D = id(givenTaskCreated("Arch", "t3", "2525-09-27"));
        String E = id(givenTaskCreated("Elsie", "t4", "2525-08-26"));

        HttpResponse<JsonNode> response = Unirest.get("http://localhost:" + serverPort + "/tasks")
                .queryString("sortBy", "id:desc")
                .asJson();

        assertEquals("Query failed", HttpStatus.SC_OK, response.getStatus());
        assertEquals("Wrong results", asList(E, D, C, B, A), getResultIds(response));

    }

    /**
     * Test that we can sort by dates descending
     *
     * @throws UnirestException
     */
    @Test
    public void sortByDate() throws UnirestException {

        String A = id(givenTaskCreated("Arch", "t1", "2525-08-24"));
        String B = id(givenTaskCreated("Bob", "t1", "2525-08-25"));
        String C = id(givenTaskCreated("Cynthia", "t2", "2525-08-24"));
        String D = id(givenTaskCreated("Arch", "t3", "2525-09-27"));
        String E = id(givenTaskCreated("Elsie", "t4", "2525-08-26"));

        HttpResponse<JsonNode> response = Unirest.get("http://localhost:" + serverPort + "/tasks")
                .queryString("sortBy", "due:desc")
                .asJson();

        assertEquals("Query failed", HttpStatus.SC_OK, response.getStatus());
        assertEquals("Wrong results", asList(D, E, B, A, C), getResultIds(response));

    }

    private String id(HttpResponse<String> givenTaskCreated) {
        String location = givenTaskCreated.getHeaders().getFirst("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    @Test
    public void sortByUser() throws UnirestException {

        String A = id(givenTaskCreated("Arch", "t1", "2525-08-26"));
        String B = id(givenTaskCreated("Bob", "t1", "2525-08-26"));
        String C = id(givenTaskCreated("Cynthia", "t2", "2525-08-26"));
        String D = id(givenTaskCreated("Arch", "t3", "2525-09-27"));
        String E = id(givenTaskCreated("Elsie", "t4", "2525-08-26"));

        HttpResponse<JsonNode> response = Unirest.get("http://localhost:" + serverPort + "/tasks")
                .queryString("sortBy", "user")
                .asJson();

        assertEquals("Query failed", HttpStatus.SC_OK, response.getStatus());
        assertEquals("Wrong results", asList(A, D, B, C, E), getResultIds(response));

    }

    @Test
    public void sortByTask() throws UnirestException {

        String A = id(givenTaskCreated("Arch", "t1", "2525-08-26"));
        String B = id(givenTaskCreated("Bob", "t2", "2525-08-26"));
        String C = id(givenTaskCreated("Cynthia", "t1", "2525-08-26"));
        String D = id(givenTaskCreated("Arch", "t5", "2525-09-27"));
        String E = id(givenTaskCreated("Elsie", "t4", "2525-08-26"));

        HttpResponse<JsonNode> response = Unirest.get("http://localhost:" + serverPort + "/tasks")
                .queryString("sortBy", "task")
                .asJson();

        assertEquals("Query failed", HttpStatus.SC_OK, response.getStatus());
        assertEquals("Wrong results", asList(A, C, B, E, D), getResultIds(response));

    }

    /**
     * Confirm that users are not duplicated when we create new tasks with the same user
     *
     * @throws UnirestException
     */
    @Test
    public void uniqueUsersForTasks() throws UnirestException {

        givenTaskCreated("Arch", "t1", "2525-08-26");
        givenTaskCreated("Bob", "t1", "2525-08-26");
        givenTaskCreated("Cynthia", "t2", "2525-08-26");
        givenTaskCreated("Arch", "t3", "2525-09-27");
        givenTaskCreated("Elsie", "t4", "2525-08-26");

        HttpResponse<JsonNode> response = Unirest.get("http://localhost:" + serverPort + "/users").asJson();
        assertEquals("User query failed", HttpStatus.SC_OK, response.getStatus());

        Set<String> observedNames = new TreeSet<>();
        JSONArray array = response.getBody().getArray();
        for (int i = 0; i != array.length(); i++) {
            observedNames.add(array.getJSONObject(i).getString("name"));
        }

        assertEquals("Names are not unique", array.length(), observedNames.size());
        assertEquals("Names are not as expected", new TreeSet<>(asList("Arch", "Bob", "Cynthia", "Elsie")), observedNames);

    }

    private HttpResponse<String> givenTaskCompleted(String id) throws UnirestException {
        return Unirest.put("http://localhost:" + serverPort + "/tasks/{id}/complete")
                .routeParam("id", id)
                .asString();
    }

    private void thenTaskExists(String user, String task, String date, String id) throws UnirestException {
        thenTaskExists(user, task, date, id, false);
    }

    private void thenTaskExists(String user, String task, String date, String id, boolean isCompleted) throws UnirestException {
        HttpResponse<JsonNode> recheck = Unirest.get("http://localhost:" + serverPort + "/tasks/{id}")
                .routeParam("id", id)
                .asJson();

        assertEquals("Recheck failed", HttpStatus.SC_OK, recheck.getStatus());

        JSONObject checkedObject = recheck.getBody().getObject();

        thenTaskJsonHasValues(checkedObject, user, task, date, isCompleted);
    }

    private void thenTaskJsonHasValues(JSONObject checkedObject, String user, String task, String date) {
        thenTaskJsonHasValues(checkedObject, user, task, date, false);
    }

    private void thenTaskJsonHasValues(JSONObject checkedObject, String user, String task, String date, boolean isCompleted) {
        assertEquals("Wrong name", user, checkedObject.getString("user"));
        assertEquals("Wrong due date", date, checkedObject.getString("due"));
        assertEquals("Wrong task", task, checkedObject.getString("task"));
        assertEquals("Wrong isCompleted", isCompleted, checkedObject.getBoolean("isCompleted"));
    }

    private HttpResponse<String> givenTaskCreated(String user, String task, String date) throws UnirestException {
        HttpResponse<String> result = Unirest.post("http://localhost:" + serverPort + "/tasks")
                .body(task(user, task, date)).asObject(String.class);

        return result;
    }

    private String task(String user, String task, String date) {
        return new JSONObject()
                .put("user", user)
                .put("task", task)
                .put("due", date)
                .toString();
    }

}


