package net.pistonmaster.pistonmotd.bungee;

import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.pistonmaster.pistonmotd.api.PlaceholderUtil;
import net.pistonmaster.pistonmotd.utils.MOTDUtil;
import net.pistonmaster.pistonmotd.utils.PistonSerializers;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class PingEvent implements Listener {
    private final PistonMOTDBungee plugin;
    private final File iconFolder;
    private List<Favicon> favicons;
    private ThreadLocalRandom random;

    protected PingEvent(PistonMOTDBungee plugin, File icons) {
        this.plugin = plugin;
        this.iconFolder = icons;
        if (plugin.config.getBoolean("icons")) {
            favicons = loadFavicons();
            random = ThreadLocalRandom.current();
        }
    }

    @EventHandler
    public void onPing(ProxyPingEvent event) throws IOException {
        int online;
        int max;
        ServerPing.Players players;
        BaseComponent motd;
        ServerPing.Protocol protocol;
        final String afterIcon = "                                                                            ";
        Favicon icon;
        final Configuration config = plugin.config;

        if (config.getBoolean("overrideonline.activated")) {
            online = config.getInt("overrideonline.value");
        } else {
            online = event.getResponse().getPlayers().getOnline();
        }

        if (config.getBoolean("overridemax.activated")) {
            max = config.getInt("overridemax.value");
        } else {
            max = event.getResponse().getPlayers().getMax();
        }

        if (config.getBoolean("playercounter.bukkitplayercounter")) {
            List<ServerPing.PlayerInfo> info = new ArrayList<>();

            int i = 0;

            for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
                if (config.getStringList("hiddenplayers").contains(player.getName())) continue;

                info.add(new ServerPing.PlayerInfo(player.getDisplayName() + ChatColor.RESET, String.valueOf(i)));
                i++;
            }

            players = new ServerPing.Players(max, online, info.toArray(new ServerPing.PlayerInfo[0]));
        } else if (config.getBoolean("hooks.luckpermsplayercounter") && plugin.luckpermsWrapper != null) {
            List<ServerPing.PlayerInfo> info = new ArrayList<>();

            int i = 0;

            for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
                CachedMetaData metaData = plugin.luckpermsWrapper.luckperms.getPlayerAdapter(ProxiedPlayer.class).getMetaData(player);

                String prefix = metaData.getPrefix() == null ? "" : metaData.getPrefix();

                String suffix = metaData.getSuffix() == null ? "" : metaData.getSuffix();

                info.add(new ServerPing.PlayerInfo(ChatColor.translateAlternateColorCodes('&', prefix + player.getDisplayName() + suffix) + ChatColor.RESET, String.valueOf(i)));
                i++;
            }

            players = new ServerPing.Players(max, online, info.toArray(new ServerPing.PlayerInfo[0]));
        } else if (config.getBoolean("playercounter.activated")) {
            List<ServerPing.PlayerInfo> info = new ArrayList<>();

            int i = 0;

            for (String str : config.getStringList("playercounter.text")) {
                info.add(new ServerPing.PlayerInfo(PlaceholderUtil.parseText(str), String.valueOf(i)));
                i++;
            }

            players = new ServerPing.Players(max, online, info.toArray(new ServerPing.PlayerInfo[0]));
        } else {
            players = new ServerPing.Players(max, online, event.getResponse().getPlayers().getSample());
        }

        if (config.getBoolean("motd.activated")) {
            boolean supportsHex = event.getConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_16;
            String randomMotd = MOTDUtil.getMOTD(config.getStringList("motd.text"), supportsHex, PlaceholderUtil::parseText);

            if (supportsHex) {
                motd = new TextComponent(BungeeComponentSerializer.get().serialize(PistonSerializers.sectionRGB.deserialize(randomMotd)));
            } else {
                motd = new TextComponent(BungeeComponentSerializer.legacy().serialize(PistonSerializers.sectionRGB.deserialize(randomMotd)));
            }
        } else {
            motd = event.getResponse().getDescriptionComponent();
        }

        if (config.getBoolean("protocol.activated") || config.getBoolean("overrideprotocolnumber.activated")) {
            ServerPing.Protocol provided = event.getResponse().getVersion();

            if (config.getBoolean("protocol.activated")) {
                provided.setName(PlaceholderUtil.parseText(config.getString("protocol.text").replace("%aftericon%", afterIcon)));
            }

            if (config.getBoolean("overrideprotocolnumber.activated")) {
                provided.setProtocol(config.getInt("overrideprotocolnumber.value"));
            }

            protocol = provided;
        } else {
            protocol = event.getResponse().getVersion();
        }

        if (config.getBoolean("icons")) {
            icon = favicons.get(random.nextInt(0, favicons.size()));
        } else {
            icon = event.getResponse().getFaviconObject();
        }

        ServerPing ping = new ServerPing(protocol, players, motd, icon);
        event.setResponse(ping);
    }

    private List<Favicon> loadFavicons() {
        File[] icons = plugin.icons.listFiles();

        List<File> validFiles = new ArrayList<>();

        if (icons != null && icons.length != 0) {
            for (File image : icons) {
                if (FilenameUtils.getExtension(image.getPath()).equals("png")) {
                    validFiles.add(image);
                }
            }
        }
        return Arrays.asList(validFiles.stream().map(this::createFavicon).filter(Objects::nonNull).toArray(Favicon[]::new));
    }

    private Favicon createFavicon(File file) {
        try {
            return Favicon.create(ImageIO.read(file));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
