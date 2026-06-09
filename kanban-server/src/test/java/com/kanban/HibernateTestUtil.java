package com.kanban;

import com.kanban.util.HibernateUtil;
import org.hibernate.SessionFactory;

public class HibernateTestUtil {

    public static void inject(SessionFactory sf) {
        HibernateUtil.setSessionFactory(sf);
    }

    public static void reset() {
        HibernateUtil.setSessionFactory(null);
    }
}