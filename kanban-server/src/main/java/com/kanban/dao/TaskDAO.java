package com.kanban.dao;

import com.kanban.model.Task;
import com.kanban.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.time.LocalDate;
import java.util.List;

public class TaskDAO {
    public Task getById(int id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Task.class, id);
        }
    }

    public void save(Task task) { execute(s -> s.persist(task)); }
    public void update(Task task) { execute(s -> s.merge(task)); }
    public void delete(Task task) { execute(s -> s.remove(task)); }

    // Task 3: HQL Complex Methods
    public List<Task> search(String query) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Task WHERE title LIKE :q OR description LIKE :q", Task.class)
                    .setParameter("q", "%" + query + "%").list();
        }
    }

    public List<Task> findOverdueTasks() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Task WHERE deadline < :today", Task.class)
                    .setParameter("today", LocalDate.now()).list();
        }
    }



    // 3. جلب المهام المستحقة غداً (هذه الدالة كانت ناقصة)
    public List<Task> findTasksDueTomorrow() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Task WHERE deadline = :tomorrow", Task.class)
                    .setParameter("tomorrow", LocalDate.now().plusDays(1)).list();
        }
    }

    private void execute(java.util.function.Consumer<Session> action) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            action.accept(session);
            tx.commit();
        } catch (Exception e) { if (tx != null) tx.rollback(); throw e; }
    }
}