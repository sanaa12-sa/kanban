package com.kanban.controller;

import com.kanban.client.*;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

public class WelcomeController {

    @FXML private ScrollPane mainScrollPane;
    @FXML private VBox       featuresSection;
    @FXML private VBox       aboutSection;

    @FXML
    private void handleLogin() {
        try {
            MainApp.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegister() {
        try {
            MainApp.showRegister();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Scroll vers la section Fonctionnalités ───────────────────────────
    @FXML
    private void handleScrollToFeatures() {
        if (mainScrollPane != null && featuresSection != null) {
            // Calculer la position Y de la section dans le ScrollPane
            double contentHeight = mainScrollPane.getContent().getBoundsInLocal().getHeight();
            double nodeMinY      = featuresSection.getBoundsInParent().getMinY();
            double scrollValue   = nodeMinY / contentHeight;
            mainScrollPane.setVvalue(scrollValue);
        }
    }

    // ── Scroll vers la section À propos ─────────────────────────────────
    @FXML
    private void handleScrollToAbout() {
        if (mainScrollPane != null && aboutSection != null) {
            double contentHeight = mainScrollPane.getContent().getBoundsInLocal().getHeight();
            double nodeMinY      = aboutSection.getBoundsInParent().getMinY();
            double scrollValue   = nodeMinY / contentHeight;
            mainScrollPane.setVvalue(scrollValue);
        }
    }
}