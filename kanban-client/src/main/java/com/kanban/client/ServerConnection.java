package com.kanban.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;

public class ServerConnection {

    private static ServerConnection instance;
    private Socket         socket;
    private BufferedReader in;
    private PrintWriter    out;

    private static final String HOST = "localhost";
    private static final int    PORT = 5008;

    private ServerConnection() throws IOException {
        socket = new Socket(HOST, PORT);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        System.out.println("✅ Connecté au serveur");
    }

    // ── Connexion principale (singleton) ────────────────────────────────────
    public static ServerConnection getInstance() throws IOException {
        if (!isAlive()) {
            instance = new ServerConnection();
        }
        return instance;
    }

    public static ServerConnection newConnection() throws IOException {
        ServerConnection conn = new ServerConnection();

        // ✅ Re-authentifier automatiquement
        String email    = MainApp.getCurrentEmail();
        String password = MainApp.getCurrentPassword();

        if (email != null && password != null) {
            JsonObject loginData = new JsonObject();
            loginData.addProperty("email",    email);
            loginData.addProperty("password", password);

            JsonObject loginResp = conn.sendRequest("LOGIN", loginData);
            if (loginResp == null || !"OK".equals(loginResp.get("status").getAsString())) {
                conn.close();
                throw new IOException("Re-authentification échouée sur nouvelle connexion");
            }
        }

        return conn;
    }

    private static boolean isAlive() {
        if (instance == null)               return false;
        if (instance.socket == null)        return false;
        if (instance.socket.isClosed())     return false;
        if (!instance.socket.isConnected()) return false;
        try {
            instance.socket.getOutputStream().flush();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // ── Envoi de requête ─────────────────────────────────────────────────────
    public JsonObject sendRequest(String action, JsonObject data) {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("action", action);
            if (data != null) request.add("data", data);
            out.println(request.toString());
            String response = in.readLine();
            if (response == null) return null;
            return JsonParser.parseString(response).getAsJsonObject();
        } catch (IOException e) {
            System.err.println("❌ Erreur communication : " + e.getMessage());
            instance = null;
            return null;
        }
    }

    public BufferedReader getIn() { return in; }

    public void close() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
        if (this == instance) instance = null;
    }
}