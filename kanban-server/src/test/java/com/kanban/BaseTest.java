package com.kanban;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public abstract class BaseTest {

    @BeforeAll
    static void setupDatabase() throws Exception {
        HibernateTestUtil.inject(TestSessionFactory.get());
    }

    @AfterAll
    static void teardownDatabase() throws Exception {
        HibernateTestUtil.reset();
        TestSessionFactory.close();
    }
}