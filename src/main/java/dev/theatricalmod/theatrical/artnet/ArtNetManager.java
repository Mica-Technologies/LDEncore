/*
 * LDEncore -- Mica Technologies' fork of Theatrical, ported to Forge 1.12.2.
 *
 * Part of the 1.12.2 port. Written by Mica Technologies after upstream's
 * artnet/ArtNetManager.java (Theatrical Team, Apache License 2.0); the upstream file targets
 * Forge 1.16 and this is its 1.12.2 counterpart, not a copy.
 *
 * CHANGED FROM UPSTREAM:
 *   - The map is synchronised and getClient never returns a half-started client. Upstream used
 *     a plain HashMap that the client thread and the tile-entity tick both touched, and its
 *     containsKey/get pair could hand out a client another thread was still creating.
 *   - A client whose socket fails to bind no longer poisons the address. Upstream stored the
 *     client before starting it, so once a bind failed every later read on that address got a
 *     dead client back for the rest of the session; the failure is now logged once, the entry
 *     dropped, and a later attempt can succeed (the interface is often configured before the
 *     network it names exists).
 *   - shutdownAll tolerates a client that throws on stop, so one bad socket cannot stop the
 *     rest being closed on the way out.
 *   - A blank address, "0.0.0.0" or "all" binds every local interface. Upstream always bound the
 *     one address typed into the interface, and a socket bound to a single address does not
 *     receive packets sent to the broadcast address on most systems -- which is how a real
 *     lighting console emits Art-Net. Listening on all interfaces is what makes a physical desk
 *     on the same network work at all, and it is the new default for a freshly placed interface.
 *
 * Upstream's ArtNetThread is not carried over: nothing referenced it, its running flag was a
 * constant false, and ArtNetClient.start already runs its own receive thread.
 */
package dev.theatricalmod.theatrical.artnet;

import ch.bildspur.artnet.ArtNetClient;
import dev.theatricalmod.theatrical.TheatricalMod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** One running Art-Net client per address the world's interfaces listen on. */
public class ArtNetManager {

    private final Map<String, ArtNetClient> clients = new HashMap<>();
    /** Addresses that already failed to bind, so the failure is logged once rather than per tick. */
    private final Set<String> failed = new HashSet<>();

    /** Whether this address means "every local interface" rather than one in particular. */
    public static boolean isAllInterfaces(String ip) {
        return ip == null || ip.trim().isEmpty() || "0.0.0.0".equals(ip.trim()) || "all".equalsIgnoreCase(ip.trim());
    }

    /** Whether a client is currently listening on this address. Drives the screen's status line. */
    public synchronized boolean isListening(String ip) {
        ArtNetClient client = clients.get(ip);
        return client != null && client.isRunning();
    }

    /**
     * The client listening on {@code ip}, started if this is the first ask. Returns null when
     * the address cannot be bound; callers must cope, because the address is player-supplied.
     */
    @Nullable
    public synchronized ArtNetClient getClient(String ip) {
        if (ip == null) {
            return null;
        }
        ArtNetClient existing = clients.get(ip);
        if (existing != null) {
            return existing;
        }
        if (failed.contains(ip)) {
            return null;
        }
        ArtNetClient client = new ArtNetClient();
        try {
            if (isAllInterfaces(ip)) {
                client.start();
            } else {
                client.start(ip);
            }
        } catch (Exception e) {
            failed.add(ip);
            TheatricalMod.LOGGER.warn("Could not start an Art-Net client on {}: {}", ip, e.toString());
            return null;
        }
        clients.put(ip, client);
        return client;
    }

    /** Lets an address be retried after it failed, for a player who has fixed their setup. */
    public synchronized void clearFailures() {
        failed.clear();
    }

    public synchronized void shutdownAll() {
        Collection<ArtNetClient> running = new ArrayList<>(clients.values());
        clients.clear();
        failed.clear();
        for (ArtNetClient client : running) {
            try {
                client.stop();
            } catch (Exception e) {
                TheatricalMod.LOGGER.warn("An Art-Net client did not shut down cleanly: {}", e.toString());
            }
        }
    }
}
