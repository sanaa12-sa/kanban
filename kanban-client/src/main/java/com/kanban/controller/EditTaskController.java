package com.kanban.controller;

import com.google.gson.JsonObject;
import com.kanban.client.ServerConnection;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.function.Consumer;

public class EditTaskController {

    @FXML private TextField        titleField;
    @FXML private TextArea         descriptionField;
    @FXML private ComboBox<String> priorityBox;
    @FXML private TextField        deadlineField;
    @FXML private TextField        assignedToField;
    @FXML private Label            errorLabel;

    private JsonObject             task;
    private Consumer<JsonObject>   onSuccess;

    public void setOnSuccess(Consumer<JsonObject> callback) { this.onSuccess = callback; }

    /**
     * Pré-remplit le formulaire avec les données de la tâche existante.
     */
    public void setTask(JsonObject task) {
        this.task = task;
        // Pré-remplissage après que initialize() a été appelé
        titleField.setText(task.get("title").getAsString());

        if (task.has("description") && !task.get("description").isJsonNull())
            descriptionField.setText(task.get("description").getAsString());

        if (task.has("priority") && !task.get("priority").isJsonNull())
            priorityBox.setValue(task.get("priority").getAsString());

        if (task.has("deadline") && !task.get("deadline").isJsonNull())
            deadlineField.setText(task.get("deadline").getAsString());

        if (task.has("assignedTo") && task.get("assignedTo").getAsInt() > 0)
            assignedToField.setText(String.valueOf(task.get("assignedTo").getAsInt()));
    }

    @FXML
    public void initialize() {
        priorityBox.getItems().addAll("HIGH", "MEDIUM", "LOW");
        priorityBox.setValue("MEDIUM");
        deadlineField.setPromptText("ex: 2026-06-01");
    }

    @FXML
    private void handleSave() {
        String title    = titleField.getText().trim();
        String deadline = deadlineField.getText().trim();

        if (title.isEmpty()) {
            errorLabel.setText("Le titre est obligatoire");
            return;
        }
        if (!deadline.isEmpty() && !deadline.matches("\\d{4}-\\d{2}-\\d{2}")) {
            errorLabel.setText("Format deadline invalide (YYYY-MM-DD)");
            return;
        }

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
        data.addProperty("taskId",      task.get("id").getAsInt());
        data.addProperty("title",       title);
        data.addProperty("description", descriptionField.getText().trim());
        data.addProperty("priority",    priorityBox.getValue());
        data.addProperty("deadline",    deadline);
        data.addProperty("assignedTo",  assignedTo);

        try {
            JsonObject response = ServerConnection.getInstance()
                    .sendRequest("UPDATE_TASK", data);

            if (response != null && "OK".equals(response.get("status").getAsString())) {
                if (onSuccess != null) onSuccess.accept(response.getAsJsonObject("data"));
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