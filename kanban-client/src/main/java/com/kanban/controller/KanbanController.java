package com.kanban.controller;

import com.google.gson.*;
import com.kanban.client.AsyncServer;
import com.kanban.client.MainApp;
import com.kanban.client.ServerConnection;
import com.kanban.client.ServerListener;
import com.kanban.client.ToastNotification;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KanbanController {

    private static final Logger logger = LoggerFactory.getLogger(KanbanController.class);

    @FXML private HBox  columnsContainer;
    @FXML private Label boardTitleLabel;
    @FXML private Label boardTitleTopLabel;

    private JsonObject     currentBoard;
    private ServerListener serverListener;  // ✅ référence directe au listener
    private Thread         listenerThread;

    // ── Initialisation ───────────────────────────────────────────────────────

    public void initBoard(JsonObject board) {
        this.currentBoard = board;
        String title = board.get("title").getAsString();
        if (boardTitleLabel    != null) boardTitleLabel.setText(title);
        if (boardTitleTopLabel != null) boardTitleTopLabel.setText(title);
        refreshKanban();
        startServerListener();
    }

    @FXML
    public void initialize() {}

    // ── Rafraîchissement du Kanban ───────────────────────────────────────────

    private void refreshKanban() { loadColumnsAndTasks(); }

    private void loadColumnsAndTasks() {
        Platform.runLater(() -> {
            columnsContainer.getChildren().clear();
            Label loading = new Label("⏳ Chargement des tâches...");
            loading.setStyle("-fx-text-fill: #6B6B80; -fx-padding: 20;");
            columnsContainer.getChildren().add(loading);
        });

        JsonObject data = new JsonObject();
        data.addProperty("boardId", currentBoard.get("id").getAsInt());

        AsyncServer.sendRequest("GET_TASKS", data,
                response -> {
                    columnsContainer.getChildren().clear();
                    JsonArray columns = response.getAsJsonArray("data");
                    for (JsonElement el : columns) {
                        JsonObject col = el.getAsJsonObject();
                        columnsContainer.getChildren().add(
                                createColumn(
                                        col.get("id").getAsInt(),
                                        col.get("name").getAsString(),
                                        col.getAsJsonArray("tasks")
                                ));
                    }
                },
                error -> {
                    columnsContainer.getChildren().clear();
                    Label err = new Label("❌ Erreur: " + error);
                    err.setStyle("-fx-text-fill: #7B2D42; -fx-padding: 20;");
                    columnsContainer.getChildren().add(err);
                }
        );
    }

    // ── ServerListener ───────────────────────────────────────────────────────

    private void startServerListener() {
        try {
            // ✅ Connexion DÉDIÉE pour le listener — pas le singleton
            ServerConnection listenerConnection = ServerConnection.newConnection();

            serverListener = new ServerListener(
                    listenerConnection,   // ← connexion propre au listener
                    (type, json) -> {
                        switch (type) {
                            case "TASK_CREATED", "TASK_UPDATED",
                                 "TASK_DELETED", "TASK_MOVED",
                                 "COMMENT_ADDED" ->
                                    Platform.runLater(() -> {
                                        loadColumnsAndTasks();
                                        String msg = getNotificationMessage(type);
                                        if (msg != null)
                                            ToastNotification.show(MainApp.getPrimaryStage(), msg);
                                    });
                        }
                    });
            listenerThread = new Thread(serverListener);
            listenerThread.setDaemon(true);
            listenerThread.start();
        } catch (Exception e) {
            System.err.println("⚠️ ServerListener non démarré : " + e.getMessage());
        }
    }

    private String getNotificationMessage(String type) {
        return switch (type) {
            case "TASK_CREATED" -> "✅ Nouvelle tâche ajoutée";
            case "TASK_UPDATED" -> "✏️ Tâche mise à jour";
            case "TASK_DELETED" -> "🗑️ Tâche supprimée";
            case "TASK_MOVED"   -> "↔️ Tâche déplacée";
            case "COMMENT_ADDED"-> "💬 Nouveau commentaire";
            default -> null;
        };
    }

    // ── Création d'une colonne ───────────────────────────────────────────────

    private VBox createColumn(int columnId, String name, JsonArray tasks) {
        VBox column = new VBox(10);
        column.getStyleClass().add("kanban-column");
        column.setPrefWidth(285);

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title   = new Label(name);
        title.getStyleClass().add("column-title");
        Label counter = new Label(String.valueOf(tasks.size()));
        counter.getStyleClass().add("column-counter");
        header.getChildren().addAll(title, counter);

        VBox tasksBox = new VBox(8);
        for (JsonElement taskEl : tasks)
            tasksBox.getChildren().add(createTaskCard(taskEl.getAsJsonObject(), tasksBox, counter));

        Button addBtn = new Button("+ Ajouter une tâche");
        addBtn.getStyleClass().add("btn-add-task");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> openCreateTask(columnId, tasksBox, counter));

        column.getChildren().addAll(header, tasksBox, addBtn);

        column.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) e.acceptTransferModes(TransferMode.MOVE);
            e.consume();
        });

        column.setOnDragDropped(e -> {
            String taskJson = e.getDragboard().getString();
            JsonObject task = JsonParser.parseString(taskJson).getAsJsonObject();

            JsonObject moveData = new JsonObject();
            moveData.addProperty("taskId", task.get("id").getAsInt());
            moveData.addProperty("targetColumnId", columnId);

            AsyncServer.sendRequest("MOVE_TASK", moveData,
                    res -> {
                        task.addProperty("columnId", columnId);
                        tasksBox.getChildren().add(createTaskCard(task, tasksBox, counter));
                        updateCounter(counter, tasksBox);
                    },
                    error -> {
                        ToastNotification.show(MainApp.getPrimaryStage(), "⚠️ " + error);
                        refreshKanban();
                    }
            );
            e.setDropCompleted(true);
            e.consume();
        });

        return column;
    }

    // ── Création d'une carte tâche ───────────────────────────────────────────

    private VBox createTaskCard(JsonObject task, VBox tasksBox, Label counter) {
        VBox card = new VBox(6);
        card.getStyleClass().add("task-card");

        String priority = task.has("priority") ? task.get("priority").getAsString() : "MEDIUM";
        Label badge = new Label(priority);
        badge.getStyleClass().add("badge-" + priority.toLowerCase());

        Label titleLabel = new Label(task.get("title").getAsString());
        titleLabel.getStyleClass().add("task-title");
        titleLabel.setWrapText(true);

        String desc = task.has("description") && !task.get("description").isJsonNull()
                ? task.get("description").getAsString() : "";
        Label descLabel = new Label(desc);
        descLabel.getStyleClass().add("task-description");

        String deadlineStr = task.has("deadline") && !task.get("deadline").isJsonNull()
                ? task.get("deadline").getAsString() : "";
        Label deadlineLabel = new Label(deadlineStr.isEmpty() ? "" : "📅 " + deadlineStr);

        if (!deadlineStr.isEmpty()) {
            try {
                java.time.LocalDate deadline = java.time.LocalDate.parse(deadlineStr);
                if (deadline.isBefore(java.time.LocalDate.now())) {
                    deadlineLabel.setStyle("-fx-text-fill: #7B2D42; -fx-font-size: 11px; -fx-font-weight: bold;");
                    deadlineLabel.setText("⚠️ " + deadlineStr + " (dépassée)");
                } else {
                    deadlineLabel.getStyleClass().add("task-deadline");
                }
            } catch (Exception ignored) {
                deadlineLabel.getStyleClass().add("task-deadline");
            }
        }

        int assignedTo = task.has("assignedTo") ? task.get("assignedTo").getAsInt() : 0;
        Label assignedLabel = new Label(assignedTo > 0 ? "👤 #" + assignedTo : "");
        assignedLabel.setStyle("-fx-text-fill: #6B6B80; -fx-font-size: 10px;");

        HBox actions = new HBox(6);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Button editBtn    = new Button("✏️");
        Button deleteBtn  = new Button("🗑️");
        Button commentBtn = new Button("💬");
        editBtn.getStyleClass().add("btn-task-action");
        deleteBtn.getStyleClass().add("btn-task-action");
        commentBtn.getStyleClass().add("btn-task-action");
        editBtn.setOnAction(e    -> openEditTask(task, card, tasksBox, counter));
        deleteBtn.setOnAction(e  -> handleDeleteTask(task, card, tasksBox, counter));
        commentBtn.setOnAction(e -> openComments(task));
        actions.getChildren().addAll(editBtn, commentBtn, deleteBtn);

        card.getChildren().addAll(badge, titleLabel, descLabel, deadlineLabel, assignedLabel, actions);

        card.setOnDragDetected(e -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent cc = new ClipboardContent();
            cc.putString(task.toString());
            db.setContent(cc);
            ((VBox) card.getParent()).getChildren().remove(card);
            e.consume();
        });

        return card;
    }

    // ── Fenêtres ─────────────────────────────────────────────────────────────

    private void openCreateTask(int columnId, VBox tasksBox, Label counter) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/com/kanban/views/create_task.fxml"));
            Scene scene = new Scene(loader.load(), 500, 500);
            applyStyle(scene);
            Stage dialog = new Stage();
            dialog.setTitle("Nouvelle Tâche");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(scene);
            CreateTaskController ctrl = loader.getController();
            ctrl.setColumnId(columnId);
            ctrl.setOnSuccess(task -> {
                dialog.close();
                tasksBox.getChildren().add(createTaskCard(task, tasksBox, counter));
                updateCounter(counter, tasksBox);
            });
            dialog.showAndWait();
        } catch (Exception e) {
            logger.error("Erreur ouverture création tâche", e);
        }
    }

    private void openEditTask(JsonObject task, VBox card, VBox tasksBox, Label counter) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/com/kanban/views/edit_task.fxml"));
            Scene scene = new Scene(loader.load(), 500, 500);
            applyStyle(scene);
            Stage dialog = new Stage();
            dialog.setTitle("Modifier la tâche");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(scene);
            EditTaskController ctrl = loader.getController();
            ctrl.setTask(task);
            ctrl.setOnSuccess(updated -> {
                dialog.close();
                int index = tasksBox.getChildren().indexOf(card);
                if (index >= 0)
                    tasksBox.getChildren().set(index, createTaskCard(updated, tasksBox, counter));
            });
            dialog.showAndWait();
        } catch (Exception e) {
            logger.error("Erreur ouverture édition tâche", e);
        }
    }

    private void handleDeleteTask(JsonObject task, VBox card, VBox tasksBox, Label counter) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer la tâche");
        confirm.setHeaderText("Supprimer \"" + task.get("title").getAsString() + "\" ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                JsonObject data = new JsonObject();
                data.addProperty("taskId", task.get("id").getAsInt());
                AsyncServer.sendRequest("DELETE_TASK", data,
                        response -> {
                            tasksBox.getChildren().remove(card);
                            updateCounter(counter, tasksBox);
                            ToastNotification.show(MainApp.getPrimaryStage(), "🗑️ Tâche supprimée");
                        },
                        error -> ToastNotification.show(MainApp.getPrimaryStage(), "❌ Erreur: " + error)
                );
            }
        });
    }

    private void openComments(JsonObject task) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/com/kanban/views/comments.fxml"));
            Scene scene = new Scene(loader.load(), 500, 550);
            applyStyle(scene);
            Stage dialog = new Stage();
            dialog.setTitle("Commentaires — " + task.get("title").getAsString());
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(scene);
            CommentsController ctrl = loader.getController();
            ctrl.setTask(task);
            dialog.showAndWait();
        } catch (Exception e) {
            logger.error("Erreur ouverture commentaires", e);
        }
    }

    @FXML
    private void handleInviteMember() {
        if (currentBoard == null) {
            ToastNotification.show(MainApp.getPrimaryStage(), "Aucun board sélectionné");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/com/kanban/views/invite_member.fxml"));
            Scene scene = new Scene(loader.load(), 420, 380);
            applyStyle(scene);
            Stage dialog = new Stage();
            dialog.setTitle("Inviter un membre");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(scene);
            InviteMemberController ctrl = loader.getController();
            ctrl.setBoardId(currentBoard.get("id").getAsInt());
            dialog.showAndWait();
        } catch (Exception e) {
            logger.error("Erreur ouverture invitation membre", e);
            ToastNotification.show(MainApp.getPrimaryStage(), "Erreur: " + e.getMessage());
        }
    }

    // ── Retour ───────────────────────────────────────────────────────────────

    @FXML
    private void handleBack() {
        try {
            // ✅ Juste arrêter le flag — ne pas fermer le socket singleton
            if (serverListener != null) {
                serverListener.stop();
                serverListener = null;
            }
            if (listenerThread != null) {
                listenerThread.interrupt();
                listenerThread = null;
            }

            // LEAVE_BOARD sur connexion dédiée
            if (currentBoard != null) {
                try {
                    JsonObject leaveData = new JsonObject();
                    leaveData.addProperty("boardId", currentBoard.get("id").getAsInt());
                    ServerConnection conn = ServerConnection.newConnection();
                    conn.sendRequest("LEAVE_BOARD", leaveData);
                    conn.close();
                } catch (Exception ignored) {}
            }

            MainApp.showDashboard();

        } catch (Exception e) {
            logger.error("Erreur retour dashboard", e);
            try { MainApp.showDashboard(); } catch (Exception ignored) {}
        }
    }

    // ── Recherche ────────────────────────────────────────────────────────────

    @FXML
    private void handleSearchTask() {
        if (currentBoard == null) {
            ToastNotification.show(MainApp.getPrimaryStage(), "Aucun board sélectionné");
            return;
        }

        Stage searchStage = new Stage();
        searchStage.setTitle("Rechercher une tâche");
        searchStage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(12);
        root.setStyle("-fx-padding: 20; -fx-background-color: #1e1e2e;");

        Label titleLbl = new Label("Recherche dans : " + currentBoard.get("title").getAsString());
        titleLbl.setStyle("-fx-text-fill: #cdd6f4; -fx-font-size: 13px;");

        TextField searchField = new TextField();
        searchField.setPromptText("Mot-clé...");
        searchField.setStyle("-fx-background-color: #313244; -fx-text-fill: #cdd6f4;" +
                " -fx-prompt-text-fill: #6c7086; -fx-padding: 8; -fx-background-radius: 6;");

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 12px;");

        VBox resultsList = new VBox(8);
        resultsList.setStyle("-fx-padding: 4 0 0 0;");
        ScrollPane scroll = new ScrollPane(resultsList);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(320);
        scroll.setStyle("-fx-background: #1e1e2e; -fx-background-color: #1e1e2e;" +
                " -fx-border-color: transparent;");

        // ✅ Recherche à chaque frappe dès 2 caractères
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal.trim();
            resultsList.getChildren().clear();

            if (query.length() < 2) {
                statusLabel.setText(query.isEmpty() ? "" : "Tapez au moins 2 caractères...");
                return;
            }

            statusLabel.setText("⏳ Recherche...");

            JsonObject data = new JsonObject();
            data.addProperty("query", query);
            data.addProperty("boardId", currentBoard.get("id").getAsInt());

            AsyncServer.sendRequest("SEARCH_TASKS", data,
                    response -> {
                        JsonArray results = response.getAsJsonArray("data");
                        resultsList.getChildren().clear();

                        if (results.size() == 0) {
                            statusLabel.setText("Aucun résultat pour \"" + query + "\"");
                            return;
                        }

                        statusLabel.setText(results.size() + " tâche(s) trouvée(s)");

                        for (JsonElement el : results) {
                            JsonObject t = el.getAsJsonObject();

                            HBox row = new HBox(10);
                            row.setStyle("-fx-background-color: #313244; -fx-padding: 10;" +
                                    " -fx-background-radius: 8; -fx-cursor: hand;");
                            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                            String pri = t.has("priority") ? t.get("priority").getAsString() : "MEDIUM";
                            Label badge = new Label(pri);
                            badge.getStyleClass().add("badge-" + pri.toLowerCase());

                            VBox info = new VBox(2);
                            Label tTitle = new Label(t.get("title").getAsString());
                            tTitle.setStyle("-fx-text-fill: #cdd6f4; -fx-font-size: 13px; -fx-font-weight: bold;");

                            String col = t.has("columnName") ? t.get("columnName").getAsString() : "";
                            Label colLabel = new Label(col.isEmpty() ? "" : "📌 " + col);
                            colLabel.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 11px;");

                            info.getChildren().addAll(tTitle, colLabel);
                            HBox.setHgrow(info, Priority.ALWAYS);
                            row.getChildren().addAll(badge, info);

                            row.setOnMouseClicked(e -> {
                                searchStage.close();
                                ToastNotification.show(MainApp.getPrimaryStage(),
                                        "📌 Tâche : " + t.get("title").getAsString());
                            });

                            resultsList.getChildren().add(row);
                        }
                    },
                    error -> statusLabel.setText("❌ Erreur: " + error)
            );
        });

        root.getChildren().addAll(titleLbl, searchField, statusLabel, scroll);

        Scene scene = new Scene(root, 460, 480);
        applyStyle(scene);
        searchStage.setScene(scene);
        searchStage.show();
        searchField.requestFocus();
    }

    // ── Utilitaires ──────────────────────────────────────────────────────────

    @FXML
    private void handleNewTask() {}

    @FXML
    private void handleScrollToColumn() {
        if (columnsContainer != null && !columnsContainer.getChildren().isEmpty())
            columnsContainer.getChildren().get(0).requestFocus();
    }

    private void updateCounter(Label counter, VBox tasksBox) {
        counter.setText(String.valueOf(tasksBox.getChildren().size()));
    }

    private void applyStyle(Scene scene) {
        String cssUrl = MainApp.class.getResource("/com/kanban/views/style.css").toExternalForm();
        if (cssUrl != null) scene.getStylesheets().add(cssUrl);
    }
}