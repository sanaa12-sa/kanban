package com.kanban.client;

import com.google.gson.JsonObject;
import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class AsyncServer {

    private static final int TIMEOUT_SECONDS = 60;

    public static void sendRequest(String action, JsonObject data,
                                   Consumer<JsonObject> onSuccess,
                                   Consumer<String> onError) {
        CompletableFuture.supplyAsync(() -> {
                    // ✅ Connexion DÉDIÉE à chaque requête — jamais le singleton
                    ServerConnection conn = null;
                    try {
                        System.out.println("📤 AsyncServer envoi: " + action);
                        conn = ServerConnection.newConnection();   // ← clé du fix
                        JsonObject response = conn.sendRequest(action, data);
                        System.out.println("📥 AsyncServer réponse reçue: " + action);
                        return response;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        // ✅ Fermer proprement après chaque requête
                        if (conn != null) conn.close();
                    }
                })
                .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .thenAccept(response -> Platform.runLater(() -> {
                    try {
                        if (response != null
                                && "OK".equals(response.get("status").getAsString())) {
                            if (onSuccess != null) onSuccess.accept(response);
                        } else {
                            String errorMsg = response != null && response.has("message")
                                    ? response.get("message").getAsString()
                                    : "Erreur inconnue";
                            if (onError != null) onError.accept(errorMsg);
                        }
                    } catch (Exception e) {
                        if (onError != null)
                            onError.accept("Erreur traitement: " + e.getMessage());
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (onError != null)
                            onError.accept("Timeout ou erreur réseau: " + ex.getMessage());
                    });
                    return null;
                });
    }

    public static void sendRequest(String action, JsonObject data,
                                   Consumer<JsonObject> onSuccess) {
        sendRequest(action, data, onSuccess, null);
    }
}