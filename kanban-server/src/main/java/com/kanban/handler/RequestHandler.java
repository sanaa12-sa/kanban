package com.kanban.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kanban.server.ClientHandler;
import com.kanban.service.BoardService;
import com.kanban.service.TaskService;
import com.kanban.service.UserService;

public class RequestHandler {

    private static final Gson gson = new Gson();

    private static final UserService  userService  = new UserService();
    private static final BoardService boardService = new BoardService();
    private static final TaskService  taskService  = new TaskService();

    public static String handle(String message, ClientHandler client) {
        try {
            JsonObject request = gson.fromJson(message, JsonObject.class);
            String     action  = request.get("action").getAsString();
            JsonObject data    = request.has("data")
                    ? request.getAsJsonObject("data") : new JsonObject();

            System.out.println("⚙️  Action reçue : " + action);

            return switch (action) {

                // ── Utilisateurs ────────────────────────────────────────────
                case "REGISTER"               -> userService.register(data);
                case "LOGIN"                  -> userService.login(data, client);
                case "GET_USER_BY_ID"         -> userService.getUserById(data, client);
                case "UPDATE_NOTIF_SETTINGS"  -> userService.updateNotifSettings(data, client);
                case "UPDATE_DEADLINE_DELAY"  -> userService.updateDeadlineDelay(data, client);
                case "UPDATE_PROFILE" -> userService.updateProfile(data, client);
                // ── Boards ──────────────────────────────────────────────────
                case "CREATE_BOARD"    -> boardService.createBoard(data, client);
                case "GET_BOARDS"      -> boardService.getBoards(client);
                case "DELETE_BOARD"    -> boardService.deleteBoard(data, client);
                case "INVITE_MEMBER"   -> boardService.inviteMember(data, client);
                case "GET_MEMBERS"     -> boardService.getMembers(data, client);
                case "GET_BOARD_BY_ID" -> boardService.getBoardById(data, client);

                // ── Tâches ──────────────────────────────────────────────────
                case "CREATE_TASK"     -> taskService.createTask(data, client);
                case "UPDATE_TASK"     -> taskService.updateTask(data, client);
                case "DELETE_TASK"     -> taskService.deleteTask(data, client);
                case "MOVE_TASK"       -> taskService.moveTask(data, client);
                case "GET_TASKS"       -> taskService.getTasks(data, client);
                case "SEARCH_TASKS"    -> taskService.searchTasks(data, client);
                case "GET_ALL_TASKS"   -> taskService.getAllTasks(client);
                case "FILTER_TASKS"    -> taskService.filterTasks(data, client);
                case "GET_COLUMN_BY_ID"-> taskService.getColumnById(data, client);

                // ── Commentaires ─────────────────────────────────────────────
                case "ADD_COMMENT"     -> taskService.addComment(data, client);
                case "GET_COMMENTS"    -> taskService.getComments(data, client);

                default -> error("Action inconnue : " + action);
            };

        } catch (Exception e) {
            System.err.println("❌ Erreur traitement requête : " + e.getMessage());
            return error("Erreur serveur : " + e.getMessage());
        }
    }

    public static String success(String message, Object data) {
        JsonObject response = new JsonObject();
        response.addProperty("status",  "OK");
        response.addProperty("message", message);
        if (data != null) {
            response.add("data", gson.toJsonTree(data));
        }
        return gson.toJson(response);
    }

    public static String error(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("status",  "ERROR");
        response.addProperty("message", message);
        return gson.toJson(response);
    }
}