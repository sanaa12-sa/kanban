package com.kanban.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ── Nouveaux champs pour les paramètres ──────────────────────────────────
    @Column(name = "notif_enabled", nullable = false)
    private boolean notifEnabled = true;

    @Column(name = "deadline_delay", nullable = false)
    private int deadlineDelay = 1; // en jours avant la deadline

    // Constructeur par défaut requis par Hibernate
    public User() {}

    // Constructeur privé utilisé par le Builder
    private User(Builder builder) {
        this.fullName      = builder.fullName;
        this.email         = builder.email;
        this.passwordHash  = builder.passwordHash;
        this.profilePicture = builder.profilePicture;
        this.createdAt     = builder.createdAt;
        this.notifEnabled  = builder.notifEnabled;
        this.deadlineDelay = builder.deadlineDelay;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isNotifEnabled() { return notifEnabled; }
    public void setNotifEnabled(boolean notifEnabled) { this.notifEnabled = notifEnabled; }

    public int getDeadlineDelay() { return deadlineDelay; }
    public void setDeadlineDelay(int deadlineDelay) { this.deadlineDelay = deadlineDelay; }

    // ── Design Pattern : Builder ───────────────────────────────────────────────

    public static class Builder {
        private final String fullName;
        private final String email;
        private final String passwordHash;
        private String profilePicture  = null;
        private LocalDateTime createdAt = LocalDateTime.now();
        private boolean notifEnabled   = true;
        private int deadlineDelay      = 1;

        public Builder(String fullName, String email, String passwordHash) {
            if (fullName == null || fullName.isBlank())
                throw new IllegalArgumentException("Le nom complet est obligatoire");
            if (email == null || !email.contains("@"))
                throw new IllegalArgumentException("Email invalide");
            if (passwordHash == null || passwordHash.isBlank())
                throw new IllegalArgumentException("Le mot de passe est obligatoire");
            this.fullName     = fullName;
            this.email        = email;
            this.passwordHash = passwordHash;
        }

        public Builder profilePicture(String profilePicture) {
            this.profilePicture = profilePicture; return this;
        }
        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt; return this;
        }
        public Builder notifEnabled(boolean notifEnabled) {
            this.notifEnabled = notifEnabled; return this;
        }
        public Builder deadlineDelay(int deadlineDelay) {
            this.deadlineDelay = deadlineDelay; return this;
        }

        public User build() { return new User(this); }
    }
}