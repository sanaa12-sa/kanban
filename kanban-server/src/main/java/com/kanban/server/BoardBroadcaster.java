package com.kanban.server;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class BoardBroadcaster {

    // Map : boardId → liste des clients connectés à ce board
    private static final ConcurrentHashMap<Integer, CopyOnWriteArrayList<ClientHandler>> boardClients
            = new ConcurrentHashMap<>();

    // Ajouter un client à un board
    public static void addClient(int boardId, ClientHandler client) {
        boardClients.computeIfAbsent(boardId, k -> new CopyOnWriteArrayList<>()).add(client);
        System.out.println("👤 Client ajouté au board " + boardId);
    }

    // Retirer un client d'un board
    public static void removeClient(int boardId, ClientHandler client) {
        CopyOnWriteArrayList<ClientHandler> clients = boardClients.get(boardId);
        if (clients != null) {
            clients.remove(client);
            System.out.println("👤 Client retiré du board " + boardId);
        }
    }

    // Envoyer un message à tous les clients du board SAUF l'émetteur
    public static void broadcast(int boardId, String message, ClientHandler sender) {
        CopyOnWriteArrayList<ClientHandler> clients = boardClients.get(boardId);
        if (clients != null) {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    // Envoyer un message à TOUS les clients du board
    public static void broadcastAll(int boardId, String message) {
        CopyOnWriteArrayList<ClientHandler> clients = boardClients.get(boardId);
        if (clients != null) {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    // Nombre de clients connectés à un board
    public static int getClientCount(int boardId) {
        CopyOnWriteArrayList<ClientHandler> clients = boardClients.get(boardId);
        return clients != null ? clients.size() : 0;
    }
}