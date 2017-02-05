package de.redstoneworld.redcountdown;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class RedCountdown extends JavaPlugin {

    private int radius;
    private int minLength;
    private int maxLength;

    private List<RedCountdownTitle> titles;

    private Map<String, CountdownRunnable> countdownTasks = new HashMap<>();

    @Override
    public void onEnable() {
        getCommand("redcountdown").setExecutor(new RedCountdownCommand(this));
        loadConfig();
    }

    public void loadConfig() {
        saveDefaultConfig();
        reloadConfig();

        PluginCommand command = getCommand("redcountdown");
        command.setPermissionMessage(getLang("error.no-permission", "permission", command.getPermission()));
        command.setUsage(translate(getLang("error.syntax")));

        radius = getConfig().getInt("radius");
        minLength = getConfig().getInt("min-length");
        maxLength = getConfig().getInt("max-length");

        titles = new ArrayList<>();
        for (Map<?, ?> titleConfig : getConfig().getMapList("titles")) {
            try {
                RedCountdownTitle title = new RedCountdownTitle(titleConfig);
                titles.add(title);
                getLogger().log(Level.INFO, "Loaded " + title.toString());
            } catch (IllegalArgumentException e) {
                getLogger().log(Level.WARNING, "Error while loading a title from the config: " + e.getMessage());
            }
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    String getLang(String key, String... repl) {
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

    public void startCountdown(CommandSender sender, List<Player> players, int length) {
        String msgStarted = getLang("started",
                "time", String.valueOf(length),
                "starter", sender.getName()
        );
        players.stream().filter(Player::isOnline).forEach(player -> {
            player.sendMessage(msgStarted);
        });
        if (sender instanceof Player && !players.contains(sender)) {
            sender.sendMessage(msgStarted);
        }

        CountdownRunnable countdownRunnable = new CountdownRunnable(sender, players, length) {

            @Override
            public void run() {
                if (!hasCountdownRunning(starter.getName().toLowerCase())) {
                    cancel();
                    return;
                }

                RedCountdownTitle title = titles.get(titlesIndex);

                String titleText = translate(title.getTitle(), "number", String.valueOf(step));
                String subTitleText = translate(title.getSubTitle(), "number", String.valueOf(step));
                String soundId = (title.getSound() != null) ? translate(title.getSound(), "number", String.valueOf(step)) : null;

                players.stream().filter(Player::isOnline).forEach(player -> {
                    player.sendTitle(titleText, subTitleText);
                    player.playSound(player.getLocation(), soundId, title.getSoundVolume(), title.getSoundPitch());
                });

                if (title.getLowest() == step) {
                    titlesIndex++;
                }

                if (step == 0) {
                    cancel();
                    countdownTasks.remove(starter.getName().toLowerCase());
                    String msgFinished = getLang("finished",
                            "time", String.valueOf(length),
                            "starter", sender.getName()
                    );
                    players.stream().filter(Player::isOnline).forEach(player -> {
                        sender.sendMessage(msgFinished);
                    });
                    if (!(starter instanceof Player) || !players.contains(starter)) {
                        starter.sendMessage(msgFinished);
                    }
                }
                step--;
            }

        };
        countdownRunnable.runTaskTimer(this, 0, 20);
        countdownTasks.put(sender.getName().toLowerCase(), countdownRunnable);
    }

    public boolean cancelCountdown(CommandSender sender, String starter) {
        if (hasCountdownRunning(starter)) {
            CountdownRunnable countdownTask = getCountdownTask(starter);
            countdownTask.cancel(sender);
            countdownTasks.remove(starter.toLowerCase());
            return true;
        }
        return false;
    }

    private CountdownRunnable getCountdownTask(String starter) {
        return countdownTasks.get(starter.toLowerCase());
    }

    public boolean hasCountdownRunning(String starter) {
        return countdownTasks.containsKey(starter.toLowerCase());
    }

    public int getMinLength() {
        return minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public int getRadius() {
        return radius;
    }

    private abstract class CountdownRunnable extends BukkitRunnable {
        private final int length;
        final List<Player> players;
        CommandSender starter;
        int step;
        int titlesIndex = 0;

        CountdownRunnable(CommandSender starter, List<Player> players, int length) {
            this.starter = starter;
            this.players = players;
            this.length = length;
            step = length;
        }

        void cancel(CommandSender sender) {
            cancel();
            String msgCancelled = getLang("cancelled",
                    "time", String.valueOf(length),
                    "starter", starter.getName()
            );
            players.stream().filter(Player::isOnline).forEach(player -> {
                player.sendMessage(msgCancelled);
            });
            if (sender != null) {
                if (!(starter instanceof Player) || !players.contains(starter)) {
                    starter.sendMessage(getLang("cancelled",
                            "time", String.valueOf(length),
                            "starter", starter.getName()
                    ));
                }
                if (sender != starter && (!(sender instanceof Player) || !players.contains(sender))) {
                    sender.sendMessage(getLang("cancelled",
                            "time", String.valueOf(length),
                            "starter", starter.getName()
                    ));
                }
            }
        }
    }
}
