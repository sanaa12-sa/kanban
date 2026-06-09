package com.kanban.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "column_id", nullable = false)
    private int columnId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.MEDIUM;

    @Column(name = "assigned_to")
    private int assignedTo;

    @Column(name = "created_by")
    private int createdBy;

    private LocalDate deadline;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum Priority { HIGH, MEDIUM, LOW }

    // Constructeur par défaut requis par Hibernate
    public Task() {}

    // Constructeur privé utilisé par le Builder
    private Task(Builder builder) {
        this.columnId  = builder.columnId;
        this.title     = builder.title;
        this.description = builder.description;
        this.priority  = builder.priority;
        this.assignedTo = builder.assignedTo;
        this.createdBy = builder.createdBy;
        this.deadline  = builder.deadline;
        this.createdAt = builder.createdAt;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getColumnId() { return columnId; }
    public void setColumnId(int columnId) { this.columnId = columnId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public int getAssignedTo() { return assignedTo; }
    public void setAssignedTo(int assignedTo) { this.assignedTo = assignedTo; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ── Design Pattern : Builder ───────────────────────────────────────────────

    public static class Builder {
        // champs obligatoires
        private final int columnId;
        private final String title;
        private final int createdBy;
        // champs optionnels avec valeurs par défaut
        private String description = "";
        private Priority priority  = Priority.MEDIUM;
        private int assignedTo     = 0;
        private LocalDate deadline = null;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder(int columnId, String title, int createdBy) {
            if (title == null || title.isBlank())
                throw new IllegalArgumentException("Le titre est obligatoire");
            this.columnId  = columnId;
            this.title     = title;
            this.createdBy = createdBy;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Builder assignedTo(int assignedTo) {
            this.assignedTo = assignedTo;
            return this;
        }

        public Builder deadline(LocalDate deadline) {
            this.deadline = deadline;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Task build() {
            return new Task(this);
        }
    }
}