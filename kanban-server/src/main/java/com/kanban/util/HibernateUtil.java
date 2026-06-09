package com.kanban.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static SessionFactory sessionFactory;

    // ── Plus de bloc static — initialisation lazy ──
    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                sessionFactory = new Configuration()
                        .configure("hibernate.cfg.xml")
                        .buildSessionFactory();
                System.out.println("✅ Connexion Hibernate OK");
            } catch (Exception e) {
                System.err.println("❌ Erreur Hibernate : " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
        return sessionFactory;
    }

    // ── Permet d'injecter une SessionFactory externe (ex: H2 pour les tests) ──
    public static void setSessionFactory(SessionFactory sf) {
        sessionFactory = sf;
    }

    public static void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }
}