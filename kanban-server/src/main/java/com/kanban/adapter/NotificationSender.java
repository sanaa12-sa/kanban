package com.kanban.adapter;

/**
 * Design Pattern Adapter — Interface cible.
 *
 * Interface générique d'envoi de notifications.
 * Permet de brancher n'importe quel système de notification
 * (email, SMS, push, Slack…) sans modifier le code métier.
 */
public interface NotificationSender {

    /**
     * Envoie une notification de bienvenue.
     * @param to       destinataire (email, numéro, userId…)
     * @param fullName nom affiché dans le message
     */
    void sendWelcome(String to, String fullName);

    /**
     * Envoie une notification d'invitation à un board.
     */
    void sendInvitation(String to, String boardName, String invitedBy);

    /**
     * Envoie une notification d'assignation de tâche.
     */
    void sendTaskAssigned(String to, String taskTitle, String description,
                          String priority, String deadline,
                          String boardName, String assignedBy);

    /**
     * Envoie une notification de tâche terminée.
     */
    void sendTaskCompleted(String to, String taskTitle,
                           String boardName, String completedBy);

    /**
     * Envoie une notification de nouveau commentaire.
     */
    void sendNewComment(String to, String taskTitle,
                        String commentContent, String commentedBy);

    /**
     * Envoie un rappel de deadline (deadline = demain).
     */
    void sendDeadlineReminder(String to, String taskTitle,
                              String deadline, String boardName);

    /**
     * Envoie une notification de suppression de board.
     */
    void sendBoardDeleted(String to, String boardName, String deletedBy);
}