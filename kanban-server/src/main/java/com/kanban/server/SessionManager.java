package com.kanban.server;

import java.util.HashMap;
import java.util.Map;

public class SessionManager {

    // Le carnet : userId → ClientHandler
    private static Map<Integer, ClientHandler> sessions = new HashMap<>();

    // ✅ Ajouter quelqu'un (après LOGIN réussi)
    public static void addSession(int userId, ClientHandler client) {
        sessions.put(userId, client);
        System.out.println("✅ Session ajoutée pour userId : " + userId);
    }

    // ❌ Supprimer quelqu'un (après déconnexion)
    public static void removeSession(int userId) {
        sessions.remove(userId);
        System.out.println("🔌 Session supprimée pour userId : " + userId);
    }

    // 🔍 Trouver le ClientHandler d'un utilisateur
    public static ClientHandler getClient(int userId) {
        return sessions.get(userId);
    }

    // ❓ Est-ce que cet utilisateur est connecté ?
    public static boolean isConnected(int userId) {
        return sessions.containsKey(userId);
    }

    // 🔢 Combien de personnes connectées ?
    public static int getConnectedCount() {
        return sessions.size();
    }
}