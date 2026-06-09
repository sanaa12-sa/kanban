package com.kanban;

import com.kanban.server.ClientHandler;
import java.io.*;
import java.net.Socket;

/**
 * Faux ClientHandler — utilise un socket piped (pas de réseau réel)
 */
public class MockClientHandler extends ClientHandler {

    private int userId;
    private int currentBoardId = 1;

    public MockClientHandler(int userId) {
        super(createFakeSocket());
        this.userId = userId;
    }

    /**
     * Crée un socket factice avec des streams valides
     */
    private static Socket createFakeSocket() {
        try {
            // PipedStream : fournit des streams valides sans réseau
            PipedInputStream  pis = new PipedInputStream();
            PipedOutputStream pos = new PipedOutputStream(pis);

            Socket fake = new Socket() {
                @Override
                public InputStream getInputStream() { return pis; }
                @Override
                public OutputStream getOutputStream() { return pos; }
                @Override
                public boolean isClosed() { return false; }
                @Override
                public synchronized void close() { /* rien */ }
            };
            return fake;
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le socket fake", e);
        }
    }

    @Override public int getUserId()                    { return userId; }
    @Override public void setUserId(int id)             { this.userId = id; }
    @Override public int getCurrentBoardId()            { return currentBoardId; }
    @Override public void setCurrentBoardId(int id)     { this.currentBoardId = id; }
    @Override public void sendMessage(String message)   { /* rien */ }
    @Override public void disconnect()                  { /* rien */ }
}