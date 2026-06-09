package com.kanban.service;
import com.kanban.adapter.NotificationSender;
import com.kanban.adapter.EmailNotificationAdapter;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kanban.handler.RequestHandler;
import com.kanban.model.*;
import com.kanban.server.BoardBroadcaster;
import com.kanban.server.ClientHandler;
import com.kanban.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TaskService {

    // ── Adapter Pattern ──────────────────────────────────────────────────────
    private final NotificationSender notifier = new EmailNotificationAdapter();

    // ── Helpers JSON ─────────────────────────────────────────────────────────

    private JsonObject taskToJson(Task task) {
        JsonObject j = new JsonObject();
        j.addProperty("id",          task.getId());
        j.addProperty("columnId",    task.getColumnId());
        j.addProperty("title",       task.getTitle());
        j.addProperty("description", task.getDescription());
        j.addProperty("priority",    task.getPriority().name());
        j.addProperty("assignedTo",  task.getAssignedTo());
        j.addProperty("createdBy",   task.getCreatedBy());
        j.addProperty("deadline",    task.getDeadline() != null ? task.getDeadline().toString() : "");
        j.addProperty("createdAt",   task.getCreatedAt() != null ? task.getCreatedAt().toString() : "");
        return j;
    }

    private JsonObject commentToJson(Comment comment) {
        JsonObject j = new JsonObject();
        j.addProperty("id",        comment.getId());
        j.addProperty("taskId",    comment.getTaskId());
        j.addProperty("userId",    comment.getUserId());
        j.addProperty("content",   comment.getContent());
        j.addProperty("createdAt", comment.getCreatedAt() != null ? comment.getCreatedAt().toString() : "");
        return j;
    }

    // ── CREATE TASK ──────────────────────────────────────────────────────────
    public String createTask(JsonObject data, ClientHandler client) {
        if (client.getUserId() == -1) return RequestHandler.error("Tu dois être connecté !");

        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            String deadlineStr = data.get("deadline").getAsString();

            // ── Builder Pattern ──
            Task task = new Task.Builder(
                    data.get("columnId").getAsInt(),
                    data.get("title").getAsString(),
                    client.getUserId())
                    .description(data.get("description").getAsString())
                    .priority(Task.Priority.valueOf(data.get("priority").getAsString()))
                    .assignedTo(data.get("assignedTo").getAsInt())
                    .deadline(deadlineStr.isEmpty() ? null : LocalDate.parse(deadlineStr))
                    .createdAt(LocalDateTime.now())
                    .build();

            session.persist(task);
            transaction.commit();

            JsonObject taskJson = taskToJson(task);

            // Broadcast temps réel
            JsonObject broadcast = new JsonObject();
            broadcast.addProperty("type", "TASK_CREATED");
            broadcast.add("data", taskJson);
            BoardBroadcaster.broadcast(client.getCurrentBoardId(), broadcast.toString(), client);

            // ── Adapter Pattern : email d'assignation si un utilisateur est assigné ──
            if (task.getAssignedTo() > 0) {
                try (Session s2 = HibernateUtil.getSessionFactory().openSession()) {
                    User assignee = s2.get(User.class, task.getAssignedTo());
                    User creator  = s2.get(User.class, client.getUserId());
                    if (assignee != null && creator != null) {
                        // Récupérer le nom du board via la colonne
                        BoardColumn col = s2.get(BoardColumn.class, task.getColumnId());
                        String boardName = col != null
                                ? s2.get(Board.class, col.getBoardId()) != null
                                  ? s2.get(Board.class, col.getBoardId()).getTitle()
                                  : "Board"
                                : "Board";
                        notifier.sendTaskAssigned(
                                assignee.getEmail(),
                                task.getTitle(),
                                task.getDescription(),
                                task.getPriority().name(),
                                task.getDeadline() != null ? task.getDeadline().toString() : "",
                                boardName,
                                creator.getFullName());
                    }
                }
            }

            return RequestHandler.success("Tâche créée", taskJson);

        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) transaction.rollback();
            return RequestHandler.error("Erreur création tâche : " + e.getMessage());
        }
    }

    // ── UPDATE TASK ──────────────────────────────────────────────────────────
    public String updateTask(JsonObject data, ClientHandler client) {
        if (client.getUserId() == -1) return RequestHandler.error("Tu dois être connecté !");

        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            int taskId = data.get("taskId").getAsInt();
            Task task  = session.get(Task.class, taskId);
            if (task == null) return RequestHandler.error("Tâche introuvable");

            String deadlineStr = data.get("deadline").getAsString();

            task.setTitle(data.get("title").getAsString());
            task.setDescription(data.get("description").getAsString());
            task.setPriority(Task.Priority.valueOf(data.get("priority").getAsString()));
            task.setAssignedTo(data.get("assignedTo").getAsInt());
            task.setDeadline(deadlineStr.isEmpty() ? null : LocalDate.parse(deadlineStr));

            session.merge(task);
            transaction.commit();

            JsonObject taskJson = taskToJson(task);
            JsonObject broadcast = new JsonObject();
            broadcast.addProperty("type", "TASK_UPDATED");
            broadcast.add("data", taskJson);
            BoardBroadcaster.broadcast(client.getCurrentBoardId(), broadcast.toString(), client);

            return RequestHandler.success("Tâche mise à jour", taskJson);

        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) transaction.rollback();
            return RequestHandler.error("Erreur mise à jour tâche : " + e.getMessage());
        }
    }

    // ── DELETE TASK ──────────────────────────────────────────────────────────
    public String deleteTask(JsonObject data, ClientHandler client) {
        if (client.getUserId() == -1) return RequestHandler.error("Tu dois être connecté !");

        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            int taskId = data.get("taskId").getAsInt();
            Task task  = session.get(Task.class, taskId);
            if (task == null) return RequestHandler.error("Tâche introuvable");

            // Supprimer les commentaires liés
            session.createMutationQuery("DELETE FROM Comment WHERE taskId = :taskId")
                    .setParameter("taskId", taskId).executeUpdate();

            session.remove(task);
            transaction.commit();

            JsonObject broadcast = new JsonObject();
            broadcast.addProperty("type",   "TASK_DELETED");
            broadcast.addProperty("taskId", taskId);
            BoardBroadcaster.broadcast(client.getCurrentBoardId(), broadcast.toString(), client);

            return RequestHandler.success("Tâche supprimée", null);

        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) transaction.rollback();
            return RequestHandler.error("Erreur suppression tâche : " + e.getMessage());
        }
    }

    // ── MOVE TASK ────────────────────────────────────────────────────────────
    public String moveTask(JsonObject data, ClientHandler client) {
        if (client.getUserId() == -1) return RequestHandler.error("Tu dois être connecté !");

        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            int taskId        = data.get("taskId").getAsInt();
            int targetColumnId = data.get("targetColumnId").getAsInt();
            Task task         = session.get(Task.class, taskId);
            if (task == null) return RequestHandler.error("Tâche introuvable");

            // Vérifier WIP limit de la colonne cible
            BoardColumn targetCol = session.get(BoardColumn.class, targetColumnId);
            if (targetCol != null && targetCol.getWipLimit() > 0) {
                Long count = session.createQuery(
                                "SELECT COUNT(t) FROM Task t WHERE t.columnId = :colId", Long.class)
                        .setParameter("colId", targetColumnId).uniqueResult();
                if (count != null && count >= targetCol.getWipLimit()) {
                    return RequestHandler.error(
                            "Limite WIP atteinte pour la colonne \"" + targetCol.getName() + "\" (" + targetCol.getWipLimit() + " max)");
                }
            }

            int oldColumnId = task.getColumnId();
            task.setColumnId(targetColumnId);
            session.merge(task);
            transaction.commit();

            JsonObject broadcast = new JsonObject();
            broadcast.addProperty("type",       "TASK_MOVED");
            broadcast.addProperty("taskId",      taskId);
            broadcast.addProperty("fromColumn",  oldColumnId);
            broadcast.addProperty("toColumn",    targetColumnId);
            BoardBroadcaster.broadcast(client.getCurrentBoardId(), broadcast.toString(), client);

            // Notifier le créateur si la tâche passe en "Terminé"
            if (targetCol != null && targetCol.getName().equalsIgnoreCase("Terminé")) {
                try (Session s2 = HibernateUtil.getSessionFactory().openSession()) {
                    User creator = s2.get(User.class, task.getCreatedBy());
                    User mover   = s2.get(User.class, client.getUserId());
                    Board board  = targetCol != null
                            ? s2.get(Board.class, targetCol.getBoardId()) : null;
                    if (creator != null && mover != null) {
                        notifier.sendTaskCompleted(
                                creator.getEmail(),
                                task.getTitle(),
                                board != null ? board.getTitle() : "Board",
                                mover.getFullName());
                    }
                }
            }

            return RequestHandler.success("Tâche déplacée", taskToJson(task));

        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) transaction.rollback();
            return RequestHandler.error("Erreur déplacement tâche : " + e.getMessage());
        }
    }

    // ── ADD COMMENT ──────────────────────────────────────────────────────────
    public String addComment(JsonObject data, ClientHandler client) {
        if (client.getUserId() == -1) return RequestHandler.error("Tu dois être connecté !");

        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            int taskId = data.get("taskId").getAsInt();
            Task task  = session.get(Task.class, taskId);
            if (task == null) return RequestHandler.error("Tâche introuvable");

            Comment comment = new Comment();
            comment.setTaskId(taskId);
            comment.setUserId(client.getUserId());
            comment.setContent(data.get("content").getAsString());
            comment.setCreatedAt(LocalDateTime.now());

            session.persist(comment);
            transaction.commit();

            JsonObject commentJson = commentToJson(comment);

            JsonObject broadcast = new JsonObject();
            broadcast.addProperty("type", "COMMENT_ADDED");
            broadcast.add("data", commentJson);
            BoardBroadcaster.broadcast(client.getCurrentBoardId(), broadcast.toString(), client);

            // ── Adapter Pattern : notifier le créateur de la tâche ──
            if (task.getCreatedBy() != client.getUserId()) {
                try (Session s2 = HibernateUtil.getSessionFactory().openSession()) {
                    User taskOwner = s2.get(User.class, task.getCreatedBy());
                    User commenter = s2.get(User.class, client.getUserId());
                    if (taskOwner != null && commenter != null) {
                        notifier.sendNewComment(
                                taskOwner.getEmail(),
                                task.getTitle(),
                                comment.getContent(),
                                commenter.getFullName());
                    }
                }
            }

            return RequestHandler.success("Commentaire ajouté", commentJson);

        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) transaction.rollback();
            return RequestHandler.error("Erreur ajout commentaire : " + e.getMessage());
        }
    }

    // ── GET COMMENTS ─────────────────────────────────────────────────────────
    public String getComments(JsonObject data, ClientHandler client) {
        if (client.getUserId() == -1) return RequestHandler.error("Tu dois être connecté !");

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            int taskId = data.get("taskId").getAsInt();

            List<Comment> comments = session.createQuery(
                            "FROM Comment WHERE taskId = :taskId ORDER BY createdAt ASC",
                            Comment.class)
                    .setParameter("taskId", taskId).list();

            JsonArray arr = new JsonArray();
            for (Comment c : comments) arr.add(commentToJson(c));

            return RequestHandler.success("Commentaires récupérés", arr);

        } catch (Exception e) {
            return RequestHandler.error("Erreur récupération commentaires : " + e.getMessage());
        }
    }

    // ── GET TASKS ────────────────────────────────────────────────────────────
    public String getTasks(JsonObject data, ClientHandler client) {
        if (client.getUserId() == -1) return RequestHandler.error("Tu dois être connecté !");

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            int boardId = data.get("boardId").getAsInt();

            List<BoardColumn> columns = session.createQuery(
                            "FROM BoardColumn WHERE boardId = :boardId ORDER BY position",
                            BoardColumn.class)
                    .setParameter("boardId", boardId).list();

            JsonArray columnsArray = new JsonArray();
            for (BoardColumn col : columns) {
                List<Task> tasks = session.createQuery(
                                "FROM Task WHERE columnId = :columnId",
                                Task.class)
                        .setParameter("columnId", col.getId()).list();

                JsonArray tasksArray = new JsonArray();
                for (Task task : tasks) tasksArray.add(taskToJson(task));

                JsonObject colJson = new JsonObject();
                colJson.addProperty("id",       col.getId());
                colJson.addProperty("name",      col.getName());
                colJson.addProperty("position",  col.getPosition());
                colJson.addProperty("wipLimit",  col.getWipLimit());
                colJson.add("tasks", tasksArray);
                columnsArray.add(colJson);
            }

            return RequestHandler.success("Tâches récupérées", columnsArray);

        } catch (Exception e) {
            return RequestHandler.error("Erreur récupération tâches : " + e.getMessage());
        }
    }

    // ── SEARCH TASKS ─────────────────────────────────────────────────────────
    public String searchTasks(JsonObject data, ClientHandler client) {
        if (client.getUserId() == -1) return RequestHandler.error("Tu dois être connecté !");

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String query   = data.get("query").getAsString().trim();
            int    boardId = data.get("boardId").getAsInt();

            List<Task> tasks = session.createQuery(
                            "FROM Task t WHERE (LOWER(t.title) LIKE :q OR LOWER(t.description) LIKE :q) " +
                                    "AND t.columnId IN (SELECT c.id FROM BoardColumn c WHERE c.boardId = :boardId)",
                            Task.class)
                    .setParameter("q",       "%" + query.toLowerCase() + "%")
                    .setParameter("boardId", boardId)
                    .list();

            JsonArray arr = new JsonArray();
            for (Task task : tasks) arr.add(taskToJson(task));

            return RequestHandler.success("Résultats de recherche", arr);

        } catch (Exception e) {
            return RequestHandler.error("Erreur recherche : " + e.getMessage());
        }
    }
    // Ajouter ces méthodes dans TaskService.java

    public String getAllTasks(ClientHandler client) {
        if (client.getUserId() == -1)
            return RequestHandler.error("Tu dois être connecté !");

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Récupérer toutes les tâches des boards où l'utilisateur est membre ou propriétaire
            List<Task> tasks = session.createQuery(
                            "SELECT DISTINCT t FROM Task t " +
                                    "JOIN BoardColumn c ON t.columnId = c.id " +
                                    "JOIN Board b ON c.boardId = b.id " +
                                    "WHERE b.createdBy = :userId " +
                                    "OR b.id IN (SELECT bm.boardId FROM BoardMember bm WHERE bm.userId = :userId)",
                            Task.class)
                    .setParameter("userId", client.getUserId())
                    .list();

            JsonArray tasksArray = new JsonArray();
            for (Task task : tasks) {
                JsonObject taskJson = taskToJson(task);
                // Ajouter l'ID du board pour référence
                BoardColumn col = session.get(BoardColumn.class, task.getColumnId());
                if (col != null) {
                    taskJson.addProperty("boardId", col.getBoardId());
                }
                tasksArray.add(taskJson);
            }

            return RequestHandler.success("Toutes les tâches récupérées", tasksArray);
        } catch (Exception e) {
            return RequestHandler.error("Erreur récupération tâches : " + e.getMessage());
        }
    }

    public String filterTasks(JsonObject data, ClientHandler client) {
        if (client.getUserId() == -1)
            return RequestHandler.error("Tu dois être connecté !");

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String search = data.has("search") ? data.get("search").getAsString() : "";
            String priorityFilter = data.has("priorityFilter") ? data.get("priorityFilter").getAsString() : "Toutes";

            StringBuilder hql = new StringBuilder(
                    "SELECT DISTINCT t FROM Task t " +
                            "JOIN BoardColumn c ON t.columnId = c.id " +
                            "JOIN Board b ON c.boardId = b.id " +
                            "WHERE (b.createdBy = :userId OR b.id IN (SELECT bm.boardId FROM BoardMember bm WHERE bm.userId = :userId))"
            );

            if (!search.isEmpty()) {
                hql.append(" AND (LOWER(t.title) LIKE :search OR LOWER(t.description) LIKE :search)");
            }

            if (!"Toutes".equals(priorityFilter)) {
                String priority = switch (priorityFilter) {
                    case "Haute" -> "HIGH";
                    case "Moyenne" -> "MEDIUM";
                    case "Basse" -> "LOW";
                    default -> null;
                };
                if (priority != null) {
                    hql.append(" AND t.priority = :priority");
                }
            }

            if ("Deadline dépassée".equals(priorityFilter)) {
                hql.append(" AND t.deadline < CURRENT_DATE");
            }

            var query = session.createQuery(hql.toString(), Task.class);
            query.setParameter("userId", client.getUserId());

            if (!search.isEmpty()) {
                query.setParameter("search", "%" + search.toLowerCase() + "%");
            }

            if (!"Toutes".equals(priorityFilter) && !"Deadline dépassée".equals(priorityFilter)) {
                String priority = switch (priorityFilter) {
                    case "Haute" -> "HIGH";
                    case "Moyenne" -> "MEDIUM";
                    case "Basse" -> "LOW";
                    default -> null;
                };
                if (priority != null) {
                    query.setParameter("priority", Task.Priority.valueOf(priority));
                }
            }

            List<Task> tasks = query.list();

            JsonArray tasksArray = new JsonArray();
            for (Task task : tasks) {
                JsonObject taskJson = taskToJson(task);
                BoardColumn col = session.get(BoardColumn.class, task.getColumnId());
                if (col != null) {
                    taskJson.addProperty("boardId", col.getBoardId());
                }
                tasksArray.add(taskJson);
            }

            return RequestHandler.success("Tâches filtrées", tasksArray);
        } catch (Exception e) {
            return RequestHandler.error("Erreur filtrage : " + e.getMessage());
        }
    }

    public String getColumnById(JsonObject data, ClientHandler client) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            int columnId = data.get("columnId").getAsInt();
            BoardColumn column = session.get(BoardColumn.class, columnId);

            if (column == null) {
                return RequestHandler.error("Colonne introuvable");
            }

            JsonObject colJson = new JsonObject();
            colJson.addProperty("id", column.getId());
            colJson.addProperty("name", column.getName());
            colJson.addProperty("boardId", column.getBoardId());

            return RequestHandler.success("Colonne récupérée", colJson);
        } catch (Exception e) {
            return RequestHandler.error("Erreur récupération colonne : " + e.getMessage());
        }
    }
}