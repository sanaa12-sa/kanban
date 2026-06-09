package com.kanban;

import com.kanban.pattern.BoardComponent;
import com.kanban.pattern.BoardComposite;
import com.kanban.pattern.ColumnComposite;
import com.kanban.pattern.TaskLeaf;
import com.kanban.adapter.EmailNotificationAdapter;
import com.kanban.adapter.NotificationSender;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class CompositePatternTest {

    // ✅ Test 1 : BoardComposite contient des ColumnComposite
    @Test
    void testBoardContainsColumns() {
        BoardComposite board = new BoardComposite(1, "Mon Board");
        ColumnComposite col1 = new ColumnComposite(1, "To Do",      5);
        ColumnComposite col2 = new ColumnComposite(2, "In Progress", 3);
        board.addColumn(col1);
        board.addColumn(col2);
        assertEquals(2, board.getColumns().size());
    }

    // ✅ Test 2 : ColumnComposite contient des TaskLeaf
    @Test
    void testColumnContainsTasks() {
        ColumnComposite col = new ColumnComposite(1, "In Progress", 3);
        col.addTask(new TaskLeaf(1, "Tâche 1", "HIGH",   ""));
        col.addTask(new TaskLeaf(2, "Tâche 2", "MEDIUM", ""));
        assertEquals(2, col.getTasks().size());
    }

    // ✅ Test 3 : getTaskCount() sur board → somme correcte de toutes les tâches
    @Test
    void testGetTaskCountOnBoard() {
        BoardComposite board = new BoardComposite(1, "Board Comptage");

        ColumnComposite col1 = new ColumnComposite(1, "To Do", 10);
        col1.addTask(new TaskLeaf(1, "T1", "LOW",    ""));
        col1.addTask(new TaskLeaf(2, "T2", "MEDIUM", ""));

        ColumnComposite col2 = new ColumnComposite(2, "Done", 10);
        col2.addTask(new TaskLeaf(3, "T3", "HIGH", ""));

        board.addColumn(col1);
        board.addColumn(col2);

        assertEquals(3, board.getTaskCount());
    }

    // ✅ Test 4 : getTaskCount() sur colonne vide → 0
    @Test
    void testGetTaskCountEmptyColumn() {
        ColumnComposite col = new ColumnComposite(1, "Vide", 5);
        assertEquals(0, col.getTaskCount());
    }

    // ✅ Test 5 : isWipLimitReached() → false quand pas pleine
    @Test
    void testWipLimitNotReached() {
        ColumnComposite col = new ColumnComposite(1, "In Progress", 3);
        col.addTask(new TaskLeaf(1, "T1", "LOW", ""));
        col.addTask(new TaskLeaf(2, "T2", "LOW", ""));
        assertFalse(col.isWipLimitReached());
    }

    // ✅ Test 6 : isWipLimitReached() → true quand limite atteinte
    @Test
    void testWipLimitReached() {
        ColumnComposite col = new ColumnComposite(1, "In Progress", 2);
        col.addTask(new TaskLeaf(1, "T1", "HIGH", ""));
        col.addTask(new TaskLeaf(2, "T2", "HIGH", ""));
        assertTrue(col.isWipLimitReached());
    }

    // ✅ Test 7 : isWipLimitReached() → false quand wipLimit = 0 (illimité)
    @Test
    void testWipLimitZeroMeansUnlimited() {
        ColumnComposite col = new ColumnComposite(1, "Sans limite", 0);
        for (int i = 0; i < 10; i++) {
            col.addTask(new TaskLeaf(i, "T" + i, "LOW", ""));
        }
        assertFalse(col.isWipLimitReached());
    }

    // ✅ Test 8 : display() sur board → s'exécute sans erreur
    @Test
    void testDisplayBoardNoException() {
        BoardComposite board = new BoardComposite(1, "Display Test");
        ColumnComposite col  = new ColumnComposite(1, "To Do", 5);
        col.addTask(new TaskLeaf(1, "Tâche", "LOW", "2026-12-31"));
        board.addColumn(col);
        assertDoesNotThrow(() -> board.display(""));
    }

    // ✅ Test 9 : display() sur colonne → s'exécute sans erreur
    @Test
    void testDisplayColumnNoException() {
        ColumnComposite col = new ColumnComposite(1, "To Do", 5);
        col.addTask(new TaskLeaf(1, "Tâche", "HIGH", ""));
        assertDoesNotThrow(() -> col.display("  "));
    }

    // ✅ Test 10 : display() sur TaskLeaf → s'exécute sans erreur
    @Test
    void testDisplayTaskNoException() {
        TaskLeaf task = new TaskLeaf(1, "Ma tâche", "MEDIUM", "2026-06-15");
        assertDoesNotThrow(() -> task.display("    "));
    }

    // ✅ Test 11 : TaskLeaf getTaskCount() → toujours 1
    @Test
    void testTaskLeafCountIsOne() {
        TaskLeaf task = new TaskLeaf(1, "Leaf", "LOW", "");
        assertEquals(1, task.getTaskCount());
    }

    // ✅ Test 12 : BoardComposite board vide → getTaskCount() = 0
    @Test
    void testEmptyBoardTaskCount() {
        BoardComposite board = new BoardComposite(1, "Board Vide");
        assertEquals(0, board.getTaskCount());
    }

    // ✅ Test 13 : BoardComponent est bien implémenté par les 3 classes
    @Test
    void testAllImplementBoardComponent() {
        BoardComponent board  = new BoardComposite(1, "B");
        BoardComponent col    = new ColumnComposite(1, "C", 5);
        BoardComponent task   = new TaskLeaf(1, "T", "LOW", "");
        assertNotNull(board);
        assertNotNull(col);
        assertNotNull(task);
    }

    // ✅ Test 14 : EmailNotificationAdapter implémente NotificationSender
    @Test
    void testAdapterImplementsInterface() {
        NotificationSender adapter = new EmailNotificationAdapter();
        assertNotNull(adapter);
        assertInstanceOf(NotificationSender.class, adapter);
    }

    // ✅ Test 15 : getName() retourne le bon nom
    @Test
    void testGetName() {
        BoardComposite  board = new BoardComposite(1,  "Mon Board");
        ColumnComposite col   = new ColumnComposite(1, "Ma Colonne", 3);
        TaskLeaf        task  = new TaskLeaf(1,        "Ma Tâche", "HIGH", "");
        assertEquals("Mon Board",   board.getName());
        assertEquals("Ma Colonne",  col.getName());
        assertEquals("Ma Tâche",    task.getName());
    }
}