package com.kanban.email;

public class EmailTemplates {

    private static String wrapper(String content) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { margin: 0; padding: 0; background-color: #eff1f5; font-family: Arial, sans-serif; }
                .container { max-width: 600px; margin: 40px auto; background: #1e1e2e; border-radius: 12px; overflow: hidden; }
                .header { background: #89b4fa; padding: 30px; text-align: center; }
                .header h1 { margin: 0; color: #1e1e2e; font-size: 24px; }
                .body { padding: 30px; color: #cdd6f4; }
                .body h2 { color: #cba6f7; margin-top: 0; }
                .info-box { background: #313244; border-radius: 8px; padding: 16px; margin: 16px 0; }
                .info-row { display: flex; margin: 8px 0; }
                .info-label { color: #a6adc8; width: 120px; font-size: 13px; }
                .info-value { color: #cdd6f4; font-size: 13px; font-weight: bold; }
                .badge-high { background: #f38ba8; color: #1e1e2e; padding: 3px 10px; border-radius: 20px; font-size: 12px; font-weight: bold; }
                .badge-medium { background: #fab387; color: #1e1e2e; padding: 3px 10px; border-radius: 20px; font-size: 12px; font-weight: bold; }
                .badge-low { background: #a6e3a1; color: #1e1e2e; padding: 3px 10px; border-radius: 20px; font-size: 12px; font-weight: bold; }
                .btn { display: inline-block; background: #89b4fa; color: #1e1e2e; padding: 12px 28px; border-radius: 8px; text-decoration: none; font-weight: bold; margin-top: 20px; }
                .footer { background: #181825; padding: 16px; text-align: center; color: #6c7086; font-size: 11px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>📋 Kanban App</h1>
                </div>
                <div class="body">
        """ + content + """
                </div>
                <div class="footer">
                    Vous recevez cet email car vous êtes membre de Kanban App.<br>
                    © 2026 Kanban App
                </div>
            </div>
        </body>
        </html>
        """;
    }

    public static String welcome(String fullName) {
        String content = """
            <h2>Bienvenue, %s ! 🎉</h2>
            <p>Votre compte Kanban App a été créé avec succès.</p>
            <p>Vous pouvez maintenant :</p>
            <div class="info-box">
                <div class="info-row"><span class="info-value">✅ Créer des boards Kanban</span></div>
                <div class="info-row"><span class="info-value">✅ Inviter des collaborateurs</span></div>
                <div class="info-row"><span class="info-value">✅ Gérer vos tâches en temps réel</span></div>
                <div class="info-row"><span class="info-value">✅ Suivre l'avancement de vos projets</span></div>
            </div>
            <p>Connectez-vous dès maintenant pour commencer !</p>
        """.formatted(fullName);
        return wrapper(content);
    }

    public static String invitation(String email, String boardName, String invitedBy) {
        String content = """
            <h2>Vous avez été invité ! 🎊</h2>
            <p><strong>%s</strong> vous a invité à rejoindre un board Kanban.</p>
            <div class="info-box">
                <div class="info-row">
                    <span class="info-label">Board :</span>
                    <span class="info-value">%s</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Invité par :</span>
                    <span class="info-value">%s</span>
                </div>
            </div>
            <p>Connectez-vous à l'application pour accéder au board.</p>
        """.formatted(invitedBy, boardName, invitedBy);
        return wrapper(content);
    }

    public static String taskAssigned(String taskTitle, String description,
                                      String priority, String deadline, String boardName, String assignedBy) {
        String badgeClass = switch (priority) {
            case "HIGH" -> "badge-high";
            case "MEDIUM" -> "badge-medium";
            default -> "badge-low";
        };
        String content = """
            <h2>Nouvelle tâche assignée 📌</h2>
            <p><strong>%s</strong> vous a assigné une tâche.</p>
            <div class="info-box">
                <div class="info-row">
                    <span class="info-label">Tâche :</span>
                    <span class="info-value">%s</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Description :</span>
                    <span class="info-value">%s</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Priorité :</span>
                    <span class="%s">%s</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Deadline :</span>
                    <span class="info-value">%s</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Board :</span>
                    <span class="info-value">%s</span>
                </div>
            </div>
        """.formatted(assignedBy, taskTitle, description,
                badgeClass, priority, deadline, boardName);
        return wrapper(content);
    }


    public static String taskCompleted(String taskTitle, String boardName, String completedBy) {
        String content = """
            <h2>Tâche terminée ✅</h2>
            <p>Une tâche que vous avez créée vient d'être complétée !</p>
            <div class="info-box">
                <div class="info-row">
                    <span class="info-label">Tâche :</span>
                    <span class="info-value">%s</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Board :</span>
                    <span class="info-value">%s</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Complétée par :</span>
                    <span class="info-value">%s</span>
                </div>
            </div>
        """.formatted(taskTitle, boardName, completedBy);
        return wrapper(content);
    }

    public static String newComment(String taskTitle, String commentContent, String commentedBy) {
        String content = """
            <h2>Nouveau commentaire 💬</h2>
            <p><strong>%s</strong> a commenté sur votre tâche.</p>
            <div class="info-box">
                <div class="info-row">
                    <span class="info-label">Tâche :</span>
                    <span class="info-value">%s</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Commentaire :</span>
                    <span class="info-value">%s</span>
                </div>
            </div>
        """.formatted(commentedBy, taskTitle, commentContent);
        return wrapper(content);
    }

    public static String deadlineReminder(String taskTitle, String deadline, String boardName) {
        String content = """
            <h2>⏰ Rappel de deadline</h2>
            <p>La deadline d'une de vos tâches est <strong>demain</strong> !</p>
            <div class="info-box">
                <div class="info-row">
                    <span class="info-label">Tâche :</span>
                    <span class="info-value">%s</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Deadline :</span>
                    <span class="info-value" style="color: #f38ba8;">%s</span>
                </div>
                <div class="info-row">
                    <span class="info-label">Board :</span>
                    <span class="info-value">%s</span>
                </div>
            </div>
            <p>Pensez à finaliser votre travail à temps !</p>
        """.formatted(taskTitle, deadline, boardName);
        return wrapper(content);
    }

    public static String boardDeleted(String boardName, String deletedBy) {
        String content = """
            <h2>Board supprimé 🗑️</h2>
            <p><strong>%s</strong> a supprimé un board auquel vous participiez.</p>
            <div class="info-box">
                <div class="info-row">
                    <span class="info-label">Board :</span>
                    <span class="info-value">%s</span>
                </div>
            </div>
            <p>Toutes les tâches associées ont été supprimées.</p>
        """.formatted(deletedBy, boardName);
        return wrapper(content);
    }
}