package net.pistonmaster.pistonmotd.bukkit;

import io.papermc.lib.PaperLib;
import net.kyori.adventure.text.Component;
import net.pistonmaster.pistonmotd.api.PlaceholderUtil;
import net.pistonmaster.pistonmotd.utils.MOTDUtil;
import net.pistonmaster.pistonmotd.utils.PistonSerializers;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PingEventSpigot implements Listener {
    private final PistonMOTDBukkit plugin;

    protected PingEventSpigot(PistonMOTDBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        if (plugin.getConfig().getBoolean("motd.activated")) {
            List<String> motd = plugin.getConfig().getStringList("motd.text");

            boolean supportsHex = PaperLib.getMinecraftVersion() >= 16;
            Component motdComponent = PistonSerializers.unusualSectionRGB.deserialize(MOTDUtil.getMOTD(motd, supportsHex, PlaceholderUtil::parseText));

            if (supportsHex) {
                event.setMotd(PistonSerializers.unusualSectionRGB.serialize(motdComponent));
            } else {
                event.setMotd(PistonSerializers.section.serialize(motdComponent));
            }
        }

        if (plugin.getConfig().getBoolean("overridemax.activated")) {
            event.setMaxPlayers(plugin.getConfig().getInt("overridemax.value"));
        }

        if (plugin.getConfig().getBoolean("icons")) {
            event.setServerIcon(plugin.favicons.get(plugin.random.nextInt(0, plugin.favicons.size())));
        }
    }
}
