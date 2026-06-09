package com.kanban.pattern;

/**
 * Design Pattern Composite — Feuille (Leaf).
 * Une Task est un élément terminal : elle n'a pas d'enfants.
 */
public class TaskLeaf implements BoardComponent {

    private final int    id;
    private final String title;
    private final String priority;
    private final String deadline;

    public TaskLeaf(int id, String title, String priority, String deadline) {
        this.id       = id;
        this.title    = title;
        this.priority = priority;
        this.deadline = deadline == null ? "" : deadline;
    }

    @Override
    public String getName() { return title; }

    @Override
    public void display(String indent) {
        System.out.println(indent + "📌 [TASK #" + id + "] " + title
                + "  [" + priority + "]"
                + (deadline.isEmpty() ? "" : "  ⏰ " + deadline));
    }

    @Override
    public int getTaskCount() { return 1; }

    // Getters utiles
    public int    getId()       { return id; }
    public String getPriority() { return priority; }
    public String getDeadline() { return deadline; }
}