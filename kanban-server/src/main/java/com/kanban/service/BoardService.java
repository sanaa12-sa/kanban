package com.kanban.service;
import com.kanban.adapter.NotificationSender;
import com.kanban.adapter.EmailNotificationAdapter;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kanban.handler.RequestHandler;
import com.kanban.model.Board;
import com.kanban.model.BoardColumn;
import com.kanban.model.BoardMember;
import com.kanban.model.User;
import com.kanban.server.ClientHandler;
import com.kanban.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.time.LocalDateTime;
import java.util.List;

public class BoardService {

    // ── Adapter Pattern ──────────────────────────────────────────────────────
    private final NotificationSender notifier = new EmailNotificationAdapter();

    // ── Helper ───────────────────────────────────────────────────────────────
    private JsonObject boardToJson(Board board) {
        JsonObject j = new JsonObject();
        j.addProperty("id",          board.getId());
        j.addProperty("title",       board.getTitle());
        j.addProperty("description", board.getDescription());
        j.addProperty("themeColor",  board.getThemeColor());
        j.addProperty("visibility",  board.getVisibility().name());
        j.addProperty("createdBy",   board.getCreatedBy());
        j.addProperty("createdAt",
                board.getCreatedAt() != null ? board.getCreatedAt().toString() : "");
        return j;
    }

    // ── CREATE BOARD ─────────────────────────────────────────────────────────
    public String createBoard(JsonObject data, ClientHandler client) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            Board board = new Board.Builder(
                    data.get("title").getAsString(),
                    client.getUserId())
                    .description(data.get("description").getAsString())
                    .themeColor(data.get("themeColor").getAsString())
                    .visibility(Board.Visibility.valueOf(data.get("visibility").getAsString()))
                    .createdAt(LocalDateTime.now())
                    .build();

            session.persist(board);
            session.flush();
            session.refresh(board);

            String[] defaultColumns = {"À faire", "En cours", "En révision", "Terminé"};
            for (int i = 0; i < defaultColumns.length; i++) {
                BoardColumn column = new BoardColumn();
                column.setBoardId(board.getId());
                column.setName(defaultColumns[i]);
                column.setPosition(i);
                column.setWipLimit(0);
                session.persist(column);
            }

            transaction.commit();
            return RequestHandler.success("Board créé avec succès", boardToJson(board));

        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) transaction.rollback();
            return RequestHandler.error("Erreur création board : " + e.getMessage());
        }
    }

    // ── GET BOARDS ───────────────────────────────────────────────────────────
    public String getBoards(ClientHandler client) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Board> boards = session.createQuery(
                            "FROM Board b WHERE b.createdBy = :userId " +
                                    "OR b.id IN (SELECT bm.boardId FROM BoardMember bm WHERE bm.userId = :userId)",
                            Board.class)
                    .setParameter("userId", client.getUserId())
                    .list();

            JsonArray boardsArray = new JsonArray();
            for (Board board : boards) boardsArray.add(boardToJson(board));

            return RequestHandler.success("Boards récupérés", boardsArray);

        } catch (Exception e) {
            return RequestHandler.error("Erreur récupération boards : " + e.getMessage());
        }
    }

    // ── INVITE MEMBER (CORRIGÉ AVEC EMAIL ASYNCHRONE) ─────────────────────────
    public String inviteMember(JsonObject data, ClientHandler client) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            String email  = data.get("email").getAsString();
            int boardId   = data.get("boardId").getAsInt();

            User user = session.createQuery("FROM User WHERE email = :email", User.class)
                    .setParameter("email", email).uniqueResult();
            if (user == null) return RequestHandler.error("Utilisateur introuvable");

            Board board = session.get(Board.class, boardId);
            if (board == null) return RequestHandler.error("Board introuvable");

            if (board.getCreatedBy() != client.getUserId())
                return RequestHandler.error("Tu n'es pas le propriétaire de ce board");

            Long count = session.createQuery(
                            "SELECT COUNT(bm) FROM BoardMember bm WHERE bm.boardId = :boardId AND bm.userId = :userId",
                            Long.class)
                    .setParameter("boardId", boardId)
                    .setParameter("userId", user.getId())
                    .uniqueResult();
            if (count != null && count > 0)
                return RequestHandler.error("Cet utilisateur est déjà membre du board");

            BoardMember member = new BoardMember();
            member.setBoardId(boardId);
            member.setUserId(user.getId());
            member.setRole(BoardMember.Role.MEMBER);
            session.persist(member);

            // Récupérer le nom de l'inviteur AVANT de fermer la session
            User inviter = session.get(User.class, client.getUserId());
            String inviterName = inviter != null ? inviter.getFullName() : "Un utilisateur";
            String boardTitle = board.getTitle();
            String userEmail = user.getEmail();

            transaction.commit();

            // ── EMAIL ASYNCHRONE (ne bloque PAS la réponse) ──
            new Thread(() -> {
                try {
                    notifier.sendInvitation(userEmail, boardTitle, inviterName);
                    System.out.println("✅ Email d'invitation envoyé à: " + userEmail);
                } catch (Exception ex) {
                    System.err.println("❌ Erreur envoi email invitation: " + ex.getMessage());
                }
            }).start();

            JsonObject userJson = new JsonObject();
            userJson.addProperty("id",       user.getId());
            userJson.addProperty("fullName", user.getFullName());
            userJson.addProperty("email",    user.getEmail());

            return RequestHandler.success("Membre invité avec succès", userJson);

        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) transaction.rollback();
            return RequestHandler.error("Erreur invitation : " + e.getMessage());
        }
    }

    // ── DELETE BOARD ─────────────────────────────────────────────────────────
    public String deleteBoard(JsonObject data, ClientHandler client) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            int boardId = data.get("boardId").getAsInt();
            Board board = session.get(Board.class, boardId);
            if (board == null) return RequestHandler.error("Board introuvable");

            if (board.getCreatedBy() != client.getUserId())
                return RequestHandler.error("Tu n'es pas le propriétaire de ce board");

            List<BoardMember> members = session.createQuery(
                            "FROM BoardMember WHERE boardId = :boardId", BoardMember.class)
                    .setParameter("boardId", boardId).list();

            User deleter = session.get(User.class, client.getUserId());
            String deleterName = deleter != null ? deleter.getFullName() : "Un utilisateur";
            String boardTitle = board.getTitle();

            // Supprimer en cascade
            session.createMutationQuery(
                            "DELETE FROM Comment c WHERE c.taskId IN " +
                                    "(SELECT t.id FROM Task t WHERE t.columnId IN " +
                                    "(SELECT col.id FROM BoardColumn col WHERE col.boardId = :boardId))")
                    .setParameter("boardId", boardId).executeUpdate();

            session.createMutationQuery(
                            "DELETE FROM Task t WHERE t.columnId IN " +
                                    "(SELECT col.id FROM BoardColumn col WHERE col.boardId = :boardId)")
                    .setParameter("boardId", boardId).executeUpdate();

            session.createMutationQuery(
                            "DELETE FROM BoardColumn WHERE boardId = :boardId")
                    .setParameter("boardId", boardId).executeUpdate();

            session.createMutationQuery(
                            "DELETE FROM BoardMember WHERE boardId = :boardId")
                    .setParameter("boardId", boardId).executeUpdate();

            session.remove(board);
            transaction.commit();

            // ── NOTIFICATIONS ASYNCHRONES ──
            new Thread(() -> {
                for (BoardMember member : members) {
                    try (Session s2 = HibernateUtil.getSessionFactory().openSession()) {
                        User memberUser = s2.get(User.class, member.getUserId());
                        if (memberUser != null && memberUser.getId() != client.getUserId()) {
                            notifier.sendBoardDeleted(memberUser.getEmail(), boardTitle, deleterName);
                        }
                    } catch (Exception ex) {
                        System.err.println("Erreur envoi email suppression: " + ex.getMessage());
                    }
                }
            }).start();

            return RequestHandler.success("Board supprimé avec succès", null);

        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) transaction.rollback();
            return RequestHandler.error("Erreur suppression board : " + e.getMessage());
        }
    }

    // ── GET MEMBERS ──────────────────────────────────────────────────────────
    public String getMembers(JsonObject data, ClientHandler client) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            int boardId = data.get("boardId").getAsInt();

            List<BoardMember> members = session.createQuery(
                            "FROM BoardMember WHERE boardId = :boardId", BoardMember.class)
                    .setParameter("boardId", boardId).list();

            JsonArray membersArray = new JsonArray();
            for (BoardMember bm : members) {
                User user = session.get(User.class, bm.getUserId());
                if (user == null) continue;
                JsonObject obj = new JsonObject();
                obj.addProperty("userId",   user.getId());
                obj.addProperty("fullName", user.getFullName());
                obj.addProperty("email",    user.getEmail());
                obj.addProperty("role",     bm.getRole().name());
                membersArray.add(obj);
            }

            return RequestHandler.success("Membres récupérés", membersArray);

        } catch (Exception e) {
            return RequestHandler.error("Erreur récupération membres : " + e.getMessage());
        }
    }

    // ── GET BOARD BY ID ──────────────────────────────────────────────────────
    public String getBoardById(JsonObject data, ClientHandler client) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            int boardId = data.get("boardId").getAsInt();
            Board board = session.get(Board.class, boardId);

            if (board == null) {
                return RequestHandler.error("Board introuvable");
            }

            if (board.getCreatedBy() != client.getUserId()) {
                Long count = session.createQuery(
                                "SELECT COUNT(bm) FROM BoardMember bm WHERE bm.boardId = :boardId AND bm.userId = :userId",
                                Long.class)
                        .setParameter("boardId", boardId)
                        .setParameter("userId", client.getUserId())
                        .uniqueResult();

                if (count == null || count == 0) {
                    return RequestHandler.error("Accès non autorisé");
                }
            }

            JsonObject boardJson = boardToJson(board);
            return RequestHandler.success("Board récupéré", boardJson);
        } catch (Exception e) {
            return RequestHandler.error("Erreur récupération board : " + e.getMessage());
        }
    }
}