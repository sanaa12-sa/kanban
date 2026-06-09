package com.kanban.controller;

import com.google.gson.JsonObject;
import com.kanban.client.MainApp;
import com.kanban.client.ServerConnection;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML private TextField     fullNameField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Label         emailValidationLabel;
    @FXML private Label         passwordStrengthLabel;

    @FXML
    public void initialize() {

        // ── Validation email en temps réel ──────────────────────────────
        emailField.textProperty().addListener((obs, old, newVal) -> {
            if (newVal.isEmpty()) {
                emailValidationLabel.setText("");
                return;
            }
            if (!isValidEmail(newVal)) {
                emailValidationLabel.setText("⚠ Format invalide (ex: nom@domaine.com)");
                emailValidationLabel.setStyle("-fx-text-fill: #7B2D42; -fx-font-size: 11px;");
            } else {
                emailValidationLabel.setText("✓ Email valide");
                emailValidationLabel.setStyle("-fx-text-fill: #1a7a4a; -fx-font-size: 11px;");
            }
        });

        // ── Indicateur force mot de passe ───────────────────────────────
        passwordField.textProperty().addListener((obs, old, newVal) ->
                updatePasswordStrength(newVal));
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");
    }

    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            passwordStrengthLabel.setText("Entrez un mot de passe");
            passwordStrengthLabel.setStyle("-fx-text-fill: #6B6B80; -fx-font-size: 11px;");
            return;
        }
        int score = 0;
        if (password.length() >= 8)                          score++;
        if (password.matches(".*[A-Z].*"))                   score++;
        if (password.matches(".*[0-9].*"))                   score++;
        if (password.matches(".*[!@#$%^&*()_+\\-=].*"))     score++;

        switch (score) {
            case 0, 1 -> { passwordStrengthLabel.setText("Trop faible");
                passwordStrengthLabel.setStyle("-fx-text-fill: #7B2D42; -fx-font-size: 11px;"); }
            case 2    -> { passwordStrengthLabel.setText("Moyen");
                passwordStrengthLabel.setStyle("-fx-text-fill: #A0620A; -fx-font-size: 11px;"); }
            case 3    -> { passwordStrengthLabel.setText("Fort ✓");
                passwordStrengthLabel.setStyle("-fx-text-fill: #1a6b40; -fx-font-size: 11px;"); }
            case 4    -> { passwordStrengthLabel.setText("Très fort ✓✓");
                passwordStrengthLabel.setStyle("-fx-text-fill: #1a7a4a; -fx-font-size: 11px;" +
                        " -fx-font-weight: bold;"); }
        }
    }

    @FXML
    private void handleRegister() {
        errorLabel.setText("");

        String fullName = fullNameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        // ── Validations client ───────────────────────────────────────────
        if (fullName.isEmpty()) {
            showError("⚠ Le nom complet est obligatoire."); return;
        }
        if (email.isEmpty()) {
            showError("⚠ L'adresse email est obligatoire."); return;
        }
        if (!isValidEmail(email)) {
            showError("⚠ Format email invalide. Exemple : nom@domaine.com"); return;
        }
        if (password.length() < 8) {
            showError("⚠ Le mot de passe doit contenir au moins 8 caractères."); return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("fullName", fullName);
        data.addProperty("email",    email);
        data.addProperty("password", password);

        try {
            JsonObject response = ServerConnection.getInstance().sendRequest("REGISTER", data);

            if (response != null && "OK".equals(response.get("status").getAsString())) {

                // ── Stocker l'utilisateur après inscription ──
                JsonObject userData = response.has("data")
                        ? response.getAsJsonObject("data")
                        : null;
                MainApp.setCurrentUser(userData);

                MainApp.showDashboard();

            } else {
                String serverMsg = response != null
                        ? response.get("message").getAsString()
                        : "Erreur de connexion au serveur";

                if (serverMsg.toLowerCase().contains("email") &&
                        serverMsg.toLowerCase().contains("utilisé")) {
                    showError("⚠ Un compte existe déjà avec cet email.\nConnectez-vous ou utilisez une autre adresse.");
                } else {
                    showError("⚠ " + serverMsg);
                }
            }
        } catch (Exception e) {
            showError("⚠ Impossible de contacter le serveur.");
        }
    }

    @FXML
    private void handleBack() {
        try {
            MainApp.showLogin();
        } catch (Exception e) {
            showError("Impossible de revenir à la connexion");
        }
    }

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill: #7B2D42; -fx-font-size: 12px;" +
                " -fx-font-weight: bold; -fx-wrap-text: true;");
        errorLabel.setText(msg);
    }
}
