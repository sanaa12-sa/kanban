package com.kanban.controller;

import com.google.gson.JsonObject;
import com.kanban.client.MainApp;
import com.kanban.client.ServerConnection;
import com.kanban.client.ToastNotification;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class SettingsController {

    // ── Thème ────────────────────────────────────────────────────────────────
    @FXML private ToggleGroup  themeGroup;
    @FXML private RadioButton  themeLight;
    @FXML private RadioButton  themeDark;

    // ── Tri des boards ───────────────────────────────────────────────────────
    @FXML private ComboBox<String> boardSortBox;

    // ── Notifications ────────────────────────────────────────────────────────
    @FXML private CheckBox     notifEmail;
    @FXML private CheckBox     notifDeadline;
    @FXML private CheckBox     notifInvite;

    // ── Délai deadline ───────────────────────────────────────────────────────
    @FXML private ComboBox<String> deadlineReminderBox;

    // ── Feedback ─────────────────────────────────────────────────────────────
    @FXML private Label savedLabel;

    // Clé statique pour partager le tri avec DashboardController
    public static String currentBoardSort = "Date de création";

    @FXML
    public void initialize() {

        // ── Tri des boards ───────────────────────────────────────────────────
        if (boardSortBox != null) {
            boardSortBox.getItems().addAll(
                    "Date de création",
                    "Nom alphabétique",
                    "Nombre de tâches");
            boardSortBox.setValue(currentBoardSort);
        }

        // ── Délai deadline ───────────────────────────────────────────────────
        if (deadlineReminderBox != null) {
            deadlineReminderBox.getItems().addAll(
                    "1 jour avant", "2 jours avant", "3 jours avant", "1 semaine avant");
        }

        // Charger les préférences actuelles
        JsonObject user = MainApp.getCurrentUser();

        // Thème
        String currentTheme = MainApp.getCurrentTheme();
        if ("dark".equals(currentTheme)) {
            if (themeDark  != null) themeDark.setSelected(true);
        } else {
            if (themeLight != null) themeLight.setSelected(true);
        }

        // Notifications
        if (user != null && user.has("notifEnabled")) {
            boolean notif = user.get("notifEnabled").getAsBoolean();
            if (notifEmail    != null) notifEmail.setSelected(notif);
            if (notifDeadline != null) notifDeadline.setSelected(notif);
            if (notifInvite   != null) notifInvite.setSelected(notif);
        } else {
            if (notifEmail    != null) notifEmail.setSelected(true);
            if (notifDeadline != null) notifDeadline.setSelected(true);
            if (notifInvite   != null) notifInvite.setSelected(true);
        }

        // Délai deadline
        if (user != null && user.has("deadlineDelay")) {
            int delay = user.get("deadlineDelay").getAsInt();
            String val = switch (delay) {
                case 2  -> "2 jours avant";
                case 3  -> "3 jours avant";
                case 7  -> "1 semaine avant";
                default -> "1 jour avant";
            };
            if (deadlineReminderBox != null) deadlineReminderBox.setValue(val);
        } else {
            if (deadlineReminderBox != null) deadlineReminderBox.setValue("1 jour avant");
        }
    }


    @FXML
    private void handleSave() {
        boolean anyError = false;

        // ── 1. Appliquer le thème immédiatement ──────────────────────────────
        if (themeGroup != null && themeGroup.getSelectedToggle() != null) {
            RadioButton selected = (RadioButton) themeGroup.getSelectedToggle();
            String theme = (selected == themeDark) ? "dark" : "light";
            MainApp.applyTheme(theme);
        }

        // ── 2. Sauvegarder le tri des boards ─────────────────────────────────
        if (boardSortBox != null && boardSortBox.getValue() != null) {
            currentBoardSort = boardSortBox.getValue();
        }

        // ── 3. Envoyer les notifications au serveur ──────────────────────────
        try {
            boolean notifEnabled = (notifEmail != null && notifEmail.isSelected())
                    || (notifDeadline != null && notifDeadline.isSelected());

            JsonObject notifData = new JsonObject();
            notifData.addProperty("notifEnabled", notifEnabled);
            ServerConnection.getInstance().sendRequest("UPDATE_NOTIF_SETTINGS", notifData);

            JsonObject user = MainApp.getCurrentUser();
            if (user != null) user.addProperty("notifEnabled", notifEnabled);

        } catch (Exception e) {
            System.err.println("Erreur mise à jour notifications : " + e.getMessage());
            anyError = true;
        }

        // ── 4. Envoyer le délai deadline au serveur ──────────────────────────
        try {
            String delayStr = (deadlineReminderBox != null)
                    ? deadlineReminderBox.getValue() : "1 jour avant";
            int delayDays = switch (delayStr) {
                case "2 jours avant"   -> 2;
                case "3 jours avant"   -> 3;
                case "1 semaine avant" -> 7;
                default                -> 1;
            };

            JsonObject delayData = new JsonObject();
            delayData.addProperty("deadlineDelay", delayDays);
            ServerConnection.getInstance().sendRequest("UPDATE_DEADLINE_DELAY", delayData);

            JsonObject user = MainApp.getCurrentUser();
            if (user != null) user.addProperty("deadlineDelay", delayDays);

        } catch (Exception e) {
            System.err.println("Erreur mise à jour délai deadline : " + e.getMessage());
            anyError = true;
        }

        // ── Feedback ─────────────────────────────────────────────────────────
        if (anyError) {
            savedLabel.setStyle("-fx-text-fill: #7B2D42;");
            savedLabel.setText("⚠️ Certains paramètres n'ont pas pu être sauvegardés");
        } else {
            savedLabel.setStyle("-fx-text-fill: #1a7a4a;");
            savedLabel.setText("✅ Paramètres appliqués et sauvegardés !");
            ToastNotification.show(
                    (Stage) savedLabel.getScene().getWindow(),
                    "⚙️ Paramètres appliqués !");
        }
    }

    @FXML
    private void handleClose() {
        ((Stage) savedLabel.getScene().getWindow()).close();
    }
}