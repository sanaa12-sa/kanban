package com.kanban.client;

import javafx.animation.*;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ToastNotification {

    public static void show(Stage stage, String message) {
        // Créer le popup
        Popup popup = new Popup();

        Label label = new Label(message);
        label.setStyle(
                "-fx-background-color: #313244;" +
                        "-fx-text-fill: #cdd6f4;" +
                        "-fx-padding: 12px 20px;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-border-color: #89b4fa;" +
                        "-fx-border-radius: 8px;" +
                        "-fx-border-width: 1px;" +
                        "-fx-font-size: 13px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 4);"
        );

        StackPane container = new StackPane(label);
        popup.getContent().add(container);
        popup.setAutoFix(true);

        // Position en bas à droite
        double x = stage.getX() + stage.getWidth() - 320;
        double y = stage.getY() + stage.getHeight() - 80;
        popup.show(stage, x, y);

        // Animation : apparition
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), container);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        // Animation : disparition après 3 secondes
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), container);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.seconds(3));
        fadeOut.setOnFinished(e -> popup.hide());

        // Lancer les animations
        SequentialTransition seq = new SequentialTransition(fadeIn, fadeOut);
        seq.play();
    }
}