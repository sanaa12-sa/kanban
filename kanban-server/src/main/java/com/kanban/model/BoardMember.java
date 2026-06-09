package com.kanban.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "board_members")
public class BoardMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "board_id")
    private int boardId;

    @Column(name = "user_id")
    private int userId;

    // ✅ Ajouté — correspond au ENUM dans MySQL
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role = Role.MEMBER; // par défaut MEMBER

    public enum Role {
        ADMIN, MEMBER
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBoardId() { return boardId; }
    public void setBoardId(int boardId) { this.boardId = boardId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}