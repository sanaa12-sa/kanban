package com.kanban.dao;

import com.kanban.model.User;
import com.kanban.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class UserDAO {

    // ✅ SAVE USER
    public void save(User user) {

        Session session = HibernateUtil
                .getSessionFactory()
                .openSession();

        Transaction tx = session.beginTransaction();

        session.persist(user);

        tx.commit();
        session.close();
    }

    // ✅ GET USER BY ID
    public User getById(int id) {

        Session session = HibernateUtil
                .getSessionFactory()
                .openSession();

        User user = session.get(User.class, id);

        session.close();

        return user;
    }

    // ✅ GET USER BY EMAIL
    public User getByEmail(String email) {

        Session session = HibernateUtil
                .getSessionFactory()
                .openSession();

        User user = session.createQuery(
                        "FROM User WHERE email = :email",
                        User.class
                )
                .setParameter("email", email)
                .uniqueResult();

        session.close();

        return user;
    }

    // ✅ GET ALL USERS
    public List<User> getAll() {

        Session session = HibernateUtil
                .getSessionFactory()
                .openSession();

        List<User> users = session
                .createQuery("FROM User", User.class)
                .list();

        session.close();

        return users;
    }

    // ✅ UPDATE USER
    public void update(User user) {

        Session session = HibernateUtil
                .getSessionFactory()
                .openSession();

        Transaction tx = session.beginTransaction();

        session.merge(user);

        tx.commit();
        session.close();
    }

    // ✅ DELETE USER
    public void delete(User user) {

        Session session = HibernateUtil
                .getSessionFactory()
                .openSession();

        Transaction tx = session.beginTransaction();

        session.remove(user);

        tx.commit();
        session.close();
    }
}