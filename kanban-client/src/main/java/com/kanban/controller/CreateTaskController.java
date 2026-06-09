package com.kanban.controller;

import com.google.gson.JsonObject;
import com.kanban.client.ServerConnection;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.function.Consumer;

public class CreateTaskController {

    @FXML private TextField        titleField;
    @FXML private TextArea         descriptionField;
    @FXML private ComboBox<String> priorityBox;
    @FXML private TextField        deadlineField;
    @FXML private TextField        assignedToField;
    @FXML private Label            errorLabel;

    private int                    columnId;
    private Consumer<JsonObject>   onSuccess;

    public void setColumnId(int columnId)                   { this.columnId = columnId; }
    public void setOnSuccess(Consumer<JsonObject> callback) { this.onSuccess = callback; }

    @FXML
    public void initialize() {
        priorityBox.getItems().addAll("HIGH", "MEDIUM", "LOW");
        priorityBox.setValue("MEDIUM");

        // Placeholder deadline
        deadlineField.setPromptText("ex: 2026-06-01");
    }

    @FXML
    private void handleCreate() {
        String title    = titleField.getText().trim();
        String deadline = deadlineField.getText().trim();

        // Validations
        if (title.isEmpty()) {
            errorLabel.setText("Le titre est obligatoire");
            return;
        }
        if (!deadline.isEmpty() && !deadline.matches("\\d{4}-\\d{2}-\\d{2}")) {
            errorLabel.setText("Format deadline invalide (YYYY-MM-DD)");
            return;
        }

        // Validation assignedTo
        int assignedTo = 0;
        if (!assignedToField.getText().trim().isEmpty()) {
            try {
                assignedTo = Integer.parseInt(assignedToField.getText().trim());
            } catch (NumberFormatException ex) {
                errorLabel.setText("L'ID assigné doit être un nombre");
                return;
            }
        }

        JsonObject data = new JsonObject();
        data.addProperty("columnId",    columnId);
        data.addProperty("title",       title);
        data.addProperty("description", descriptionField.getText().trim());
        data.addProperty("priority",    priorityBox.getValue());
        data.addProperty("deadline",    deadline);
        data.addProperty("assignedTo",  assignedTo);

        try {
            JsonObject response = ServerConnection.getInstance()
                    .sendRequest("CREATE_TASK", data);

            if (response != null && "OK".equals(response.get("status").getAsString())) {
                JsonObject task = response.getAsJsonObject("data");
                if (onSuccess != null) onSuccess.accept(task);
            } else {
                errorLabel.setText(response != null
                        ? response.get("message").getAsString() : "Erreur serveur");
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