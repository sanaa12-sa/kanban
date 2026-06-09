package com.kanban.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;

public class ServerListener implements Runnable {

    private ServerConnection connection;
    private BroadcastHandler handler;
    private boolean running = true;

    public ServerListener(ServerConnection connection, BroadcastHandler handler) {
        this.connection = connection;
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            String line;
            while (running && (line = connection.getIn().readLine()) != null) {
                final String message = line;
                System.out.println("📡 Broadcast reçu : " + message);

                Platform.runLater(() -> {
                    try {
                        JsonObject json = JsonParser.parseString(message).getAsJsonObject();
                        // Vérifier si le message a un champ "type" (broadcast)
                        // ou si c'est une réponse normale
                        if (json.has("type")) {
                            String type = json.get("type").getAsString();
                            handler.onBroadcast(type, json);
                        } else {
                            // C'est une réponse normale, pas un broadcast
                            System.out.println("📩 Réponse normale reçue (ignorée par listener)");
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Erreur traitement broadcast : " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("❌ Connexion perdue : " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        connection.close(); // ✅ Ferme la connexion DÉDIÉE du listener, pas le singleton
    }
    public interface BroadcastHandler {
        void onBroadcast(String type, JsonObject data);
    }

}