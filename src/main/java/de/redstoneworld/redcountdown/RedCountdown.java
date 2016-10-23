package de.redstoneworld.redcountdown;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class RedCountdown extends JavaPlugin {

    private int radius;
    private int maxLength;

    private List<Title> titles;

    private BukkitTask countdownTask = null;

    @Override
    public void onEnable() {
        getCommand("redcountdown").setExecutor(this);
        loadConfig();
    }

    public void loadConfig() {
        saveDefaultConfig();
        reloadConfig();

        PluginCommand command = getCommand("redcountdown");
        command.setPermissionMessage(getLang("error.no-permission", "permission", command.getPermission()));
        command.setUsage(translate(getLang("error.syntax")));

        radius = getConfig().getInt("radius");
        maxLength = getConfig().getInt("max-length");

        titles = new ArrayList<>();
        for (Map<?, ?> title : getConfig().getMapList("titles")) {
            titles.add(new Title(title));
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0) {
            if ("reload".equalsIgnoreCase(args[0]) && sender.hasPermission("rwm.redcountdown.reload")) {
                loadConfig();
                sender.sendMessage(ChatColor.YELLOW + "Config reloaded!");
                return true;

            } else if ("cancel".equalsIgnoreCase(args[0])) {
                if (countdownTask != null) {
                    countdownTask.cancel();
                    countdownTask = null;
                    sender.sendMessage(getLang("cancelled"));
                } else {
                    sender.sendMessage(getLang("error.no-countdown-running"));
                }
                return true;

            } else {
                if (countdownTask != null) {
                    sender.sendMessage(getLang("error.countdown-already-running"));
                    return true;
                }

                final int length;
                try {
                    length = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(getLang("error.not-a-number", "input", args[0]));
                    return false;
                }

                if (length > maxLength) {
                    sender.sendMessage(getLang("error.countdown-too-long",
                            "input", String.valueOf(length),
                            "max-length", String.valueOf(maxLength)
                    ));
                    return true;
                }

                Location senderLocation;

                if (sender instanceof Entity) {
                    senderLocation = ((Entity) sender).getLocation();
                } else if (sender instanceof BlockCommandSender) {
                    senderLocation = ((BlockCommandSender) sender).getBlock().getLocation();
                } else {
                    sender.sendMessage(getLang("error.unsupported-sender", "type", sender.getClass().getSimpleName()));
                    return true;
                }

                final List<Player> players = getServer().getOnlinePlayers().stream().filter(
                        player -> player.getLocation().distanceSquared(senderLocation) <= radius * radius
                ).collect(Collectors.toList());

                countdownTask = new BukkitRunnable() {
                    int step = length;
                    int titlesIndex = 0;
                    CommandSender starter = sender;

                    @Override
                    public void run() {
                        if (countdownTask == null) {
                            return;
                        }

                        Title title = titles.get(titlesIndex);

                        String titleText = translate(title.getTitle(), "number", String.valueOf(step));
                        String subTitleText = translate(title.getSubTitle(), "number", String.valueOf(step));

                        players.stream().filter(Player::isOnline).forEach(player -> {
                            player.sendTitle(titleText, subTitleText);
                        });

                        if (title.getLowest() == step) {
                            titlesIndex++;
                        }

                        if (step == 0) {
                            cancel();
                            countdownTask = null;
                            starter.sendMessage(getLang("finished", "time", String.valueOf(length)));
                        }
                        step--;
                    }
                }.runTaskTimer(this, 0, 20);

                sender.sendMessage(getLang("started", "time", String.valueOf(length)));
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private String getLang(String key, String... repl) {
        String msg = getConfig().getString("msg." + key, null);
        if (msg == null) {
            msg = ChatColor.RED + "Unknown language key " + ChatColor.YELLOW + key;
        }
        return translate(msg, repl);
    }

    private String translate(String msg, String... repl) {
        for (int i = 0; i + 1 < repl.length; i += 2) {
            msg = msg.replace("%" + repl[i] + "%", repl[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private class Title {
        private final int lowest;
        private final String title;
        private final String subTitle;

        public Title(int lowest, String title, String subTitle) {
            this.lowest = lowest;
            this.title = title;
            this.subTitle = subTitle;
        }

        public Title(Map<?, ?> title) throws IllegalArgumentException {
            if (!(title.containsKey("lowest") && title.get("lowest") instanceof Integer)) {
                throw new IllegalArgumentException("lowest is not an Integer?");
            }
            this.lowest = (Integer) title.get("lowest");
            if (title.containsKey("title")) {
                this.title = (String) title.get("title");
            } else {
                this.title = "";
            }
            if (title.containsKey("subtitle")) {
                this.subTitle = (String) title.get("subtitle");
            } else {
                this.subTitle = "";
            }
        }

        public int getLowest() {
            return lowest;
        }

        public String getTitle() {
            return title;
        }

        public String getSubTitle() {
            return subTitle;
        }
    }
}
