package com.evcharging;

import com.evcharging.ui.LoginFrame;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        // 1. Explicit Multithreading: Start a background monitoring worker thread
        Thread backgroundMonitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("[Thread Worker] Background system monitor thread started.");
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        // Periodic background check simulating slot health / status polling
                        Thread.sleep(30000); // Polls every 30 seconds
                        System.out.println("[Thread Worker] Polling EV station slot availability...");
                    } catch (InterruptedException e) {
                        System.out.println("[Thread Worker] Background thread interrupted.");
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }, "EV-Slot-Monitor-Thread");

        backgroundMonitorThread.setDaemon(true); // Allows JVM to shut down cleanly when main window closes
        backgroundMonitorThread.start();

        // 2. Set System Look & Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // 3. Launch Swing GUI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}