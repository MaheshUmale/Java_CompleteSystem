package com.trading.hf.dashboard;

import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class DashboardService {
    private Javalin app;
    private final Set<WsContext> sessions = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void start() {
        app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(it -> it.anyHost()));
            config.staticFiles.add("/public");
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost();
                });
            });
        }).start(7070);

        app.ws("/data", ws -> {
            ws.onConnect(ctx -> {
                sessions.add(ctx);
                System.out.println("--- MINIMAL TEST --- Client connected successfully.");
            });
            ws.onClose(ctx -> {
                sessions.remove(ctx);
                System.out.println("--- MINIMAL TEST --- Client disconnected.");
            });
            ws.onError(ctx -> {
                sessions.remove(ctx);
                System.err.println("--- MINIMAL TEST --- Client error: " + ctx.error());
            });
        });
        System.out.println("--- MINIMAL TEST --- Dashboard service started on port 7070.");
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }

    public void broadcast(String message) {
        sessions.forEach(session -> {
            if (session.session.isOpen()) {
                try {
                    session.send(message);
                } catch (Exception e) {
                    System.err.println("--- MINIMAL TEST --- Failed to send message: " + e.getMessage());
                }
            }
        });
    }
}
