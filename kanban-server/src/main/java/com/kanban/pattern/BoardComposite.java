package com.kanban.pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Design Pattern Composite — Racine (Root Composite).
 * Un Board contient des BoardColumns, qui contiennent des Tasks.
 *
 * Hiérarchie :
 *   BoardComposite
 *     └── ColumnComposite  (1..n)
 *           └── TaskLeaf   (0..n)
 */
public class BoardComposite implements BoardComponent {

    private final int    id;
    private final String title;
    private final List<ColumnComposite> columns = new ArrayList<>();

    public BoardComposite(int id, String title) {
        this.id    = id;
        this.title = title;
    }

    public void addColumn(ColumnComposite column) {
        columns.add(column);
    }

    public void removeColumn(ColumnComposite column) {
        columns.remove(column);
    }

    public List<ColumnComposite> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    @Override
    public String getName() { return title; }

    @Override
    public void display(String indent) {
        System.out.println(indent + "🗂️  [BOARD #" + id + "] " + title
                + "  — " + getTaskCount() + " tâche(s) au total");
        for (ColumnComposite col : columns) {
            col.display(indent + "  ");
        }
    }

    @Override
    public int getTaskCount() {
        return columns.stream().mapToInt(ColumnComposite::getTaskCount).sum();
    }

    public int getId() { return id; }
}