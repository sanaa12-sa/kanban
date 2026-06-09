package com.kanban.server;
import com.kanban.email.DeadlineScheduler;
import com.kanban.util.HibernateUtil;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static final int PORT = 5008;

    public static void main(String[] args) {
        // Initialiser Hibernate au démarrage
        HibernateUtil.getSessionFactory();

        // Démarrer le scheduler de deadlines
        DeadlineScheduler.start();
        System.out.println("🚀 Serveur Kanban démarré sur le port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("✅ Nouveau client connecté : "
                        + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket);
                Thread thread = new Thread(handler);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur serveur : " + e.getMessage());
        }
    }
}
