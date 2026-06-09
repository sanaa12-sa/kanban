package com.kanban.client;

import com.google.gson.JsonObject;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class MainApp extends Application {

    private static Stage      primaryStage;
    private static JsonObject currentUser;

    // ── Préférences thème ─────────────────────────────────────────────────────
    private static String currentTheme    = "light";   // "light" ou "dark"
    private static String currentFontSize = "normal";  // "small", "normal", "large"

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;



        primaryStage.setTitle("Kanban App");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        showWelcome();
        primaryStage.show();
    }

    // ── Gestion utilisateur connecté ─────────────────────────────────────────

    public static JsonObject getCurrentUser() { return currentUser; }
    public static void setCurrentUser(JsonObject user) { currentUser = user; }

    // ── Gestion thème ────────────────────────────────────────────────────────

    public static String getCurrentTheme() { return currentTheme; }

    /**
     * Applique le thème clair ou sombre à la scène courante ET à toutes les
     * futures scènes (via primaryStage).
     * @param theme "light" ou "dark"
     */
    public static void applyTheme(String theme) {

        currentTheme = theme;

        Scene scene = primaryStage.getScene();

        if (scene == null)
            return;

        scene.getStylesheets()
                .removeIf(css -> css.contains("dark-theme.css"));

        if ("dark".equals(theme)) {

            var url =
                    MainApp.class.getResource(
                            "/com/kanban/views/dark-theme.css");

            if (url != null) {
                scene.getStylesheets().add(url.toExternalForm());
            }
        }
    }

    /**
     * Applique la taille de police à la scène courante.
     * @param size "small", "normal", "large"
     */
    public static void applyFontSize(String size) {
        currentFontSize = size;
        Scene scene = primaryStage.getScene();
        if (scene == null) return;

        double fontSize = switch (size) {
            case "small"  -> 11.0;
            case "large"  -> 15.0;
            default       -> 13.0;
        };
        // Appliquer via style inline sur la scène root
        scene.getRoot().setStyle("-fx-font-size: " + fontSize + "px;");
    }

    /** Réapplique le thème et la taille de police après chaque changement de scène */
    private static void applyCurrentPreferences(Scene scene) {
        // Thème sombre
        if ("dark".equals(currentTheme)) {
            String darkCss = MainApp.class
                    .getResource("/com/kanban/views/dark-theme.css")
                    .toExternalForm();
            if (!scene.getStylesheets().contains(darkCss)) {
                scene.getStylesheets().add(darkCss);
            }
        }
        // Taille de police
        double fontSize = switch (currentFontSize) {
            case "small"  -> 11.0;
            case "large"  -> 15.0;
            default       -> 13.0;
        };
        scene.getRoot().setStyle("-fx-font-size: " + fontSize + "px;");
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    public static void showWelcome() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/kanban/views/welcome.fxml"));
        Scene scene = new Scene(loader.load(), 900, 600);
        scene.getStylesheets().add(
                MainApp.class.getResource("/com/kanban/views/style.css").toExternalForm());
        primaryStage.setScene(scene);
        applyCurrentPreferences(scene);
    }

    public static void showLogin() throws Exception {
        currentUser = null;
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/kanban/views/login.fxml"));
        Scene scene = new Scene(loader.load(), 800, 600);
        scene.getStylesheets().add(
                MainApp.class.getResource("/com/kanban/views/style.css").toExternalForm());
        primaryStage.setScene(scene);
        applyCurrentPreferences(scene);
    }

    public static void showRegister() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/kanban/views/register.fxml"));
        Scene scene = new Scene(loader.load(), 800, 600);
        scene.getStylesheets().add(
                MainApp.class.getResource("/com/kanban/views/style.css").toExternalForm());
        primaryStage.setScene(scene);
        applyCurrentPreferences(scene);
    }

    public static void showDashboard() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                MainApp.class.getResource("/com/kanban/views/dashboard.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 700);
        scene.getStylesheets().add(
                MainApp.class.getResource("/com/kanban/views/style.css").toExternalForm());
        primaryStage.setScene(scene);
        applyCurrentPreferences(scene);
    }

    public static Stage getPrimaryStage() { return primaryStage; }
    // Ajoute ces champs et méthodes dans MainApp
    private static String currentEmail;
    private static String currentPassword;

    public static void setCredentials(String email, String password) {
        currentEmail = email;
        currentPassword = password;
    }

    public static String getCurrentEmail()    { return currentEmail; }
    public static String getCurrentPassword() { return currentPassword; }

    public static void main(String[] args) { launch(args); }
}