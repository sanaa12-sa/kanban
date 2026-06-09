package com.kanban;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class TestSessionFactory {

    private static SessionFactory sessionFactory;

    public static SessionFactory get() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            sessionFactory = new Configuration()
                    .configure("hibernate-test.cfg.xml")
                    .buildSessionFactory();
        }
        return sessionFactory;
    }

    public static void close() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }
}