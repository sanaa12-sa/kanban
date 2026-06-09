package com.kanban.service;

import com.kanban.adapter.NotificationSender;
import com.kanban.adapter.EmailNotificationAdapter;
import com.google.gson.JsonObject;
import com.kanban.handler.RequestHandler;
import com.kanban.model.User;
import com.kanban.server.ClientHandler;
import com.kanban.server.SessionManager;
import com.kanban.util.HibernateUtil;
import com.kanban.util.PasswordUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.time.LocalDateTime;

public class UserService {

    private final NotificationSender notifier = new EmailNotificationAdapter();

    public String register(JsonObject data) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            String fullName = data.get("fullName").getAsString().trim();
            String email    = data.get("email").getAsString().trim();
            String password = data.get("password").getAsString();

            if (fullName.isEmpty())
                return RequestHandler.error("Le nom complet est obligatoire");
            if (!email.contains("@"))
                return RequestHandler.error("Email invalide");
            if (password.length() < 8)
                return RequestHandler.error("Le mot de passe doit contenir au moins 8 caractères");

            User existing = session.createQuery("FROM User WHERE email = :email", User.class)
                    .setParameter("email", email).uniqueResult();
            if (existing != null) return RequestHandler.error("Email déjà utilisé");

            User user = new User.Builder(fullName, email, PasswordUtil.hash(password))
                    .createdAt(LocalDateTime.now())
                    .build();

            session.persist(user);
            transaction.commit();

            notifier.sendWelcome(email, fullName);

            JsonObject userJson = new JsonObject();
            userJson.addProperty("id",        user.getId());
            userJson.addProperty("fullName",   user.getFullName());
            userJson.addProperty("email",      user.getEmail());
            userJson.addProperty("createdAt",  user.getCreatedAt().toString());

            return RequestHandler.success("Inscription réussie", userJson);

        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) transaction.rollback();
            return RequestHandler.error("Erreur inscription : " + e.getMessage());
        }
    }

    public String login(JsonObject data, ClientHandler client) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String email    = data.get("email").getAsString().trim();
            String password = data.get("password").getAsString();

            User user = session.createQuery("FROM User WHERE email = :email", User.class)
                    .setParameter("email", email).uniqueResult();

            if (user == null || !PasswordUtil.verify(password, user.getPasswordHash()))
                return RequestHandler.error("Email ou mot de passe incorrect");

            client.setUserId(user.getId());
            SessionManager.addSession(user.getId(), client);

            JsonObject userData = new JsonObject();
            userData.addProperty("id",            user.getId());
            userData.addProperty("fullName",       user.getFullName());
            userData.addProperty("email",          user.getEmail());
            userData.addProperty("createdAt",      user.getCreatedAt().toString());
            userData.addProperty("notifEnabled",   user.isNotifEnabled());
            userData.addProperty("deadlineDelay",  user.getDeadlineDelay());

            return RequestHandler.success("Connexion réussie", userData);

        } catch (Exception e) {
            return RequestHandler.error("Erreur connexion : " + e.getMessage());
        }
    }

    public String getUserById(JsonObject data, ClientHandler client) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            int userId = data.get("userId").getAsInt();
            User user = session.get(User.class, userId);

            if (user == null)
                return RequestHandler.error("Utilisateur introuvable");

            JsonObject userJson = new JsonObject();
            userJson.addProperty("id",       user.getId());
            userJson.addProperty("fullName", user.getFullName());
            userJson.addProperty("email",    user.getEmail());

            return RequestHandler.success("Utilisateur récupéré", userJson);
        } catch (Exception e) {
            return RequestHandler.error("Erreur récupération utilisateur : " + e.getMessage());
        }
    }

    // ── Mettre à jour les notifications (on/off) ─────────────────────────────
    public String updateNotifSettings(JsonObject data, ClientHandler client) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            int     userId       = client.getUserId();
            boolean notifEnabled = data.get("notifEnabled").getAsBoolean();

            User user = session.get(User.class, userId);
            if (user == null)
                return RequestHandler.error("Utilisateur introuvable");

            user.setNotifEnabled(notifEnabled);
            session.merge(user);
            tx.commit();

            return RequestHandler.success("Notifications mises à jour", null);
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            return RequestHandler.error("Erreur mise à jour notifications : " + e.getMessage());
        }
    }

    // ── Mettre à jour le délai de rappel deadline ────────────────────────────
    public String updateDeadlineDelay(JsonObject data, ClientHandler client) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            int userId        = client.getUserId();
            int deadlineDelay = data.get("deadlineDelay").getAsInt();

            User user = session.get(User.class, userId);
            if (user == null)
                return RequestHandler.error("Utilisateur introuvable");

            user.setDeadlineDelay(deadlineDelay);
            session.merge(user);
            tx.commit();

            return RequestHandler.success("Délai de deadline mis à jour", null);
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            return RequestHandler.error("Erreur mise à jour délai : " + e.getMessage());
        }
    }
    // ── Mettre à jour le profil (nom + mot de passe) ─────────────────────────
    public String updateProfile(JsonObject data, ClientHandler client) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            int userId = client.getUserId();

            User user = session.get(User.class, userId);
            if (user == null)
                return RequestHandler.error("Utilisateur introuvable");

            // Mettre à jour le nom
            if (data.has("fullName")) {
                String newName = data.get("fullName").getAsString().trim();
                if (newName.isEmpty())
                    return RequestHandler.error("Le nom ne peut pas être vide");
                user.setFullName(newName);
            }

            // Mettre à jour le mot de passe seulement s'il est fourni
            if (data.has("password")) {
                String newPassword = data.get("password").getAsString();
                if (newPassword.length() < 8)
                    return RequestHandler.error("Le mot de passe doit contenir au moins 8 caractères");
                user.setPasswordHash(PasswordUtil.hash(newPassword)); // même hash que register()
            }

            session.merge(user);
            tx.commit();

            return RequestHandler.success("Profil mis à jour", null);

        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            return RequestHandler.error("Erreur mise à jour profil : " + e.getMessage());
        }
    }
}