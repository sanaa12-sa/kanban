package com.kanban.controller;

import com.google.gson.*;
import com.kanban.client.AsyncServer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class CommentsController {

    @FXML private Label    taskTitleLabel;
    @FXML private VBox     commentsBox;
    @FXML private TextArea commentInput;
    @FXML private Label    errorLabel;

    private JsonObject task;

    public void setTask(JsonObject task) {
        this.task = task;
        taskTitleLabel.setText(task.get("title").getAsString());
        loadComments();
    }

    @FXML
    public void initialize() {}

    // ── Charger les commentaires ─────────────────────────────────────────────

    private void loadComments() {
        commentsBox.getChildren().clear();
        Label loading = new Label("⏳ Chargement...");
        loading.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 13px;");
        commentsBox.getChildren().add(loading);

        JsonObject data = new JsonObject();
        data.addProperty("taskId", task.get("id").getAsInt());

        // ✅ AsyncServer = connexion dédiée, ne bloque pas le ServerListener
        AsyncServer.sendRequest("GET_COMMENTS", data,
                response -> {
                    commentsBox.getChildren().clear();
                    JsonArray comments = response.getAsJsonArray("data");

                    if (comments.size() == 0) {
                        Label empty = new Label("Aucun commentaire pour l'instant.");
                        empty.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 13px;");
                        commentsBox.getChildren().add(empty);
                        return;
                    }

                    for (JsonElement el : comments) {
                        commentsBox.getChildren().add(createCommentCard(el.getAsJsonObject()));
                    }
                },
                error -> {
                    commentsBox.getChildren().clear();
                    Platform.runLater(() ->
                            errorLabel.setText("Erreur chargement : " + error));
                }
        );
    }

    // ── Créer une carte commentaire ──────────────────────────────────────────

    private VBox createCommentCard(JsonObject comment) {
        VBox card = new VBox(4);
        card.setStyle(
                "-fx-background-color: #313244;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-padding: 10px;"
        );

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label userLabel = new Label("👤 Utilisateur #" + comment.get("userId").getAsInt());
        userLabel.setStyle("-fx-text-fill: #89b4fa; -fx-font-size: 12px; -fx-font-weight: bold;");

        String createdAt = comment.has("createdAt") && !comment.get("createdAt").isJsonNull()
                ? comment.get("createdAt").getAsString().replace("T", " ").substring(0, 16)
                : "";
        Label dateLabel = new Label(createdAt);
        dateLabel.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(userLabel, spacer, dateLabel);

        Label content = new Label(comment.get("content").getAsString());
        content.setStyle("-fx-text-fill: #cdd6f4; -fx-font-size: 13px;");
        content.setWrapText(true);

        card.getChildren().addAll(header, content);
        return card;
    }

    // ── Envoyer un commentaire ───────────────────────────────────────────────

    @FXML
    private void handleSendComment() {
        String content = commentInput.getText().trim();
        if (content.isEmpty()) {
            errorLabel.setText("Le commentaire ne peut pas être vide");
            return;
        }

        // Désactiver le bouton pendant l'envoi pour éviter double-clic
        commentInput.setDisable(true);
        errorLabel.setText("");

        JsonObject data = new JsonObject();
        data.addProperty("taskId",  task.get("id").getAsInt());
        data.addProperty("content", content);

        // ✅ AsyncServer = non-bloquant
        AsyncServer.sendRequest("ADD_COMMENT", data,
                response -> {
                    commentInput.clear();
                    commentInput.setDisable(false);
                    loadComments();
                },
                error -> {
                    Platform.runLater(() -> {
                        errorLabel.setText("Impossible de contacter le serveur : " + error);
                        commentInput.setDisable(false);
                    });
                }
        );
    }

    @FXML
    private void handleClose() {
        commentInput.getScene().getWindow().hide();
    }
}