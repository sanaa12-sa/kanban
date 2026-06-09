package com.kanban.model;

import com.kanban.pattern.ColumnComposite;
import jakarta.persistence.*;

@Entity
@Table(name = "columns")
public class BoardColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "board_id", nullable = false)
    private int boardId;

    @Column(nullable = false)
    private String name;

    private int position;

    @Column(name = "wip_limit")
    private int wipLimit;

    public BoardColumn() {}

    // ── Getters & Setters ──────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBoardId() { return boardId; }
    public void setBoardId(int boardId) { this.boardId = boardId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public int getWipLimit() { return wipLimit; }
    public void setWipLimit(int wipLimit) { this.wipLimit = wipLimit; }

    // ── Bridge vers le Design Pattern Composite ────────────────────────────

    /**
     * Convertit cette entité JPA en nœud Composite.
     * Permet d'intégrer BoardColumn dans la hiérarchie
     * BoardComposite → ColumnComposite → TaskLeaf
     * sans coupler la couche persistance au pattern.
     */
    public ColumnComposite toComposite() {
        return new ColumnComposite(this.id, this.name, this.wipLimit);
    }
}