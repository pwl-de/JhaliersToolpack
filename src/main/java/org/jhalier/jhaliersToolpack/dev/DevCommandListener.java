package org.jhalier.jhaliersToolpack.dev;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DevCommandListener implements Listener {

    private static final Set<UUID> FROZEN_PLAYERS =
            new HashSet<>();

    /*

     * FREEZE

     */

    public static void freeze(
            Player player
    ) {

        FROZEN_PLAYERS.add(
                player.getUniqueId()
        );

        player.setFreezeTicks(
                Math.max(
                        player.getFreezeTicks(),
                        140
                )
        );
    }

    /*

     * UNFREEZE

     */

    public static void unfreeze(
            Player player
    ) {

        FROZEN_PLAYERS.remove(
                player.getUniqueId()
        );

        player.setFreezeTicks(0);
    }

    /*

     * STATUS

     */

    public static boolean isFrozen(
            Player player
    ) {

        return FROZEN_PLAYERS.contains(
                player.getUniqueId()
        );
    }

    /*

     * MOVEMENT

     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onPlayerMove(
            PlayerMoveEvent event
    ) {

        Player player =
                event.getPlayer();

        if (!isFrozen(player)) {
            return;
        }

        if (event.getTo() == null) {
            return;
        }

        /*
         * Rotation erlauben.
         * Position blockieren.
         */
        if (event.getFrom().getX()
                != event.getTo().getX()
                || event.getFrom().getY()
                != event.getTo().getY()
                || event.getFrom().getZ()
                != event.getTo().getZ()) {

            event.setTo(
                    event.getFrom()
            );
        }
    }

    /*

     * CLEANUP

     */

    public static void clear() {

        FROZEN_PLAYERS.clear();
    }
}