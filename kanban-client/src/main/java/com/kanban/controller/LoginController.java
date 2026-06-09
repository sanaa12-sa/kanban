package com.kanban.controller;

import com.google.gson.JsonObject;
import com.kanban.client.MainApp;
import com.kanban.client.ServerConnection;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    @FXML
    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        try {
            JsonObject data = new JsonObject();
            data.addProperty("email",    email);
            data.addProperty("password", password);
            MainApp.setCredentials(email, password);

            JsonObject response = ServerConnection.getInstance().sendRequest("LOGIN", data);

            if (response != null && "OK".equals(response.get("status").getAsString())) {

                // ── Stocker l'utilisateur connecté pour le dashboard ──
                JsonObject userData = response.has("data")
                        ? response.getAsJsonObject("data")
                        : null;
                MainApp.setCurrentUser(userData);

                errorLabel.setStyle("-fx-text-fill: #1a7a4a; -fx-font-size: 12px;");
                errorLabel.setText("Connexion réussie !");
                MainApp.showDashboard();

            } else {
                showError(response != null
                        ? response.get("message").getAsString()
                        : "Erreur de connexion");
            }

        } catch (Exception e) {
            showError("Impossible de contacter le serveur");
        }
    }

    @FXML
    private void handleRegister() {
        try {
            MainApp.showRegister();
        } catch (Exception e) {
            showError("Impossible d'ouvrir la page d'inscription");
        }
    }

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill: #7B2D42; -fx-font-size: 12px; -fx-font-weight: bold;");
        errorLabel.setText(msg);
    }

}
