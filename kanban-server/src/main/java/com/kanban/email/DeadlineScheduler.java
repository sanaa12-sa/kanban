package com.kanban.email;

import com.kanban.util.HibernateUtil;
import com.kanban.model.Task;
import com.kanban.model.User;
import com.kanban.model.BoardColumn;
import com.kanban.model.Board;
import org.hibernate.Session;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DeadlineScheduler {

    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    public static void start() {
        long delayUntilMidnight = getDelayUntilMidnight();

        scheduler.scheduleAtFixedRate(
                DeadlineScheduler::checkDeadlines,
                delayUntilMidnight,
                TimeUnit.DAYS.toSeconds(1),
                TimeUnit.SECONDS
        );

        System.out.println("⏰ Scheduler de deadlines démarré");
    }

    private static void checkDeadlines() {
        System.out.println("🔍 Vérification des deadlines...");

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            // Récupérer toutes les tâches qui ont une deadline
            List<Task> tasks = session.createQuery(
                    "FROM Task WHERE deadline IS NOT NULL AND assignedTo > 0", Task.class).list();

            System.out.println("📋 " + tasks.size() + " tâche(s) avec deadline à vérifier");

            for (Task task : tasks) {
                User assignedUser = session.get(User.class, task.getAssignedTo());
                if (assignedUser == null) continue;

                // Respecter la préférence de notification de l'utilisateur
                if (!assignedUser.isNotifEnabled()) continue;

                // Utiliser le délai personnalisé de l'utilisateur
                int daysBeforeDeadline = assignedUser.getDeadlineDelay();
                LocalDate targetDate   = LocalDate.now().plusDays(daysBeforeDeadline);

                if (!task.getDeadline().equals(targetDate)) continue;

                // Récupérer le nom du board
                BoardColumn column = session.get(BoardColumn.class, task.getColumnId());
                String boardName = "Board inconnu";
                if (column != null) {
                    Board board = session.get(Board.class, column.getBoardId());
                    if (board != null) boardName = board.getTitle();
                }

                EmailNotificationService.sendDeadlineReminderEmail(
                        assignedUser.getEmail(),
                        task.getTitle(),
                        task.getDeadline().toString(),
                        boardName
                );

                System.out.println("📧 Rappel envoyé à : " + assignedUser.getEmail()
                        + " (" + daysBeforeDeadline + " jour(s) avant deadline)");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur vérification deadlines : " + e.getMessage());
        }
    }

    private static long getDelayUntilMidnight() {
        LocalTime now  = LocalTime.now();
        LocalTime midnight = LocalTime.MIDNIGHT;
        long secondsUntilMidnight = now.until(midnight, java.time.temporal.ChronoUnit.SECONDS);
        if (secondsUntilMidnight <= 0) {
            secondsUntilMidnight += TimeUnit.DAYS.toSeconds(1);
        }
        System.out.println("⏰ Prochain check dans : " + secondsUntilMidnight + " secondes");
        return secondsUntilMidnight;
    }

    public static void stop() {
        scheduler.shutdown();
    }
}