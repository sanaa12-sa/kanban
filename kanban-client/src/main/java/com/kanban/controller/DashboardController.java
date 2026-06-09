package com.kanban.controller;

import com.google.gson.*;
import com.kanban.client.MainApp;
import com.kanban.client.ServerConnection;
import com.kanban.client.ToastNotification;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private FlowPane boardsContainer;
    @FXML private TextField searchField;
    @FXML private Label avatarLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label totalBoardsLabel;
    @FXML private Label totalTasksLabel;
    @FXML private Label membersLabel;
    @FXML private Label pageTitle;
    @FXML private Label pageSubtitle;

    private ScrollPane boardsScrollPane;

    // filtre actif
    private String activeFilterPriority = "TOUS";
    private String activeFilterBoard    = "TOUS";

    @FXML
    public void initialize() {
        loadUserInfo();
        loadBoards();
        if (searchField != null)
            searchField.textProperty().addListener((obs, o, n) -> filterBoards(n));
        if (boardsContainer != null && boardsContainer.getParent() instanceof ScrollPane sp)
            boardsScrollPane = sp;
    }

    // ── User info ────────────────────────────────────────────────────────────

    private void loadUserInfo() {
        JsonObject user = MainApp.getCurrentUser();
        if (user == null) return;
        String name  = user.has("fullName") ? user.get("fullName").getAsString() : "Utilisateur";
        String email = user.has("email")    ? user.get("email").getAsString()    : "";
        if (userNameLabel  != null) userNameLabel.setText(name);
        if (userEmailLabel != null) userEmailLabel.setText(email);
        if (avatarLabel    != null && !name.isEmpty())
            avatarLabel.setText(String.valueOf(name.charAt(0)).toUpperCase());
    }

    // ── Boards ───────────────────────────────────────────────────────────────

    private void loadBoards() {
        try {
            JsonObject response = ServerConnection.getInstance().sendRequest("GET_BOARDS", null);
            boardsContainer.getChildren().clear();
            if (response == null || !"OK".equals(response.get("status").getAsString())) {
                updateStats(0, 0, 0); showEmpty(); return;
            }
            JsonElement dataEl = response.get("data");
            if (dataEl == null || dataEl.isJsonNull()) { updateStats(0, 0, 0); showEmpty(); return; }
            JsonArray boards = dataEl.getAsJsonArray();
            if (boards.isEmpty()) { updateStats(0, 0, 0); showEmpty(); return; }

            List<JsonObject> boardList = new ArrayList<>();
            for (JsonElement el : boards)
                boardList.add(el.getAsJsonObject());

            // ✅ Compter tâches et membres depuis les boards
            int totalTasks   = 0;
            java.util.Set<Integer> uniqueMembers = new java.util.HashSet<>();

            for (JsonObject board : boardList) {
                // Tâches
                JsonObject tData = new JsonObject();
                tData.addProperty("boardId", board.get("id").getAsInt());
                JsonObject tResp = ServerConnection.getInstance().sendRequest("GET_TASKS", tData);
                if (tResp != null && "OK".equals(tResp.get("status").getAsString())) {
                    for (JsonElement cEl : tResp.getAsJsonArray("data")) {
                        JsonArray tasks = cEl.getAsJsonObject().getAsJsonArray("tasks");
                        totalTasks += tasks.size();
                        // Collecter les assignedTo pour compter les collaborateurs
                        for (JsonElement taskEl : tasks) {
                            JsonObject task = taskEl.getAsJsonObject();
                            if (task.has("assignedTo") && !task.get("assignedTo").isJsonNull()) {
                                int uid = task.get("assignedTo").getAsInt();
                                if (uid > 0) uniqueMembers.add(uid);
                            }
                        }
                    }
                }

                // Membres du board si le serveur les retourne
                if (board.has("memberCount") && !board.get("memberCount").isJsonNull()) {
                    // optionnel : si le serveur envoie déjà le compte
                }
            }

            String sortMode = SettingsController.currentBoardSort;
            switch (sortMode) {
                case "Nom alphabétique" ->
                        boardList.sort(Comparator.comparing(
                                b -> b.get("title").getAsString().toLowerCase()));
                case "Nombre de tâches" ->
                        boardList.sort(Comparator.comparingInt(
                                b -> -(b.has("taskCount") ? b.get("taskCount").getAsInt() : 0)));
                default -> { }
            }

            updateStats(boardList.size(), totalTasks, uniqueMembers.size());
            for (JsonObject board : boardList)
                boardsContainer.getChildren().add(createBoardCard(board));

        } catch (Exception e) {
            System.err.println("Erreur boards : " + e.getMessage());
            updateStats(0, 0, 0); showEmpty();
        }
    }

    private void updateStats(int boardCount, int taskCount, int memberCount) {
        if (totalBoardsLabel != null) totalBoardsLabel.setText(String.valueOf(boardCount));
        if (totalTasksLabel  != null) totalTasksLabel.setText(taskCount  > 0 ? String.valueOf(taskCount)  : "—");
        if (membersLabel     != null) membersLabel.setText(memberCount > 0 ? String.valueOf(memberCount) : "—");
    }

    private VBox createBoardCard(JsonObject board) {
        VBox card = new VBox(10);
        card.getStyleClass().add("board-card");
        card.setPrefWidth(250);

        String color = board.has("themeColor") && !board.get("themeColor").isJsonNull()
                ? board.get("themeColor").getAsString() : "#1B3A5C";
        Region colorBar = new Region();
        colorBar.setPrefHeight(6);
        colorBar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4 4 0 0;");

        Label title = new Label(board.get("title").getAsString());
        title.getStyleClass().add("board-card-title");

        String descText = board.has("description") && !board.get("description").isJsonNull()
                ? board.get("description").getAsString() : "Aucune description";
        Label desc = new Label(descText);
        desc.getStyleClass().add("board-card-description");
        desc.setWrapText(true);

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #C0392B;" +
                " -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 2 6;");
        deleteBtn.setOnAction(e -> { e.consume(); handleDeleteBoard(board, card); });
        footer.getChildren().add(deleteBtn);

        card.getChildren().addAll(colorBar, title, desc, footer);
        card.setOnMouseClicked(e -> { if (e.getTarget() != deleteBtn) openKanban(board); });
        return card;
    }

    private void handleDeleteBoard(JsonObject board, VBox card) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le board");
        confirm.setHeaderText("Supprimer \"" + board.get("title").getAsString() + "\" ?");
        confirm.setContentText("Toutes les tâches et colonnes seront supprimées.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    JsonObject data = new JsonObject();
                    data.addProperty("boardId", board.get("id").getAsInt());
                    JsonObject res = ServerConnection.getInstance().sendRequest("DELETE_BOARD", data);
                    if (res != null && "OK".equals(res.get("status").getAsString())) {
                        boardsContainer.getChildren().remove(card);
                        if (boardsContainer.getChildren().isEmpty()) showEmpty();
                        updateStats(boardsContainer.getChildren().size(), 0, 0);
                        ToastNotification.show(
                                (Stage) boardsContainer.getScene().getWindow(), "🗑️ Board supprimé");
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    private void openKanban(JsonObject board) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/com/kanban/views/kanban.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 700);
            scene.getStylesheets().add(
                    MainApp.class.getResource("/com/kanban/views/style.css").toExternalForm());
            KanbanController ctrl = loader.getController();
            ctrl.initBoard(board);
            ((Stage) boardsContainer.getScene().getWindow()).setScene(scene);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showEmpty() {
        boardsContainer.getChildren().clear();
        Label empty = new Label("Aucun board.\nCliquez sur « Nouveau board » pour commencer.");
        empty.setStyle("-fx-text-fill: #9B8E82; -fx-font-size: 14px;" +
                " -fx-text-alignment: center; -fx-padding: 40;");
        empty.setAlignment(Pos.CENTER);
        boardsContainer.getChildren().add(empty);
    }

    private void filterBoards(String query) {
        boardsContainer.getChildren().forEach(node -> {
            if (node instanceof VBox card) {
                boolean visible = card.getChildren().stream()
                        .filter(n -> n instanceof Label)
                        .map(n -> ((Label) n).getText().toLowerCase())
                        .anyMatch(t -> t.contains(query.toLowerCase()));
                card.setVisible(visible);
                card.setManaged(visible);
            }
        });
    }

    // ── Sidebar : Mes Boards ─────────────────────────────────────────────────

    @FXML
    private void handleShowBoards() {
        if (pageTitle    != null) pageTitle.setText("Mes Boards");
        if (pageSubtitle != null) pageSubtitle.setText("Gérez et organisez vos projets");
        // Remettre le FlowPane normal des boards
        boardsContainer.getChildren().clear();
        showBoardsScrollPane();
        loadBoards();
    }

    // ── Sidebar : Toutes les tâches ──────────────────────────────────────────

    @FXML
    private void handleAllTasks() {
        if (pageTitle    != null) pageTitle.setText("Toutes les tâches");
        if (pageSubtitle != null) pageSubtitle.setText("Vue globale de vos tâches");
        activeFilterPriority = "TOUS";
        activeFilterBoard    = "TOUS";
        loadAllTasksFromServer();
    }

    private void loadAllTasksFromServer() {
        try {
            JsonObject boardsResp = ServerConnection.getInstance().sendRequest("GET_BOARDS", null);
            if (boardsResp == null || !"OK".equals(boardsResp.get("status").getAsString())) {
                showTasksError("Impossible de récupérer les boards"); return;
            }
            JsonArray boards = boardsResp.getAsJsonArray("data");
            if (boards == null || boards.isEmpty()) { showTasksEmptyState(); return; }

            List<TaskInfo> allTasks = new ArrayList<>();
            for (JsonElement bEl : boards) {
                JsonObject board = bEl.getAsJsonObject();
                String boardTitle = board.get("title").getAsString();
                JsonObject tData = new JsonObject();
                tData.addProperty("boardId", board.get("id").getAsInt());
                JsonObject tResp = ServerConnection.getInstance().sendRequest("GET_TASKS", tData);
                if (tResp == null || !"OK".equals(tResp.get("status").getAsString())) continue;
                for (JsonElement cEl : tResp.getAsJsonArray("data")) {
                    JsonObject col = cEl.getAsJsonObject();
                    String colName = col.get("name").getAsString();
                    for (JsonElement taskEl : col.getAsJsonArray("tasks")) {
                        JsonObject task = taskEl.getAsJsonObject();
                        TaskInfo info = new TaskInfo();
                        info.title     = task.get("title").getAsString();
                        info.boardName = boardTitle;
                        info.status    = colName;
                        info.priority  = task.has("priority") ? task.get("priority").getAsString() : "MEDIUM";
                        info.deadline  = task.has("deadline") && !task.get("deadline").isJsonNull()
                                ? task.get("deadline").getAsString() : "";
                        allTasks.add(info);
                    }
                }
            }

            if (allTasks.isEmpty()) { showTasksEmptyState(); return; }
            if (totalTasksLabel != null) totalTasksLabel.setText(String.valueOf(allTasks.size()));
            showTasksView(allTasks);

        } catch (Exception e) { e.printStackTrace(); showTasksError(e.getMessage()); }
    }

    private static class TaskInfo {
        String title, boardName, status, priority, deadline;
    }

    private void showTasksView(List<TaskInfo> allTasks) {
        // Vider le boardsContainer et y injecter directement la vue tâches
        boardsContainer.getChildren().clear();
        showBoardsScrollPane();

        VBox tasksRoot = new VBox(0);
        tasksRoot.setStyle("-fx-background-color: #F7F4EF;");
        // Faire en sorte que tasksRoot prenne toute la largeur du FlowPane parent
        tasksRoot.prefWidthProperty().bind(boardsContainer.widthProperty());

        // ── 1. Barre de filtres ──────────────────────────────────────────────
        HBox filterBar = new HBox(10);
        filterBar.setStyle("-fx-padding: 0 0 16 0; -fx-background-color: #F7F4EF;");
        filterBar.setAlignment(Pos.CENTER_LEFT);

        Label filterLbl = new Label("Filtrer :");
        filterLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B6B80; -fx-font-weight: bold;");
        filterBar.getChildren().add(filterLbl);

        // Boutons priorité
        String[] priorities = {"TOUS", "HIGH", "MEDIUM", "LOW"};
        ToggleGroup prioGroup = new ToggleGroup();
        for (String p : priorities) {
            ToggleButton btn = new ToggleButton(p.equals("TOUS") ? "Toutes" : p);
            btn.setToggleGroup(prioGroup);
            btn.setSelected(p.equals(activeFilterPriority));
            String[] colors = priorityColors(p);
            btn.setStyle(
                    "-fx-background-color: " + (p.equals(activeFilterPriority) ? "#1B3A5C" : colors[0]) + ";" +
                            "-fx-text-fill: " + (p.equals(activeFilterPriority) ? "#FFFFFF" : colors[1]) + ";" +
                            "-fx-font-size: 11px; -fx-font-weight: bold;" +
                            "-fx-background-radius: 20; -fx-padding: 4 14; -fx-cursor: hand; -fx-border-width: 0;");

            final String fp = p;
            VBox listRef = new VBox(0); // référence temporaire, sera remplacée
            btn.setOnAction(e -> {
                activeFilterPriority = fp;
                // Reconstruire la vue complète avec les nouveaux filtres
                showTasksView(allTasks);
            });
            filterBar.getChildren().add(btn);
        }

        // Séparateur
        Label sep = new Label("|");
        sep.setStyle("-fx-text-fill: #C8C0B5; -fx-padding: 0 4;");
        filterBar.getChildren().add(sep);

        // Filtre par board
        ComboBox<String> boardFilter = new ComboBox<>();
        boardFilter.getItems().add("Tous les boards");
        allTasks.stream().map(t -> t.boardName).distinct().forEach(boardFilter.getItems()::add);
        boardFilter.setValue(activeFilterBoard.equals("TOUS") ? "Tous les boards" : activeFilterBoard);
        boardFilter.setStyle("-fx-font-size: 11px; -fx-background-radius: 8;");
        boardFilter.setOnAction(e -> {
            String val = boardFilter.getValue();
            activeFilterBoard = val.equals("Tous les boards") ? "TOUS" : val;
            showTasksView(allTasks);
        });
        filterBar.getChildren().add(boardFilter);

        // Compteur à droite
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        long count = allTasks.stream()
                .filter(t -> activeFilterPriority.equals("TOUS") || t.priority.equals(activeFilterPriority))
                .filter(t -> activeFilterBoard.equals("TOUS") || t.boardName.equals(activeFilterBoard))
                .count();
        Label countLbl = new Label(count + " tâche" + (count > 1 ? "s" : ""));
        countLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #9B8E82;");
        filterBar.getChildren().addAll(spacer, countLbl);

        // ── 2. Tableau ───────────────────────────────────────────────────────
        VBox tableBox = new VBox(0);
        tableBox.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10;" +
                "-fx-border-color: #E8E2D9; -fx-border-radius: 10; -fx-border-width: 1;");

        // En-tête
        HBox header = new HBox();
        header.setStyle("-fx-padding: 10 16; -fx-background-color: #EDE9E3;" +
                "-fx-background-radius: 10 10 0 0;");
        String[] headers = {"Titre", "Board", "Statut", "Priorité", "Deadline"};
        double[] widths   = {0.32,    0.18,    0.18,     0.14,       0.18};
        for (int i = 0; i < headers.length; i++) {
            Label h = new Label(headers[i]);
            h.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #6B6B80;");
            HBox.setHgrow(h, Priority.ALWAYS);
            h.setMaxWidth(Double.MAX_VALUE);
            final double pct = widths[i];
            h.prefWidthProperty().bind(header.widthProperty().multiply(pct));
            header.getChildren().add(h);
        }
        tableBox.getChildren().add(header);

        // Lignes filtrées
        List<TaskInfo> filtered = allTasks.stream()
                .filter(t -> activeFilterPriority.equals("TOUS") || t.priority.equals(activeFilterPriority))
                .filter(t -> activeFilterBoard.equals("TOUS") || t.boardName.equals(activeFilterBoard))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Label empty = new Label("Aucune tâche pour ces filtres.");
            empty.setStyle("-fx-text-fill: #9B8E82; -fx-font-size: 13px; -fx-padding: 40;");
            empty.setAlignment(Pos.CENTER);
            tableBox.getChildren().add(empty);
        } else {
            for (int i = 0; i < filtered.size(); i++) {
                TaskInfo t = filtered.get(i);
                HBox row = new HBox();
                String rowBg = i % 2 == 0 ? "#FFFFFF" : "#FDFCFB";
                row.setStyle("-fx-padding: 10 16; -fx-background-color: " + rowBg + "; -fx-cursor: hand;");
                row.setAlignment(Pos.CENTER_LEFT);

                // Titre avec point coloré
                HBox titleCell = new HBox(8);
                titleCell.setAlignment(Pos.CENTER_LEFT);
                String[] pc = priorityColors(t.priority);
                Label dot = new Label("●");
                dot.setStyle("-fx-text-fill: " + pc[1] + "; -fx-font-size: 10px;");
                Label titleLbl = new Label(t.title);
                titleLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #1B3A5C;");
                titleCell.getChildren().addAll(dot, titleLbl);
                titleCell.prefWidthProperty().bind(row.widthProperty().multiply(0.32));
                HBox.setHgrow(titleCell, Priority.ALWAYS);

                // Board
                Label boardLbl = new Label(t.boardName);
                boardLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #4A4A6A;");
                boardLbl.prefWidthProperty().bind(row.widthProperty().multiply(0.18));

                // Statut
                Label statusLbl = new Label(t.status);
                statusLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #5A5A7A;" +
                        "-fx-background-color: #F0EDE8; -fx-background-radius: 6; -fx-padding: 2 8;");
                statusLbl.prefWidthProperty().bind(row.widthProperty().multiply(0.18));

                // Priorité badge
                Label prioLbl = new Label(t.priority);
                prioLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-background-color: " + pc[0] + "; -fx-text-fill: " + pc[1] + ";" +
                        "-fx-background-radius: 10; -fx-padding: 2 10;");
                HBox prioCell = new HBox(prioLbl);
                prioCell.setAlignment(Pos.CENTER_LEFT);
                prioCell.prefWidthProperty().bind(row.widthProperty().multiply(0.14));

                // Deadline
                boolean overdue = !t.deadline.isEmpty() && isDeadlineOverdue(t.deadline);
                String deadlineText = t.deadline.isEmpty() ? "—" : t.deadline;
                Label deadlineLbl = new Label(overdue ? "⚠ " + deadlineText : deadlineText);
                deadlineLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " +
                        (overdue ? "#C0392B" : "#6B6B80") + ";" +
                        (overdue ? "-fx-font-weight: bold;" : ""));
                deadlineLbl.prefWidthProperty().bind(row.widthProperty().multiply(0.18));

                // Hover
                final String bg = rowBg;
                row.setOnMouseEntered(e -> row.setStyle(
                        "-fx-padding: 10 16; -fx-background-color: #EEF3FA; -fx-cursor: hand;"));
                row.setOnMouseExited(e -> row.setStyle(
                        "-fx-padding: 10 16; -fx-background-color: " + bg + "; -fx-cursor: hand;"));

                row.getChildren().addAll(titleCell, boardLbl, statusLbl, prioCell, deadlineLbl);
                tableBox.getChildren().add(row);

                if (i < filtered.size() - 1) {
                    Separator divider = new Separator();
                    divider.setStyle("-fx-background-color: #EDE9E3;");
                    tableBox.getChildren().add(divider);
                }
            }
        }

        ScrollPane scroll = new ScrollPane(tableBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;" +
                "-fx-border-color: transparent;");
        scroll.setPrefHeight(500);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        tasksRoot.getChildren().addAll(filterBar, scroll);

        boardsContainer.getChildren().clear();
        boardsContainer.getChildren().add(tasksRoot);
    }

    private String[] priorityColors(String priority) {
        return switch (priority) {
            case "HIGH"   -> new String[]{"#FAE8EC", "#7B2D42"};
            case "LOW"    -> new String[]{"#E8F5EE", "#1A6B40"};
            case "MEDIUM" -> new String[]{"#FEF4E6", "#A0620A"};
            default       -> new String[]{"#F0EDE8", "#5A5A7A"};
        };
    }

    private boolean isDeadlineOverdue(String deadline) {
        try {
            return LocalDate.parse(deadline).isBefore(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    private void showTasksEmptyState() {
        showBoardsScrollPane();
        boardsContainer.getChildren().clear();
        Label empty = new Label("📋 Aucune tâche.\nCréez des tâches dans vos boards.");
        empty.setStyle("-fx-text-fill: #9B8E82; -fx-font-size: 14px;" +
                "-fx-text-alignment: center; -fx-padding: 60;");
        empty.setAlignment(Pos.CENTER);
        boardsContainer.getChildren().add(empty);
    }

    private void showTasksError(String error) {
        showBoardsScrollPane();
        boardsContainer.getChildren().clear();
        Label lbl = new Label("⚠️ Erreur : " + error);
        lbl.setStyle("-fx-text-fill: #7B2D42; -fx-font-size: 13px; -fx-padding: 40;");
        boardsContainer.getChildren().add(lbl);
    }

    // ── Sidebar : Notifications ──────────────────────────────────────────────

    @FXML
    private void handleNotifications() {
        if (pageTitle    != null) pageTitle.setText("Notifications");
        if (pageSubtitle != null) pageSubtitle.setText("Vos alertes et rappels");
        showBoardsScrollPane();
        boardsContainer.getChildren().clear();

        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10;" +
                "-fx-padding: 20; -fx-border-color: #E8E2D9; -fx-border-radius: 10; -fx-border-width: 1;");
        card.setPrefWidth(420);

        Label title = new Label("🔔 Centre de notifications");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1B3A5C;");

        String email = MainApp.getCurrentUser() != null
                ? MainApp.getCurrentUser().get("email").getAsString() : "—";
        Label info = new Label(
                "Les notifications sont envoyées automatiquement par email à :\n" + email + "\n\n" +
                        "• 📧 Email de bienvenue à l'inscription\n" +
                        "• 📧 Tâche assignée\n" +
                        "• 📧 Rappel 24h avant la deadline\n" +
                        "• 📧 Nouveau commentaire sur une tâche\n" +
                        "• 📧 Invitation à rejoindre un board");
        info.setStyle("-fx-font-size: 12px; -fx-text-fill: #4A4A6A;");
        info.setWrapText(true);

        card.getChildren().addAll(title, new Separator(), info);
        boardsContainer.getChildren().add(card);
    }

    // ── Sidebar : Paramètres ─────────────────────────────────────────────────

    @FXML
    private void handleSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/com/kanban/views/settings.fxml"));
            Scene scene = new Scene(loader.load(), 480, 620);
            scene.getStylesheets().add(
                    MainApp.class.getResource("/com/kanban/views/style.css").toExternalForm());
            Stage dialog = new Stage();
            dialog.setTitle("Paramètres");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(scene);
            dialog.showAndWait();
            loadBoards();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Sidebar : Profil ─────────────────────────────────────────────────────

    @FXML
    private void handleProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/com/kanban/views/profile.fxml"));
            Scene scene = new Scene(loader.load(), 520, 720);
            scene.getStylesheets().add(
                    MainApp.class.getResource("/com/kanban/views/style.css").toExternalForm());
            Stage dialog = new Stage();
            dialog.setTitle("Mon Profil");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(scene);
            dialog.showAndWait();
            loadUserInfo();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Nouveau board ────────────────────────────────────────────────────────

    @FXML
    private void handleNewBoard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource("/com/kanban/views/create_board.fxml"));
            Scene scene = new Scene(loader.load(), 500, 400);
            scene.getStylesheets().add(
                    MainApp.class.getResource("/com/kanban/views/style.css").toExternalForm());
            Stage dialog = new Stage();
            dialog.setTitle("Nouveau Board");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(scene);
            CreateBoardController ctrl = loader.getController();
            ctrl.setOnSuccess(() -> { dialog.close(); loadBoards(); });
            dialog.showAndWait();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Déconnexion ──────────────────────────────────────────────────────────

    @FXML
    private void handleLogout() {
        try { ServerConnection.getInstance().close(); } catch (Exception ignored) {}
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Utilitaires ──────────────────────────────────────────────────────────

    private void showBoardsScrollPane() {
        if (boardsScrollPane != null) {
            boardsScrollPane.setVisible(true);
            boardsScrollPane.setManaged(true);
        }
    }
}