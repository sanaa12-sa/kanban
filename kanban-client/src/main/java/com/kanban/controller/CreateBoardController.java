package com.kanban.controller;

import com.google.gson.JsonObject;
import com.kanban.client.ServerConnection;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class CreateBoardController {

    @FXML private TextField titleField;
    @FXML private TextField descriptionField;
    @FXML private TextField colorField;
    @FXML private ComboBox<String> visibilityBox;
    @FXML private Label errorLabel;

    private Runnable onSuccess;

    public void setOnSuccess(Runnable callback) {
        this.onSuccess = callback;
    }

    @FXML
    public void initialize() {
        visibilityBox.getItems().addAll("PRIVATE", "PUBLIC");
        visibilityBox.setValue("PRIVATE");
        colorField.setText("#89b4fa");
    }

    @FXML
    private void handleCreate() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            errorLabel.setText("Le titre est obligatoire");
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("title", title);
        data.addProperty("description", descriptionField.getText().trim());
        data.addProperty("themeColor", colorField.getText().trim());
        data.addProperty("visibility", visibilityBox.getValue());

        try {
            JsonObject response = ServerConnection.getInstance().sendRequest("CREATE_BOARD", data);
            if (response != null && "OK".equals(response.get("status").getAsString())) {
                if (onSuccess != null) onSuccess.run();
            } else {
                errorLabel.setText(response != null ? response.get("message").getAsString() : "Erreur");
            }
        } catch (Exception e) {
            errorLabel.setText("Impossible de contacter le serveur");
        }
    }

    @FXML
    private void handleCancel() {
        titleField.getScene().getWindow().hide();
    }
}