package com.kanban.email;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmailNotificationService {

    // ⚠️ Remplace par ton Gmail et mot de passe d'application
    private static final String FROM_EMAIL = "fanirisanaa@gmail.com";
    private static final String APP_PASSWORD = "ewxamgfszarwilyp";

    // Thread séparé pour ne pas bloquer le serveur
    private static final ExecutorService executor = Executors.newFixedThreadPool(3);

    private static Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });
    }

    // Méthode générique d'envoi (asynchrone)
    private static void sendEmail(String toEmail, String subject, String htmlBody) {
        executor.submit(() -> {
            try {
                Session session = createSession();
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(FROM_EMAIL, "Kanban App"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                message.setSubject(subject);
                message.setContent(htmlBody, "text/html; charset=utf-8");
                Transport.send(message);
                System.out.println("✅ Email envoyé à : " + toEmail);
            } catch (Exception e) {
                System.err.println("❌ Erreur envoi email : " + e.getMessage());
            }
        });
    }

    // ── 1. Email de bienvenue ──
    public static void sendWelcomeEmail(String toEmail, String fullName) {
        String subject = "Bienvenue sur Kanban App !";
        String body = EmailTemplates.welcome(fullName);
        sendEmail(toEmail, subject, body);
    }

    // ── 2. Email d'invitation à un board ──
    public static void sendInvitationEmail(String toEmail, String boardName, String invitedBy) {
        String subject = "Vous avez été invité au board : " + boardName;
        String body = EmailTemplates.invitation(toEmail, boardName, invitedBy);
        sendEmail(toEmail, subject, body);
    }

    // ── 3. Email d'assignation à une tâche ──
    public static void sendTaskAssignedEmail(String toEmail, String taskTitle,
                                             String description, String priority, String deadline, String boardName, String assignedBy) {
        String subject = "Nouvelle tâche assignée : " + taskTitle;
        String body = EmailTemplates.taskAssigned(taskTitle, description, priority, deadline, boardName, assignedBy);
        sendEmail(toEmail, subject, body);
    }

    // ── 4. Email de tâche terminée ──
    public static void sendTaskCompletedEmail(String toEmail, String taskTitle,
                                              String boardName, String completedBy) {
        String subject = "Tâche terminée : " + taskTitle;
        String body = EmailTemplates.taskCompleted(taskTitle, boardName, completedBy);
        sendEmail(toEmail, subject, body);
    }

    // ── 5. Email de nouveau commentaire ──
    public static void sendNewCommentEmail(String toEmail, String taskTitle,
                                           String commentContent, String commentedBy) {
        String subject = "Nouveau commentaire sur : " + taskTitle;
        String body = EmailTemplates.newComment(taskTitle, commentContent, commentedBy);
        sendEmail(toEmail, subject, body);
    }

    // ── 6. Email de rappel deadline ──
    public static void sendDeadlineReminderEmail(String toEmail, String taskTitle,
                                                 String deadline, String boardName) {
        String subject = "⏰ Rappel : deadline demain pour " + taskTitle;
        String body = EmailTemplates.deadlineReminder(taskTitle, deadline, boardName);
        sendEmail(toEmail, subject, body);
    }

    // ── 7. Email suppression board ──
    public static void sendBoardDeletedEmail(String toEmail, String boardName, String deletedBy) {
        String subject = "Board supprimé : " + boardName;
        String body = EmailTemplates.boardDeleted(boardName, deletedBy);
        sendEmail(toEmail, subject, body);
    }

    public static void shutdown() {
        executor.shutdown();
    }
}