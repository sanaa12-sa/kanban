package com.kanban.adapter;

import com.kanban.email.EmailNotificationService;

/**
 * Design Pattern Adapter — Adaptateur concret.
 *
 * Adapte l'interface EmailNotificationService (existante, incompatible)
 * vers l'interface cible NotificationSender.
 *
 * Le code métier (services) utilise NotificationSender uniquement.
 * Si demain on veut passer à SMS ou Slack, on crée un autre Adapter
 * sans toucher aux services.
 *
 *  Adaptee  : EmailNotificationService  (classe existante)
 *  Target   : NotificationSender        (interface souhaitée)
 *  Adapter  : EmailNotificationAdapter  (cette classe)
 */
public class EmailNotificationAdapter implements NotificationSender {

    // L'adaptee : le service email déjà codé
    private final EmailNotificationService emailService;

    public EmailNotificationAdapter() {
        this.emailService = new EmailNotificationService();
    }

    @Override
    public void sendWelcome(String to, String fullName) {
        EmailNotificationService.sendWelcomeEmail(to, fullName);
    }

    @Override
    public void sendInvitation(String to, String boardName, String invitedBy) {
        EmailNotificationService.sendInvitationEmail(to, boardName, invitedBy);
    }

    @Override
    public void sendTaskAssigned(String to, String taskTitle, String description,
                                 String priority, String deadline,
                                 String boardName, String assignedBy) {
        EmailNotificationService.sendTaskAssignedEmail(
                to, taskTitle, description, priority, deadline, boardName, assignedBy);
    }

    @Override
    public void sendTaskCompleted(String to, String taskTitle,
                                  String boardName, String completedBy) {
        EmailNotificationService.sendTaskCompletedEmail(to, taskTitle, boardName, completedBy);
    }

    @Override
    public void sendNewComment(String to, String taskTitle,
                               String commentContent, String commentedBy) {
        EmailNotificationService.sendNewCommentEmail(to, taskTitle, commentContent, commentedBy);
    }

    @Override
    public void sendDeadlineReminder(String to, String taskTitle,
                                     String deadline, String boardName) {
        EmailNotificationService.sendDeadlineReminderEmail(to, taskTitle, deadline, boardName);
    }

    @Override
    public void sendBoardDeleted(String to, String boardName, String deletedBy) {
        EmailNotificationService.sendBoardDeletedEmail(to, boardName, deletedBy);
    }
}