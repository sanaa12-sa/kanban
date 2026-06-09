package com.kanban;

import com.google.gson.JsonObject;
import com.kanban.service.UserService;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserServiceTest extends BaseTest {

    private static UserService userService;

    @BeforeAll
    static void setup() throws Exception {
        setupDatabase();
        userService = new UserService();
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private JsonObject registerJson(String name, String email, String password) {
        JsonObject j = new JsonObject();
        j.addProperty("fullName",  name);
        j.addProperty("email",     email);
        j.addProperty("password",  password);
        return j;
    }

    private JsonObject loginJson(String email, String password) {
        JsonObject j = new JsonObject();
        j.addProperty("email",    email);
        j.addProperty("password", password);
        return j;
    }

    // ✅ Test 1 : register avec email valide → OK
    @Test
    @Order(1)
    void testRegisterSuccess() {
        String result = userService.register(registerJson("Alice Dupont", "alice@test.com", "password123"));
        assertTrue(result.contains("\"status\":\"OK\""), "Expected OK but got: " + result);
    }

    // ❌ Test 2 : register avec email déjà utilisé → ERROR
    @Test
    @Order(2)
    void testRegisterDuplicateEmail() {
        userService.register(registerJson("Bob Martin", "bob@test.com", "password123"));
        String result = userService.register(registerJson("Bob2", "bob@test.com", "autrepass1"));
        assertTrue(result.contains("\"status\":\"ERROR\""), "Expected ERROR but got: " + result);
        assertTrue(result.contains("déjà utilisé"), "Expected duplicate message but got: " + result);
    }

    // ❌ Test 3 : register avec mot de passe trop court → ERROR
    @Test
    @Order(3)
    void testRegisterShortPassword() {
        String result = userService.register(registerJson("Charlie", "charlie@test.com", "123"));
        assertTrue(result.contains("\"status\":\"ERROR\""), "Expected ERROR but got: " + result);
        assertTrue(result.contains("8 caract"), "Expected password message but got: " + result);
    }

    // ❌ Test 4 : register avec email invalide → ERROR
    @Test
    @Order(4)
    void testRegisterInvalidEmail() {
        String result = userService.register(registerJson("Dave", "notanemail", "password123"));
        assertTrue(result.contains("\"status\":\"ERROR\""), "Expected ERROR but got: " + result);
        assertTrue(result.contains("Email invalide"), "Expected email message but got: " + result);
    }

    // ❌ Test 5 : register sans nom → ERROR
    @Test
    @Order(5)
    void testRegisterEmptyName() {
        String result = userService.register(registerJson("", "noname@test.com", "password123"));
        assertTrue(result.contains("\"status\":\"ERROR\""), "Expected ERROR but got: " + result);
    }

    // ✅ Test 6 : login avec bons identifiants → OK
    @Test
    @Order(6)
    void testLoginSuccess() {
        userService.register(registerJson("Eve Lambert", "eve@test.com", "securepass1"));
        MockClientHandler client = new MockClientHandler(-1);
        String result = userService.login(loginJson("eve@test.com", "securepass1"), client);
        assertTrue(result.contains("\"status\":\"OK\""), "Expected OK but got: " + result);
        assertTrue(result.contains("eve@test.com"), "Expected email in response but got: " + result);
    }

    // ❌ Test 7 : login avec mauvais mot de passe → ERROR
    @Test
    @Order(7)
    void testLoginWrongPassword() {
        userService.register(registerJson("Frank Noir", "frank@test.com", "correctpass"));
        MockClientHandler client = new MockClientHandler(-1);
        String result = userService.login(loginJson("frank@test.com", "wrongpass123"), client);
        assertTrue(result.contains("\"status\":\"ERROR\""), "Expected ERROR but got: " + result);
    }

    // ❌ Test 8 : login avec email inexistant → ERROR
    @Test
    @Order(8)
    void testLoginUnknownEmail() {
        MockClientHandler client = new MockClientHandler(-1);
        String result = userService.login(loginJson("ghost@test.com", "password123"), client);
        assertTrue(result.contains("\"status\":\"ERROR\""), "Expected ERROR but got: " + result);
    }

    // ✅ Test 9 : Builder User valide
    @Test
    @Order(9)
    void testUserBuilderValid() {
        com.kanban.model.User user = new com.kanban.model.User.Builder(
                "Grace Hopper",
                "grace@test.com",
                "hashedpassword")
                .createdAt(java.time.LocalDateTime.now())
                .build();
        assertNotNull(user);
        assertEquals("grace@test.com", user.getEmail());
        assertEquals("Grace Hopper",   user.getFullName());
    }

    // ✅ Test 10 : userId bien défini après login
    @Test
    @Order(10)
    void testLoginSetsUserId() {
        userService.register(registerJson("Henri Paul", "henri@test.com", "mypassword1"));
        MockClientHandler client = new MockClientHandler(-1);
        userService.login(loginJson("henri@test.com", "mypassword1"), client);
        assertTrue(client.getUserId() > 0, "userId should be set after login but got: " + client.getUserId());
    }
}