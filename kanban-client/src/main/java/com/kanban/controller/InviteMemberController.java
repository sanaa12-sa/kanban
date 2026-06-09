package com.kanban.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kanban.client.ServerConnection;
import com.kanban.client.ToastNotification;
import com.kanban.client.MainApp;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class InviteMemberController {

    @FXML private TextField emailField;
    @FXML private Label     errorLabel;
    @FXML private Label     successLabel;
    @FXML private VBox      membersBox;
    @FXML private Button    inviteBtn;

    private int              boardId;
    private ServerConnection conn; // connexion dédiée à cette modale

    // ── Initialisation ───────────────────────────────────────────────────────

    public void setBoardId(int boardId) {
        this.boardId = boardId;
        // Ouvrir une connexion dédiée pour cette modale
        try {
            conn = ServerConnection.newConnection();
        } catch (Exception e) {
            showError("Impossible de se connecter au serveur");
            return;
        }
        loadMembers();
    }

    @FXML
    public void initialize() {}

    // ── Chargement des membres ───────────────────────────────────────────────

    private void loadMembers() {
        membersBox.getChildren().clear();
        Label loading = new Label("⏳ Chargement...");
        loading.setStyle("-fx-text-fill: #6B6B80; -fx-font-size: 12px;");
        membersBox.getChildren().add(loading);

        // Thread séparé pour ne pas bloquer l'UI
        Thread t = new Thread(() -> {
            try {
                JsonObject data = new JsonObject();
                data.addProperty("boardId", boardId);
                JsonObject response = conn.sendRequest("GET_MEMBERS", data);

                Platform.runLater(() -> {
                    membersBox.getChildren().clear();
                    if (response != null && "OK".equals(response.get("status").getAsString())) {
                        afficherMembres(response.getAsJsonArray("data"));
                    } else {
                        showMembersError("Impossible de charger les membres");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showMembersError("Erreur : " + e.getMessage()));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void afficherMembres(JsonArray members) {
        if (members == null || members.size() == 0) {
            Label empty = new Label("Aucun membre pour l'instant.");
            empty.setStyle("-fx-text-fill: #6B6B80; -fx-font-size: 12px;");
            membersBox.getChildren().add(empty);
            return;
        }

        Label title = new Label("Membres (" + members.size() + ")");
        title.setStyle("-fx-text-fill: #1B3A5C; -fx-font-size: 12px; -fx-font-weight: bold;");
        membersBox.getChildren().add(title);

        for (JsonElement el : members) {
            JsonObject member = el.getAsJsonObject();
            String name  = member.get("fullName").getAsString();
            String email = member.get("email").getAsString();
            String role  = member.get("role").getAsString();

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle(
                    "-fx-background-color: #F7F4EF;" +
                            "-fx-background-radius: 8;" +
                            "-fx-padding: 8 12;"
            );

            // Avatar initiale
            Label avatar = new Label(String.valueOf(name.charAt(0)).toUpperCase());
            avatar.setStyle(
                    "-fx-background-color: #1B3A5C;" +
                            "-fx-text-fill: #FFFFFF;" +
                            "-fx-font-size: 12px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 50%;" +
                            "-fx-min-width: 30px; -fx-min-height: 30px;" +
                            "-fx-max-width: 30px; -fx-max-height: 30px;" +
                            "-fx-alignment: center;"
            );

            VBox info = new VBox(2);
            Label nameLbl  = new Label(name);
            nameLbl.setStyle("-fx-text-fill: #1A1A2E; -fx-font-size: 12px; -fx-font-weight: bold;");
            Label emailLbl = new Label(email);
            emailLbl.setStyle("-fx-text-fill: #6B6B80; -fx-font-size: 10px;");
            info.getChildren().addAll(nameLbl, emailLbl);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label roleLbl = new Label(role);
            roleLbl.setStyle(
                    "-fx-background-color: #EBF0F6;" +
                            "-fx-text-fill: #1B3A5C;" +
                            "-fx-font-size: 10px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-padding: 2 8;" +
                            "-fx-background-radius: 10;"
            );

            row.getChildren().addAll(avatar, info, roleLbl);
            membersBox.getChildren().add(row);
        }
    }

    private void showMembersError(String msg) {
        Label err = new Label("⚠ " + msg);
        err.setStyle("-fx-text-fill: #7B2D42; -fx-font-size: 12px;");
        membersBox.getChildren().add(err);
    }

    // ── Invitation ───────────────────────────────────────────────────────────

    @FXML
    private void handleInvite() {
        String email = emailField.getText().trim();
        errorLabel.setText("");
        successLabel.setText("");

        if (email.isEmpty() || !email.contains("@")) {
            showError("Format email invalide");
            return;
        }

        inviteBtn.setDisable(true);
        inviteBtn.setText("Envoi...");

        Thread t = new Thread(() -> {
            try {
                JsonObject data = new JsonObject();
                data.addProperty("email",   email);
                data.addProperty("boardId", boardId);

                JsonObject response = conn.sendRequest("INVITE_MEMBER", data);

                Platform.runLater(() -> {
                    inviteBtn.setDisable(false);
                    inviteBtn.setText("Inviter ✉️");

                    if (response != null && "OK".equals(response.get("status").getAsString())) {
                        successLabel.setStyle("-fx-text-fill: #1a7a4a; -fx-font-size: 12px;");
                        successLabel.setText("✅ Invitation envoyée à " + email);
                        emailField.clear();
                        // Fermer la connexion actuelle et en ouvrir une nouvelle
                        // pour le rechargement des membres
                        try {
                            conn.close();
                            conn = ServerConnection.newConnection();
                        } catch (Exception ignored) {}
                        loadMembers();
                        ToastNotification.show(MainApp.getPrimaryStage(), "✅ Membre invité !");
                    } else {
                        String msg = (response != null && response.has("message"))
                                ? response.get("message").getAsString()
                                : "Erreur serveur";
                        showError(msg);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    inviteBtn.setDisable(false);
                    inviteBtn.setText("Inviter ✉️");
                    showError("Impossible de contacter le serveur");
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Fermeture ────────────────────────────────────────────────────────────

    @FXML
    private void handleClose() {
        // Fermer la connexion dédiée proprement
        if (conn != null) conn.close();
        ((Stage) emailField.getScene().getWindow()).close();
    }

    // ── Utilitaire ───────────────────────────────────────────────────────────

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill: #7B2D42; -fx-font-size: 12px; -fx-font-weight: bold;");
        errorLabel.setText("⚠ " + msg);
    }
}