package com.kanban.dao;

import com.kanban.model.Board;
import com.kanban.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class BoardDAO {

    public void save(Board board) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();

        session.save(board);

        tx.commit();
        session.close();
    }

    public Board getById(int id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Board board = session.get(Board.class, id);
        session.close();
        return board;
    }

    public List<Board> getAll() {
        Session session = HibernateUtil.getSessionFactory().openSession();

        List<Board> boards = session.createQuery("FROM Board", Board.class).list();

        session.close();
        return boards;
    }

    public List<Board> getByUser(int userId) {
        Session session = HibernateUtil.getSessionFactory().openSession();

        List<Board> boards = session.createQuery(
                        "FROM Board WHERE createdBy = :uid", Board.class)
                .setParameter("uid", userId)
                .list();

        session.close();
        return boards;
    }

    public void update(Board board) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();

        session.update(board);

        tx.commit();
        session.close();
    }

    public void delete(Board board) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();

        session.delete(board);

        tx.commit();
        session.close();
    }
}