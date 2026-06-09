package com.kanban;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kanban.service.BoardService;
import com.kanban.service.TaskService;
import com.kanban.service.UserService;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TaskServiceTest extends BaseTest {

    private static TaskService        taskService;
    private static BoardService       boardService;
    private static UserService        userService;
    private static MockClientHandler  userClient;
    private static int                columnId;
    private static int                column2Id;
    private static int                taskId;

    @BeforeAll
    static void setup() throws Exception {
        setupDatabase();
        taskService  = new TaskService();
        boardService = new BoardService();
        userService  = new UserService();

        // Créer un utilisateur
        userService.register(buildUser("Task User", "taskuser@test.com", "password123"));
        userClient = new MockClientHandler(-1);
        userService.login(buildLogin("taskuser@test.com", "password123"), userClient);

        // Créer un board → récupérer les colonnes
        String boardResult = boardService.createBoard(buildBoard("Task Board"), userClient);
        int boardId = extractId(boardResult, "id");

        // Récupérer les tâches pour avoir les columnIds
        JsonObject tasksData = new JsonObject();
        tasksData.addProperty("boardId", boardId);
        String tasksResult = taskService.getTasks(tasksData, userClient);

        // Extraire les 2 premières colonnes
        com.google.gson.JsonArray cols = JsonParser.parseString(tasksResult)
                .getAsJsonObject().getAsJsonArray("data");
        columnId  = cols.get(0).getAsJsonObject().get("id").getAsInt();
        column2Id = cols.get(1).getAsJsonObject().get("id").getAsInt();

        // Mettre currentBoardId sur le client
        userClient.setCurrentBoardId(boardId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private static JsonObject buildUser(String name, String email, String pass) {
        JsonObject j = new JsonObject();
        j.addProperty("fullName",  name);
        j.addProperty("email",     email);
        j.addProperty("password",  pass);
        return j;
    }

    private static JsonObject buildLogin(String email, String pass) {
        JsonObject j = new JsonObject();
        j.addProperty("email",    email);
        j.addProperty("password", pass);
        return j;
    }

    private static JsonObject buildBoard(String title) {
        JsonObject j = new JsonObject();
        j.addProperty("title",       title);
        j.addProperty("description", "desc");
        j.addProperty("themeColor",  "#FFFFFF");
        j.addProperty("visibility",  "PRIVATE");
        return j;
    }

    private JsonObject buildTask(String title, int colId) {
        JsonObject j = new JsonObject();
        j.addProperty("columnId",    colId);
        j.addProperty("title",       title);
        j.addProperty("description", "Description de test");
        j.addProperty("priority",    "MEDIUM");
        j.addProperty("assignedTo",  0);
        j.addProperty("deadline",    "");
        return j;
    }

    private static int extractId(String result, String field) {
        return JsonParser.parseString(result)
                .getAsJsonObject()
                .getAsJsonObject("data")
                .get(field).getAsInt();
    }

    // ✅ Test 1 : createTask → tâche créée en base
    @Test
    @Order(1)
    void testCreateTask() {
        String result = taskService.createTask(buildTask("Ma Tâche", columnId), userClient);
        assertTrue(result.contains("\"status\":\"OK\""), "Expected OK but got: " + result);
        assertTrue(result.contains("Ma Tâche"), "Expected task title but got: " + result);
        taskId = extractId(result, "id");
    }

    // ✅ Test 2 : updateTask → champs mis à jour
    @Test
    @Order(2)
    void testUpdateTask() {
        JsonObject j = new JsonObject();
        j.addProperty("taskId",      taskId);
        j.addProperty("title",       "Tâche Modifiée");
        j.addProperty("description", "Nouvelle description");
        j.addProperty("priority",    "HIGH");
        j.addProperty("assignedTo",  0);
        j.addProperty("deadline",    "");
        String result = taskService.updateTask(j, userClient);
        assertTrue(result.contains("\"status\":\"OK\""), "Expected OK but got: " + result);
        assertTrue(result.contains("Tâche Modifiée"), "Expected updated title but got: " + result);
    }

    // ✅ Test 3 : moveTask → columnId mis à jour
    @Test
    @Order(3)
    void testMoveTask() {
        JsonObject j = new JsonObject();
        j.addProperty("taskId",         taskId);
        j.addProperty("targetColumnId", column2Id);
        String result = taskService.moveTask(j, userClient);
        assertTrue(result.contains("\"status\":\"OK\""), "Expected OK but got: " + result);
    }

    // ✅ Test 4 : moveTask avec WIP limit dépassée → ERROR
    @Test
    @Order(4)
    void testMoveTaskWipLimitReached() {
        // Créer une colonne avec WIP limit = 1 via H2 directement
        org.hibernate.Session session = com.kanban.util.HibernateUtil
                .getSessionFactory().openSession();
        org.hibernate.Transaction tx = session.beginTransaction();

        com.kanban.model.BoardColumn wipCol = new com.kanban.model.BoardColumn();
        wipCol.setBoardId(userClient.getCurrentBoardId());
        wipCol.setName("WIP Test");
        wipCol.setPosition(99);
        wipCol.setWipLimit(1); // limite à 1
        session.persist(wipCol);
        tx.commit();
        int wipColId = wipCol.getId();

        // Créer une tâche dans cette colonne (la remplit)
        taskService.createTask(buildTask("Tâche WIP 1", wipColId), userClient);

        // Essayer d'en déplacer une autre → doit être refusé
        String newTask = taskService.createTask(buildTask("Tâche WIP 2", columnId), userClient);
        int newTaskId = extractId(newTask, "id");

        JsonObject j = new JsonObject();
        j.addProperty("taskId",         newTaskId);
        j.addProperty("targetColumnId", wipColId);
        String result = taskService.moveTask(j, userClient);

        assertTrue(result.contains("\"status\":\"ERROR\""), "Expected ERROR but got: " + result);
        assertTrue(result.contains("WIP"), "Expected WIP message but got: " + result);

        session.close();
    }

    // ✅ Test 5 : addComment → commentaire sauvegardé
    @Test
    @Order(5)
    void testAddComment() {
        JsonObject j = new JsonObject();
        j.addProperty("taskId",  taskId);
        j.addProperty("content", "Super commentaire !");
        String result = taskService.addComment(j, userClient);
        assertTrue(result.contains("\"status\":\"OK\""), "Expected OK but got: " + result);
        assertTrue(result.contains("Super commentaire"), "Expected comment content but got: " + result);
    }

    // ✅ Test 6 : searchTasks → résultats filtrés
    @Test
    @Order(6)
    void testSearchTasks() {
        taskService.createTask(buildTask("Fix bug login", columnId), userClient);
        taskService.createTask(buildTask("Fix bug dashboard", columnId), userClient);
        taskService.createTask(buildTask("Design homepage", columnId), userClient);

        JsonObject j = new JsonObject();
        j.addProperty("query",   "bug");
        j.addProperty("boardId", userClient.getCurrentBoardId());
        String result = taskService.searchTasks(j, userClient);

        assertTrue(result.contains("\"status\":\"OK\""), "Expected OK but got: " + result);
        assertTrue(result.contains("bug"), "Expected bug tasks but got: " + result);
        assertFalse(result.contains("Design homepage"), "Should not contain unrelated task");
    }

    // ✅ Test 7 : getComments → retourne les commentaires de la tâche
    @Test
    @Order(7)
    void testGetComments() {
        JsonObject j = new JsonObject();
        j.addProperty("taskId", taskId);
        String result = taskService.getComments(j, userClient);
        assertTrue(result.contains("\"status\":\"OK\""), "Expected OK but got: " + result);
    }

    // ✅ Test 8 : deleteTask → tâche supprimée
    @Test
    @Order(8)
    void testDeleteTask() {
        String created = taskService.createTask(buildTask("À supprimer", columnId), userClient);
        int idToDelete = extractId(created, "id");

        JsonObject j = new JsonObject();
        j.addProperty("taskId", idToDelete);
        String result = taskService.deleteTask(j, userClient);
        assertTrue(result.contains("\"status\":\"OK\""), "Expected OK but got: " + result);
    }

    // ❌ Test 9 : deleteTask inexistante → ERROR
    @Test
    @Order(9)
    void testDeleteTaskNotFound() {
        JsonObject j = new JsonObject();
        j.addProperty("taskId", 99999);
        String result = taskService.deleteTask(j, userClient);
        assertTrue(result.contains("\"status\":\"ERROR\""), "Expected ERROR but got: " + result);
    }

    // ❌ Test 10 : Task.Builder sans titre → exception
    @Test
    @Order(10)
    void testTaskBuilderNoTitle() {
        assertThrows(Exception.class, () -> {
            new com.kanban.model.Task.Builder(columnId, "", userClient.getUserId())
                    .description("Pas de titre")
                    .build();
        });
    }
}