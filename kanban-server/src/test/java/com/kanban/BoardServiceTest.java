package com.kanban;

import com.google.gson.JsonObject;
import com.kanban.service.BoardService;
import com.kanban.service.UserService;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BoardServiceTest extends BaseTest {

    private static BoardService boardService;
    private static UserService  userService;
    private static MockClientHandler ownerClient;
    private static MockClientHandler otherClient;
    private static MockClientHandler inviteeClient;

    @BeforeAll
    static void setup() throws Exception {
        setupDatabase();
        boardService = new BoardService();
        userService  = new UserService();

        // Créer 3 utilisateurs pour les tests
        userService.register(buildUser("Owner User",   "owner@board.com",   "password123"));
        userService.register(buildUser("Other User",   "other@board.com",   "password123"));
        userService.register(buildUser("Invitee User", "invitee@board.com", "password123"));

        // Login pour récupérer les userId
        ownerClient   = new MockClientHandler(-1);
        otherClient   = new MockClientHandler(-1);
        inviteeClient = new MockClientHandler(-1);

        userService.login(buildLogin("owner@board.com",   "password123"), ownerClient);
        userService.login(buildLogin("other@board.com",   "password123"), otherClient);
        userService.login(buildLogin("invitee@board.com", "password123"), inviteeClient);
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

    private JsonObject buildBoard(String title) {
        JsonObject j = new JsonObject();
        j.addProperty("title",       title);
        j.addProperty("description", "Description test");
        j.addProperty("themeColor",  "#FFFFFF");
        j.addProperty("visibility",  "PRIVATE");
        return j;
    }

    private JsonObject buildInvite(int boardId, String email) {
        JsonObject j = new JsonObject();
        j.addProperty("boardId", boardId);
        j.addProperty("email",   email);
        return j;
    }

    private JsonObject buildBoardId(int boardId) {
        JsonObject j = new JsonObject();
        j.addProperty("boardId", boardId);
        return j;
    }

    // ── Extraire boardId depuis la réponse JSON ───────────────────────────
    private int extractBoardId(String result) {
        // result = {"status":"OK","data":{"id":1,...}}
        com.google.gson.JsonObject json = com.google.gson.JsonParser
                .parseString(result).getAsJsonObject();
        return json.getAsJsonObject("data").get("id").getAsInt();
    }

    // ✅ Test 1 : createBoard → board créé avec 4 colonnes par défaut
    @Test
    @Order(1)
    void testCreateBoardWithDefaultColumns() {
        String result = boardService.createBoard(buildBoard("Mon Board"), ownerClient);
        assertTrue(result.contains("\"status\":\"OK\""), "Expected OK but got: " + result);
        assertTrue(result.contains("Mon Board"), "Expected board title but got: " + result);
    }

    // ✅ Test 2 : getBoards → retourne les boards du bon utilisateur
    @Test
    @Order(2)
    void testGetBoardsForUser() {
        boardService.createBoard(buildBoard("Board A"), ownerClient);
        boardService.createBoard(buildBoard("Board B"), ownerClient);
        String result = boardService.getBoards(ownerClient);
        assertTrue(result.contains("\"status\":\"OK\""), "Expected OK but got: " + result);
        assertTrue(result.contains("Board A"), "Expected Board A but got: " + result);
        assertTrue(result.contains("Board B"), "Expected Board B but got: " + result);
    }

    // ✅ Test 3 : inviteMember avec email existant → OK
    @Test
    @Order(3)
    void testInviteMemberSuccess() {
        String createResult = boardService.createBoard(buildBoard("Invite Board"), ownerClient);
        int boardId = extractBoardId(createResult);
        String result = boardService.inviteMember(buildInvite(boardId, "invitee@board.com"), ownerClient);
        assertTrue(result.contains("\"status\":\"OK\""), "Expected OK but got: " + result);
    }

    // ❌ Test 4 : inviteMember avec email inexistant → ERROR
    @Test
    @Order(4)
    void testInviteMemberUnknownEmail() {
        String createResult = boardService.createBoard(buildBoard("Board X"), ownerClient);
        int boardId = extractBoardId(createResult);
        String result = boardService.inviteMember(buildInvite(boardId, "ghost@nowhere.com"), ownerClient);
        assertTrue(result.contains("\"status\":\"ERROR\""), "Expected ERROR but got: " + result);
        assertTrue(result.contains("introuvable"), "Expected not found message but got: " + result);
    }

    // ❌ Test 5 : inviteMember si déjà membre → ERROR
    @Test
    @Order(5)
    void testInviteMemberAlreadyMember() {
        String createResult = boardService.createBoard(buildBoard("Board Y"), ownerClient);
        int boardId = extractBoardId(createResult);
        // Première invitation → OK
        boardService.inviteMember(buildInvite(boardId, "invitee@board.com"), ownerClient);
        // Deuxième invitation même personne → ERROR
        String result = boardService.inviteMember(buildInvite(boardId, "invitee@board.com"), ownerClient);
        assertTrue(result.contains("\"status\":\"ERROR\""), "Expected ERROR but got: " + result);
        assertTrue(result.contains("déjà membre"), "Expected already member message but got: " + result);
    }

    // ❌ Test 6 : inviteMember par quelqu'un qui n'est pas owner → ERROR
    @Test
    @Order(6)
    void testInviteMemberNotOwner() {
        String createResult = boardService.createBoard(buildBoard("Board Z"), ownerClient);
        int boardId = extractBoardId(createResult);
        String result = boardService.inviteMember(buildInvite(boardId, "invitee@board.com"), otherClient);
        assertTrue(result.contains("\"status\":\"ERROR\""), "Expected ERROR but got: " + result);
    }

    // ✅ Test 7 : deleteBoard par le propriétaire → OK
    @Test
    @Order(7)
    void testDeleteBoardByOwner() {
        String createResult = boardService.createBoard(buildBoard("To Delete"), ownerClient);
        int boardId = extractBoardId(createResult);
        String result = boardService.deleteBoard(buildBoardId(boardId), ownerClient);
        assertTrue(result.contains("\"status\":\"OK\""), "Expected OK but got: " + result);
    }

    // ❌ Test 8 : deleteBoard par quelqu'un d'autre → ERROR
    @Test
    @Order(8)
    void testDeleteBoardByNonOwner() {
        String createResult = boardService.createBoard(buildBoard("Protected Board"), ownerClient);
        int boardId = extractBoardId(createResult);
        String result = boardService.deleteBoard(buildBoardId(boardId), otherClient);
        assertTrue(result.contains("\"status\":\"ERROR\""), "Expected ERROR but got: " + result);
        assertTrue(result.contains("propriétaire"), "Expected owner message but got: " + result);
    }

    // ✅ Test 9 : Board.Builder → objet créé correctement
    @Test
    @Order(9)
    void testBoardBuilder() {
        com.kanban.model.Board board = new com.kanban.model.Board.Builder(
                "Builder Board",
                ownerClient.getUserId())
                .description("Une description")
                .themeColor("#FF0000")
                .visibility(com.kanban.model.Board.Visibility.PRIVATE)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        assertNotNull(board);
        assertEquals("Builder Board", board.getTitle());
        assertEquals(ownerClient.getUserId(), board.getCreatedBy());
    }
}