package org.jhalier.jhaliersToolpack;

import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public class JhaliersToolpack extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(
                new ConsoleCommandListener(this),
                this
        );

        getLogger().info("JhaliersToolpack wurde aktiviert!");
    }

    @Override
    public void onDisable() {
        getLogger().info("JhaliersToolpack wurde deaktiviert!");
    }

    public Path getPluginJar() {
        return getFile().toPath();
    }
}