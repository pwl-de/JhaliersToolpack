package org.jhalier.jhaliersToolpack.dev;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DevCommandExecutor implements CommandExecutor {

    private final org.jhalier.jhaliersToolpack.JhaliersToolpack plugin;

    /*
     * ==========================================
     * PERFORMANCE
     * ==========================================
     */

    private long lastTickNanos = System.nanoTime();

    private final List<Double> tickSamples =
            new ArrayList<>();

    /*
     * 18.000 Samples = ca. 15 Minuten bei 20 TPS.
     */
    private static final int MAX_TICK_SAMPLES = 18000;

    public DevCommandExecutor(
            org.jhalier.jhaliersToolpack.JhaliersToolpack plugin
    ) {

        this.plugin = plugin;
    }

    /*
     * ==========================================
     * COMMAND
     * ==========================================
     */

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        /*
         * Nur Spieler.
         */
        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    ChatColor.RED
                            + "[Dev] Dieser Befehl kann nur von einem Spieler verwendet werden."
            );

            return true;
        }

        /*
         * Nur OPs.
         */
        if (!player.isOp()) {

            player.sendMessage(
                    ChatColor.RED
                            + "[Dev] Keine Berechtigung."
            );

            return true;
        }

        /*
         * /dev
         * /dev help
         */
        if (args.length == 0
                || args[0].equalsIgnoreCase("help")) {

            sendHelp(player);
            return true;
        }

        String subCommand =
                args[0].toLowerCase(Locale.ROOT);

        String[] subArgs =
                new String[Math.max(0, args.length - 1)];

        if (args.length > 1) {

            System.arraycopy(
                    args,
                    1,
                    subArgs,
                    0,
                    args.length - 1
            );
        }

        switch (subCommand) {

            case "playerinfo":
            case "info":
                handlePlayerInfo(player, subArgs);
                return true;

            case "perf":
            case "performance":
                handlePerf(player);
                return true;

            case "freeze":
                handleFreeze(player, subArgs);
                return true;

            case "unfreeze":
            case "unfreezeplayer":
                handleUnfreeze(player, subArgs);
                return true;

            case "ping":
                handlePing(player, subArgs);
                return true;

            case "pingall":
                handlePingAll(player);
                return true;

            default:

                player.sendMessage(
                        ChatColor.RED
                                + "[Dev] Unbekannter Befehl: "
                                + args[0]
                );

                player.sendMessage(
                        ChatColor.GRAY
                                + "Nutze "
                                + ChatColor.WHITE
                                + "/dev help"
                                + ChatColor.GRAY
                                + "."
                );

                return true;
        }
    }

    /*
     * ==========================================
     * /dev help
     * ==========================================
     */

    private void sendHelp(
            Player player
    ) {

        player.sendMessage(
                ChatColor.DARK_GRAY
                        + "========== "
                        + ChatColor.GOLD
                        + "DEV COMMANDS"
                        + ChatColor.DARK_GRAY
                        + " =========="
        );

        sendCommandHelp(
                player,
                "/dev help",
                "Dieses Menü"
        );

        sendCommandHelp(
                player,
                "/dev playerinfo [spieler]",
                "Spielerinformationen"
        );

        sendCommandHelp(
                player,
                "/dev perf",
                "Server Performance"
        );

        sendCommandHelp(
                player,
                "/dev ping [spieler]",
                "Spieler-Ping"
        );

        sendCommandHelp(
                player,
                "/dev pingall",
                "Pings aller Spieler"
        );

        sendCommandHelp(
                player,
                "/dev freeze <spieler>",
                "Spieler einfrieren"
        );

        sendCommandHelp(
                player,
                "/dev unfreeze <spieler>",
                "Spieler freigeben"
        );

        player.sendMessage(
                ChatColor.DARK_GRAY
                        + "========================================"
        );
    }

    private void sendCommandHelp(
            Player player,
            String command,
            String description
    ) {

        player.sendMessage(
                ChatColor.GOLD
                        + command
                        + ChatColor.DARK_GRAY
                        + " - "
                        + ChatColor.GRAY
                        + description
        );
    }

    /*
     * ==========================================
     * /dev ping
     * ==========================================
     */

    private void handlePing(
            Player sender,
            String[] args
    ) {

        Player target;

        /*
         * /dev ping
         */
        if (args.length == 0) {

            target = sender;

            /*
             * /dev ping Jhailier
             */
        } else if (args.length == 1) {

            target =
                    Bukkit.getPlayerExact(
                            args[0]
                    );

            if (target == null) {

                sender.sendMessage(
                        ChatColor.RED
                                + "[Dev] Spieler ist nicht online: "
                                + args[0]
                );

                return;
            }

        } else {

            sender.sendMessage(
                    ChatColor.RED
                            + "Verwendung: /dev ping [spieler]"
            );

            return;
        }

        int ping =
                Math.max(
                        0,
                        target.getPing()
                );

        sender.sendMessage(
                ChatColor.DARK_GRAY
                        + "========== "
                        + ChatColor.GOLD
                        + "PLAYER PING"
                        + ChatColor.DARK_GRAY
                        + " =========="
        );

        sendInfo(
                sender,
                "Player",
                target.getName()
        );

        sendInfo(
                sender,
                "Ping",
                ping + "ms"
        );

        sender.sendMessage(
                ChatColor.DARK_GRAY
                        + "========================================"
        );
    }

    /*
     * ==========================================
     * /dev pingall
     * ==========================================
     */

    private void handlePingAll(
            Player sender
    ) {

        List<Player> players =
                new ArrayList<>(
                        Bukkit.getOnlinePlayers()
                );

        /*
         * Nach Ping sortieren.
         * Niedrigster Ping zuerst.
         */
        players.sort(
                Comparator.comparingInt(
                        player -> Math.max(
                                0,
                                player.getPing()
                        )
                )
        );

        sender.sendMessage(
                ChatColor.DARK_GRAY
                        + "========== "
                        + ChatColor.GOLD
                        + "PLAYER PINGS"
                        + ChatColor.DARK_GRAY
                        + " =========="
        );

        if (players.isEmpty()) {

            sender.sendMessage(
                    ChatColor.GRAY
                            + "Keine Spieler online."
            );

        } else {

            sender.sendMessage("");

            for (Player player : players) {

                int ping =
                        Math.max(
                                0,
                                player.getPing()
                        );

                sender.sendMessage(
                        ChatColor.WHITE
                                + String.format(
                                Locale.ROOT,
                                "%-18s",
                                player.getName()
                        )
                                + ChatColor.GRAY
                                + ping
                                + "ms"
                );
            }

            sender.sendMessage("");
        }

        sender.sendMessage(
                ChatColor.DARK_GRAY
                        + "========================================"
        );
    }

    /*
     * ==========================================
     * /dev playerinfo
     * ==========================================
     */

    private void handlePlayerInfo(
            Player sender,
            String[] args
    ) {

        Player target;

        if (args.length == 0) {

            target = sender;

        } else if (args.length == 1) {

            target =
                    Bukkit.getPlayerExact(
                            args[0]
                    );

            if (target == null) {

                sender.sendMessage(
                        ChatColor.RED
                                + "[Dev] Spieler ist nicht online: "
                                + args[0]
                );

                return;
            }

        } else {

            sender.sendMessage(
                    ChatColor.RED
                            + "Verwendung: /dev playerinfo [spieler]"
            );

            return;
        }

        sendPlayerInfo(
                sender,
                target
        );
    }

    private void sendPlayerInfo(
            Player sender,
            Player target
    ) {

        sender.sendMessage(
                ChatColor.DARK_GRAY
                        + "========== "
                        + ChatColor.GOLD
                        + "PLAYER INFO"
                        + ChatColor.DARK_GRAY
                        + " =========="
        );

        sendInfo(
                sender,
                "Name",
                target.getName()
        );

        sendInfo(
                sender,
                "UUID",
                target.getUniqueId().toString()
        );

        sendInfo(
                sender,
                "World",
                target.getWorld().getName()
        );

        sendInfo(
                sender,
                "Position",
                String.format(
                        Locale.US,
                        "%.2f / %.2f / %.2f",
                        target.getLocation().getX(),
                        target.getLocation().getY(),
                        target.getLocation().getZ()
                )
        );

        sendInfo(
                sender,
                "Gamemode",
                target.getGameMode().name()
        );

        sendInfo(
                sender,
                "Health",
                String.format(
                        Locale.US,
                        "%.1f / %.1f",
                        target.getHealth(),
                        target.getMaxHealth()
                )
        );

        sendInfo(
                sender,
                "Food",
                target.getFoodLevel()
                        + " / 20"
        );

        sendInfo(
                sender,
                "Level",
                String.valueOf(
                        target.getLevel()
                )
        );

        sendInfo(
                sender,
                "XP",
                String.format(
                        Locale.US,
                        "%.1f%%",
                        target.getExp() * 100.0
                )
        );

        sendInfo(
                sender,
                "Ping",
                target.getPing() + "ms"
        );

        sendInfo(
                sender,
                "OP",
                String.valueOf(
                        target.isOp()
                )
        );

        sendInfo(
                sender,
                "Whitelisted",
                String.valueOf(
                        target.isWhitelisted()
                )
        );

        BanList banList =
                Bukkit.getBanList(
                        BanList.Type.NAME
                );

        sendInfo(
                sender,
                "Banned",
                String.valueOf(
                        banList.isBanned(
                                target.getName()
                        )
                )
        );

        sendInfo(
                sender,
                "Online",
                "true"
        );

        /*
         * NETWORK
         */

        String ip =
                target.getAddress() != null
                        ? target.getAddress()
                        .getAddress()
                        .getHostAddress()
                        : "unknown";

        sendInfo(
                sender,
                "IP",
                ip
        );

        /*
         * CLIENT
         */

        sendInfo(
                sender,
                "Locale",
                target.getLocale()
        );

        sendInfo(
                sender,
                "Client Brand",
                getClientBrand(target)
        );

        sendInfo(
                sender,
                "Protocol Version",
                getProtocolVersion(target)
        );

        /*
         * PERMISSIONS
         */

        sendInfo(
                sender,
                "Permissions",
                getPermissionSummary(target)
        );

        /*
         * POTION EFFECTS
         */

        sendInfo(
                sender,
                "Potion Effects",
                getPotionEffects(target)
        );

        /*
         * MOVEMENT
         */

        sendInfo(
                sender,
                "Velocity",
                String.format(
                        Locale.US,
                        "%.3f / %.3f / %.3f",
                        target.getVelocity().getX(),
                        target.getVelocity().getY(),
                        target.getVelocity().getZ()
                )
        );

        sendInfo(
                sender,
                "Flight",
                String.valueOf(
                        target.isFlying()
                )
        );

        sendInfo(
                sender,
                "AllowFlight",
                String.valueOf(
                        target.getAllowFlight()
                )
        );

        sendInfo(
                sender,
                "FireTicks",
                String.valueOf(
                        target.getFireTicks()
                )
        );

        sendInfo(
                sender,
                "FreezeTicks",
                String.valueOf(
                        target.getFreezeTicks()
                )
        );

        sendInfo(
                sender,
                "Frozen by Dev",
                String.valueOf(
                        DevCommandListener.isFrozen(target)
                )
        );

        /*
         * ADDITIONAL
         */

        sendInfo(
                sender,
                "Air",
                target.getRemainingAir()
                        + " / "
                        + target.getMaximumAir()
        );

        sendInfo(
                sender,
                "On Ground",
                String.valueOf(
                        target.isOnGround()
                )
        );

        sendInfo(
                sender,
                "Sneaking",
                String.valueOf(
                        target.isSneaking()
                )
        );

        sendInfo(
                sender,
                "Sprinting",
                String.valueOf(
                        target.isSprinting()
                )
        );

        sendInfo(
                sender,
                "Swimming",
                String.valueOf(
                        target.isSwimming()
                )
        );

        sendInfo(
                sender,
                "Gliding",
                String.valueOf(
                        target.isGliding()
                )
        );

        sendInfo(
                sender,
                "Vehicle",
                target.getVehicle() == null
                        ? "none"
                        : target.getVehicle()
                        .getType()
                        .name()
        );

        sendInfo(
                sender,
                "World Time",
                String.valueOf(
                        target.getWorld().getTime()
                )
        );

        sendInfo(
                sender,
                "World Difficulty",
                target.getWorld()
                        .getDifficulty()
                        .name()
        );

        sender.sendMessage(
                ChatColor.DARK_GRAY
                        + "========================================"
        );
    }

    /*
     * ==========================================
     * /dev perf
     * ==========================================
     */

    private void handlePerf(
            Player player
    ) {

        PerformanceSnapshot snapshot =
                createPerformanceSnapshot();

        player.sendMessage(
                ChatColor.DARK_GRAY
                        + "========== "
                        + ChatColor.GOLD
                        + "SERVER PERFORMANCE"
                        + ChatColor.DARK_GRAY
                        + " =========="
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "TPS"
        );

        sendInfo(
                player,
                "1m",
                formatTps(snapshot.tps1m)
        );

        sendInfo(
                player,
                "5m",
                formatTps(snapshot.tps5m)
        );

        sendInfo(
                player,
                "15m",
                formatTps(snapshot.tps15m)
        );

        player.sendMessage("");

        player.sendMessage(
                ChatColor.YELLOW
                        + "MSPT"
        );

        sendInfo(
                player,
                "Min",
                formatMspt(snapshot.minMspt)
        );

        sendInfo(
                player,
                "Avg",
                formatMspt(snapshot.avgMspt)
        );

        sendInfo(
                player,
                "Max",
                formatMspt(snapshot.maxMspt)
        );

        player.sendMessage("");

        sendInfo(
                player,
                "Players",
                String.valueOf(
                        Bukkit.getOnlinePlayers().size()
                )
        );

        int entities = 0;
        int chunks = 0;

        for (World world : Bukkit.getWorlds()) {

            entities +=
                    world.getEntities().size();

            chunks +=
                    world.getLoadedChunks().length;
        }

        sendInfo(
                player,
                "Entities",
                String.valueOf(
                        entities
                )
        );

        sendInfo(
                player,
                "Chunks loaded",
                String.valueOf(
                        chunks
                )
        );

        player.sendMessage("");

        player.sendMessage(
                ChatColor.YELLOW
                        + "Worlds"
        );

        for (World world : Bukkit.getWorlds()) {

            player.sendMessage(
                    ChatColor.GRAY
                            + String.format(
                            Locale.ROOT,
                            "%-18s",
                            world.getName()
                    )
                            + ChatColor.WHITE
                            + world.getEntities().size()
                            + " entities"
            );
        }

        player.sendMessage("");

        /*
         * MEMORY
         */

        MemoryMXBean memory =
                ManagementFactory
                        .getMemoryMXBean();

        MemoryUsage heap =
                memory.getHeapMemoryUsage();

        player.sendMessage(
                ChatColor.YELLOW
                        + "Memory"
        );

        sendInfo(
                player,
                "Used",
                formatBytes(heap.getUsed())
        );

        sendInfo(
                player,
                "Max",
                formatBytes(heap.getMax())
        );

        sendInfo(
                player,
                "Free",
                formatBytes(
                        heap.getMax()
                                - heap.getUsed()
                )
        );

        player.sendMessage(
                ChatColor.DARK_GRAY
                        + "========================================"
        );
    }

    /*
     * ==========================================
     * PERFORMANCE SNAPSHOT
     * ==========================================
     */

    private PerformanceSnapshot createPerformanceSnapshot() {

        synchronized (tickSamples) {

            if (tickSamples.isEmpty()) {

                return new PerformanceSnapshot(
                        20.0,
                        20.0,
                        20.0,
                        50.0,
                        50.0,
                        50.0
                );
            }

            double oneMinute =
                    averageLastTicks(1200);

            double fiveMinutes =
                    averageLastTicks(6000);

            double fifteenMinutes =
                    averageLastTicks(18000);

            double min =
                    tickSamples.stream()
                            .mapToDouble(
                                    value -> value
                            )
                            .min()
                            .orElse(50.0);

            double avg =
                    tickSamples.stream()
                            .mapToDouble(
                                    value -> value
                            )
                            .average()
                            .orElse(50.0);

            double max =
                    tickSamples.stream()
                            .mapToDouble(
                                    value -> value
                            )
                            .max()
                            .orElse(50.0);

            return new PerformanceSnapshot(
                    msptToTps(oneMinute),
                    msptToTps(fiveMinutes),
                    msptToTps(fifteenMinutes),
                    min,
                    avg,
                    max
            );
        }
    }

    private double averageLastTicks(
            int count
    ) {

        int from =
                Math.max(
                        0,
                        tickSamples.size() - count
                );

        return tickSamples
                .subList(
                        from,
                        tickSamples.size()
                )
                .stream()
                .mapToDouble(
                        value -> value
                )
                .average()
                .orElse(50.0);
    }

    private double msptToTps(
            double mspt
    ) {

        if (mspt <= 0) {
            return 20.0;
        }

        return Math.min(
                20.0,
                1000.0 / mspt
        );
    }

    /*
     * ==========================================
     * TICK MONITOR
     * ==========================================
     */

    public void recordTick() {

        long now =
                System.nanoTime();

        double mspt =
                (now - lastTickNanos)
                        / 1_000_000.0;

        lastTickNanos = now;

        if (mspt <= 0
                || mspt > 5000) {

            return;
        }

        synchronized (tickSamples) {

            tickSamples.add(mspt);

            if (tickSamples.size()
                    > MAX_TICK_SAMPLES) {

                tickSamples.remove(0);
            }
        }
    }

    /*
     * ==========================================
     * /dev freeze
     * ==========================================
     */

    private void handleFreeze(
            Player sender,
            String[] args
    ) {

        if (args.length != 1) {

            sender.sendMessage(
                    ChatColor.RED
                            + "Verwendung: /dev freeze <spieler>"
            );

            return;
        }

        Player target =
                Bukkit.getPlayerExact(
                        args[0]
                );

        if (target == null) {

            sender.sendMessage(
                    ChatColor.RED
                            + "[Dev] Spieler ist nicht online."
            );

            return;
        }

        if (DevCommandListener.isFrozen(target)) {

            sender.sendMessage(
                    ChatColor.YELLOW
                            + "[Dev] Spieler ist bereits eingefroren."
            );

            return;
        }

        DevCommandListener.freeze(target);

        sender.sendMessage(
                ChatColor.GREEN
                        + "[Dev] "
                        + ChatColor.WHITE
                        + target.getName()
                        + ChatColor.GREEN
                        + " wurde eingefroren."
        );

        target.sendMessage(
                ChatColor.RED
                        + "[Dev] Du wurdest eingefroren."
        );
    }

    /*
     * ==========================================
     * /dev unfreeze
     * ==========================================
     */

    private void handleUnfreeze(
            Player sender,
            String[] args
    ) {

        if (args.length != 1) {

            sender.sendMessage(
                    ChatColor.RED
                            + "Verwendung: /dev unfreeze <spieler>"
            );

            return;
        }

        Player target =
                Bukkit.getPlayerExact(
                        args[0]
                );

        if (target == null) {

            sender.sendMessage(
                    ChatColor.RED
                            + "[Dev] Spieler ist nicht online."
            );

            return;
        }

        if (!DevCommandListener.isFrozen(target)) {

            sender.sendMessage(
                    ChatColor.YELLOW
                            + "[Dev] Spieler ist nicht eingefroren."
            );

            return;
        }

        DevCommandListener.unfreeze(target);

        sender.sendMessage(
                ChatColor.GREEN
                        + "[Dev] "
                        + ChatColor.WHITE
                        + target.getName()
                        + ChatColor.GREEN
                        + " wurde freigegeben."
        );

        target.sendMessage(
                ChatColor.GREEN
                        + "[Dev] Du bist nicht mehr eingefroren."
        );
    }

    /*
     * ==========================================
     * INFO HELPER
     * ==========================================
     */

    private void sendInfo(
            Player player,
            String name,
            String value
    ) {

        player.sendMessage(
                ChatColor.GRAY
                        + String.format(
                        Locale.ROOT,
                        "%-18s",
                        name + ":"
                )
                        + ChatColor.WHITE
                        + value
        );
    }

    /*
     * ==========================================
     * CLIENT BRAND
     * ==========================================
     */

    private String getClientBrand(
            Player player
    ) {

        try {

            String brand =
                    player.getClientBrandName();

            return brand == null
                    ? "unknown"
                    : brand;

        } catch (Throwable ignored) {

            return "unknown";
        }
    }

    /*
     * ==========================================
     * PROTOCOL
     * ==========================================
     */

    private String getProtocolVersion(
            Player player
    ) {

        try {

            java.lang.reflect.Method method =
                    player.getClass()
                            .getMethod(
                                    "getProtocolVersion"
                            );

            Object value =
                    method.invoke(player);

            return String.valueOf(value);

        } catch (Throwable ignored) {

            return "unknown";
        }
    }

    /*
     * ==========================================
     * PERMISSIONS
     * ==========================================
     */

    private String getPermissionSummary(
            Player player
    ) {

        int effective = 0;
        int total = 0;

        for (
                org.bukkit.permissions.PermissionAttachmentInfo permission :
                player.getEffectivePermissions()
        ) {

            total++;

            if (permission.getValue()) {
                effective++;
            }
        }

        return effective
                + " active / "
                + total
                + " total";
    }

    /*
     * ==========================================
     * POTION EFFECTS
     * ==========================================
     */

    private String getPotionEffects(
            Player player
    ) {

        if (player.getActivePotionEffects().isEmpty()) {

            return "none";
        }

        StringBuilder result =
                new StringBuilder();

        for (
                PotionEffect effect :
                player.getActivePotionEffects()
        ) {

            if (result.length() > 0) {
                result.append(", ");
            }

            result.append(
                    effect.getType().getName()
            );

            result.append(" ");
            result.append(
                    effect.getAmplifier() + 1
            );

            result.append(
                    " ("
            );

            result.append(
                    effect.getDuration()
            );

            result.append(
                    "t)"
            );
        }

        return result.toString();
    }

    /*
     * ==========================================
     * FORMAT
     * ==========================================
     */

    private String formatTps(
            double tps
    ) {

        return String.format(
                Locale.US,
                "%.2f",
                tps
        );
    }

    private String formatMspt(
            double mspt
    ) {

        return String.format(
                Locale.US,
                "%.1f ms",
                mspt
        );
    }

    private String formatBytes(
            long bytes
    ) {

        if (bytes < 1024) {

            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {

            return String.format(
                    Locale.US,
                    "%.1f KB",
                    bytes / 1024.0
            );
        }

        if (bytes < 1024L * 1024L * 1024L) {

            return String.format(
                    Locale.US,
                    "%.1f MB",
                    bytes / 1024.0 / 1024.0
            );
        }

        return String.format(
                Locale.US,
                "%.2f GB",
                bytes / 1024.0 / 1024.0 / 1024.0
        );
    }

    /*
     * ==========================================
     * SNAPSHOT
     * ==========================================
     */

    private record PerformanceSnapshot(
            double tps1m,
            double tps5m,
            double tps15m,
            double minMspt,
            double avgMspt,
            double maxMspt
    ) {
    }
}