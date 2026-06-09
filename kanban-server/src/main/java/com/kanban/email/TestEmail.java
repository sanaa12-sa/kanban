package com.kanban.email;

public class TestEmail {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("📧 Test des emails en cours...");

        // ── Test 1 : Email de bienvenue ──
        System.out.println("1️⃣ Envoi email de bienvenue...");
        EmailNotificationService.sendWelcomeEmail(
                "fanirisanaa@gmail.com",  // ⚠️ Mets ton email ici
                "Sanaa Test"
        );

        // ── Test 2 : Email d'assignation ──
        System.out.println("2️⃣ Envoi email d'assignation...");
        EmailNotificationService.sendTaskAssignedEmail(
                "fanirisanaa@gmail.com",  // ⚠️ Mets ton email ici
                "Rédiger le cahier des charges",
                "Rédiger le document complet du projet",
                "HIGH",
                "2026-05-10",
                "Projet PFE",
                "Sanaa"
        );

        // ── Test 3 : Email de rappel deadline ──
        System.out.println("3️⃣ Envoi email de rappel...");
        EmailNotificationService.sendDeadlineReminderEmail(
                "fanirisanaa@gmail.com",  // ⚠️ Mets ton email ici
                "Rédiger le cahier des charges",
                "2026-05-10",
                "Projet PFE"
        );

        // Attendre que les threads terminent
        System.out.println("⏳ Attente envoi...");
        Thread.sleep(5000);

        System.out.println("✅ Tests terminés ! Vérifie ta boîte mail.");
        EmailNotificationService.shutdown();
    }
}