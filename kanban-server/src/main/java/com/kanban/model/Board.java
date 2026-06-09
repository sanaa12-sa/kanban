package com.kanban.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "boards")
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "theme_color")
    private String themeColor;

    @Enumerated(EnumType.STRING)
    private Visibility visibility = Visibility.PRIVATE;

    @Column(name = "created_by")
    private int createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum Visibility { PRIVATE, PUBLIC }

    // Constructeur par défaut requis par Hibernate
    public Board() {}

    // Constructeur privé utilisé par le Builder
    private Board(Builder builder) {
        this.title      = builder.title;
        this.description = builder.description;
        this.themeColor = builder.themeColor;
        this.visibility = builder.visibility;
        this.createdBy  = builder.createdBy;
        this.createdAt  = builder.createdAt;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getThemeColor() { return themeColor; }
    public void setThemeColor(String themeColor) { this.themeColor = themeColor; }

    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ── Design Pattern : Builder ───────────────────────────────────────────────

    public static class Builder {
        // champs obligatoires
        private final String title;
        private final int createdBy;
        // champs optionnels
        private String description = "";
        private String themeColor  = "#89b4fa";
        private Visibility visibility = Visibility.PRIVATE;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder(String title, int createdBy) {
            if (title == null || title.isBlank())
                throw new IllegalArgumentException("Le titre du board est obligatoire");
            this.title     = title;
            this.createdBy = createdBy;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder themeColor(String themeColor) {
            this.themeColor = themeColor;
            return this;
        }

        public Builder visibility(Visibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Board build() {
            return new Board(this);
        }
    }
}