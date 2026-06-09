package com.kanban.controller;

import com.google.gson.JsonObject;
import com.kanban.client.MainApp;
import com.kanban.client.ServerConnection;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ProfileController {

    @FXML private Label         avatarBigLabel;
    @FXML private Label         profileNameLabel;
    @FXML private Label         profileEmailLabel;
    @FXML private TextField     fullNameField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         messageLabel;

    @FXML
    public void initialize() {
        JsonObject user = MainApp.getCurrentUser();
        if (user == null) return;

        String name  = user.has("fullName") ? user.get("fullName").getAsString() : "";
        String email = user.has("email")    ? user.get("email").getAsString()    : "";

        profileNameLabel.setText(name);
        profileEmailLabel.setText(email);
        if (!name.isEmpty())
            avatarBigLabel.setText(String.valueOf(name.charAt(0)).toUpperCase());

        fullNameField.setText(name);
        emailField.setText(email);
    }

    @FXML
    private void handleSave() {
        String newName     = fullNameField.getText().trim();
        String newPassword = passwordField.getText();

        if (newName.isEmpty()) {
            showMessage("⚠ Le nom ne peut pas être vide.", false);
            return;
        }

        try {
            // Construire la requête vers le serveur
            JsonObject data = new JsonObject();
            data.addProperty("fullName", newName);

            // N'envoyer le mot de passe que s'il est rempli
            if (!newPassword.isEmpty()) {
                data.addProperty("password", newPassword);
            }

            JsonObject response = ServerConnection.getInstance().sendRequest("UPDATE_PROFILE", data);

            if (response != null && "OK".equals(response.get("status").getAsString())) {
                // Mettre à jour le currentUser localement seulement si le serveur confirme
                JsonObject user = MainApp.getCurrentUser();
                if (user != null) {
                    user.addProperty("fullName", newName);
                    MainApp.setCurrentUser(user);
                }

                showMessage("✅ Profil mis à jour avec succès !", true);

                // Fermer après 1 seconde
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        javafx.application.Platform.runLater(() ->
                                fullNameField.getScene().getWindow().hide());
                    } catch (InterruptedException ignored) {}
                }).start();

            } else {
                String errorMsg = (response != null && response.has("message"))
                        ? response.get("message").getAsString()
                        : "Erreur inconnue";
                showMessage("❌ Erreur : " + errorMsg, false);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("❌ Connexion au serveur impossible.", false);
        }
    }

    @FXML
    private void handleCancel() {
        fullNameField.getScene().getWindow().hide();
    }

    private void showMessage(String msg, boolean success) {
        messageLabel.setText(msg);
        messageLabel.setStyle(success
                ? "-fx-text-fill: #1a7a4a; -fx-font-size: 12px; -fx-font-weight: bold;"
                : "-fx-text-fill: #7B2D42; -fx-font-size: 12px; -fx-font-weight: bold;");
    }
}