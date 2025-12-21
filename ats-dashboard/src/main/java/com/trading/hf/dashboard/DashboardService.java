package com.trading.hf.dashboard;

import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

public class DashboardService {
    private Javalin app;
    private final Set<WsContext> sessions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final AtomicReference<String> lastMessage = new AtomicReference<>();

    public void start() {
        app = Javalin.create(config -> {
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
                System.out.println("Dashboard client connected.");
                // Immediately send the last known message to the new client
                if (lastMessage.get() != null) {
                    try {
                        ctx.send(lastMessage.get());
                    } catch (Exception e) {
                        System.err.println("Failed to send last message to newly connected client: " + e.getMessage());
                    }
                }
            });
            ws.onClose(ctx -> {
                sessions.remove(ctx);
                System.out.println("Dashboard client disconnected.");
            });
            ws.onError(ctx -> {
                sessions.remove(ctx);
                System.err.println("Dashboard client error: " + ctx.error());
            });
        });
        System.out.println("Dashboard service started on port 7070.");
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }

    public void broadcast(String message) {
        lastMessage.set(message); // Cache the latest message
        sessions.forEach(session -> {
            if (session.session.isOpen()) {
                try {
                    session.send(message);
                } catch (Exception e) {
                    System.err.println("Failed to send message to client, removing session: " + e.getMessage());
                    // Optionally remove the session here if an error occurs
                    // sessions.remove(session);
                }
            }
        });
    }
}
