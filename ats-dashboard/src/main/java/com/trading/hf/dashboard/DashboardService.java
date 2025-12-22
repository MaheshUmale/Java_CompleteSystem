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
        }).start(7070);

        app.ws("/data", ws -> {
            ws.onConnect(ctx -> {
                sessions.add(ctx);
                System.out.println("Dashboard client connected.");
                ctx.enableAutomaticPings(); // Keep the connection alive
                // Use a CompletableFuture to send the initial message shortly after connection,
                // avoiding a race condition where the message is sent before the handshake is fully complete.
                CompletableFuture.runAsync(() -> {
                    try {
                        // A small delay to ensure the client-side is ready
                        Thread.sleep(100);
                        if (ctx.session.isOpen() && lastMessage.get() != null) {
                            ctx.send(lastMessage.get());
                        }
                    } catch (Exception e) {
                        // This can happen if the client disconnects before the message is sent, which is safe to ignore.
                        System.err.println("Could not send initial message to client: " + e.getMessage());
                    }
                });
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
