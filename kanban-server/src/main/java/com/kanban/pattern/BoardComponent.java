package com.kanban.pattern;

/**
 * Design Pattern Composite — Composant de base.
 *
 * Représente un élément générique de la hiérarchie Kanban :
 *   Board (Composite)
 *     └── BoardColumn (Composite)
 *           └── Task (Leaf)
 */
public interface BoardComponent {

    /** Nom affiché de l'élément */
    String getName();

    /** Affiche la hiérarchie avec indentation */
    void display(String indent);

    /** Nombre total de feuilles (tâches) dans ce nœud */
    int getTaskCount();
}