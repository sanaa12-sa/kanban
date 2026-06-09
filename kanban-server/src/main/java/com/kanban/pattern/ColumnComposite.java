package com.kanban.pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Design Pattern Composite — Nœud intermédiaire.
 * Une BoardColumn contient des Tasks (feuilles).
 */
public class ColumnComposite implements BoardComponent {

    private final int    id;
    private final String name;
    private final int    wipLimit;
    private final List<BoardComponent> tasks = new ArrayList<>();

    public ColumnComposite(int id, String name, int wipLimit) {
        this.id       = id;
        this.name     = name;
        this.wipLimit = wipLimit;
    }

    public void addTask(BoardComponent task) {
        tasks.add(task);
    }

    public void removeTask(BoardComponent task) {
        tasks.remove(task);
    }

    public List<BoardComponent> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    /** Vérifie si la limite WIP est atteinte (0 = illimité) */
    public boolean isWipLimitReached() {
        return wipLimit > 0 && tasks.size() >= wipLimit;
    }

    @Override
    public String getName() { return name; }

    @Override
    public void display(String indent) {
        System.out.println(indent + "📂 [COLUMN #" + id + "] " + name
                + "  (" + tasks.size() + " tâche(s)"
                + (wipLimit > 0 ? ", WIP=" + wipLimit : "") + ")");
        for (BoardComponent task : tasks) {
            task.display(indent + "    ");
        }
    }

    @Override
    public int getTaskCount() {
        return tasks.stream().mapToInt(BoardComponent::getTaskCount).sum();
    }

    public int getId()       { return id; }
    public int getWipLimit() { return wipLimit; }
}