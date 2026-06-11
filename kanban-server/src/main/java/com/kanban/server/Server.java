package com.kanban.server;

import com.kanban.email.DeadlineScheduler;
import com.kanban.util.HibernateUtil;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static final int PORT         = 5009;
    private static final int METRICS_PORT = 8080;

    // ── Métriques Prometheus ─────────────────────────────────────────────────

    // Nombre total de connexions reçues depuis le démarrage
    static final Counter connectionsTotal = Counter.build()
            .name("kanban_connections_total")
            .help("Nombre total de connexions clients")
            .register();

    // Nombre de clients actuellement connectés
    static final Gauge activeConnections = Gauge.build()
            .name("kanban_active_connections")
            .help("Nombre de clients actuellement connectés")
            .register();

    // ── Main ─────────────────────────────────────────────────────────────────

    public static void main(String[] args) {

        // 1. Métriques JVM (CPU, mémoire, threads, GC)
        DefaultExports.initialize();

        // 2. Serveur HTTP pour Prometheus sur port 8080
        try {
            HTTPServer metricsServer = new HTTPServer(METRICS_PORT);
            System.out.println("📊 Métriques Prometheus disponibles sur le port " + METRICS_PORT);
        } catch (IOException e) {
            System.err.println("⚠️ Impossible de démarrer le serveur de métriques : " + e.getMessage());
        }

        // 3. Hibernate + Scheduler
        HibernateUtil.getSessionFactory();
        DeadlineScheduler.start();

        System.out.println("🚀 Serveur Kanban démarré sur le port " + PORT);

        // 4. Serveur socket principal
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("✅ Nouveau client connecté : "
                        + clientSocket.getInetAddress());

                // Incrémenter les métriques
                connectionsTotal.inc();
                activeConnections.inc();

                ClientHandler handler = new ClientHandler(clientSocket);
                Thread thread = new Thread(() -> {
                    try {
                        handler.run();
                    } finally {
                        activeConnections.dec(); // décrémenter à la déconnexion
                    }
                });
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur serveur : " + e.getMessage());
        }
    }
}