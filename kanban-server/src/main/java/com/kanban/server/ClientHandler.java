package com.kanban.server;

import com.kanban.handler.RequestHandler;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private int userId = -1;
    private int currentBoardId = -1;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        } catch (IOException e) {
            System.err.println("❌ Erreur initialisation client : " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("📩 Reçu : " + message);
                String response = RequestHandler.handle(message, this);
                sendMessage(response);
            }
        } catch (IOException e) {
            System.out.println("🔌 Client déconnecté : " + socket.getInetAddress());
        } finally {
            disconnect();
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void disconnect() {
        try {
            if (userId != -1) {
                SessionManager.removeSession(userId); // ✅ correct
            }
            if (currentBoardId != -1) {
                BoardBroadcaster.removeClient(currentBoardId, this);
            }
            socket.close();
        } catch (IOException e) {
            System.err.println("❌ Erreur déconnexion : " + e.getMessage());
        }
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getCurrentBoardId() { return currentBoardId; }
    public void setCurrentBoardId(int currentBoardId) { this.currentBoardId = currentBoardId; }
    public Socket getSocket() { return socket; }
}