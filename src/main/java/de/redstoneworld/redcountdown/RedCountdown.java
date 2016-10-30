package de.redstoneworld.redcountdown;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class RedCountdown extends JavaPlugin {

    private int radius;
    private int maxLength;

    private List<RedCountdownTitle> titles;

    private BukkitTask countdownTask = null;
    private CommandSender countdownStarter = null;

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
        countdownStarter = sender;
        countdownTask = new BukkitRunnable() {
            int step = length;
            int titlesIndex = 0;

            @Override
            public void run() {
                if (!isCountdownRunning()) {
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
                    countdownTask = null;
                    countdownStarter.sendMessage(getLang("finished", "time", String.valueOf(length)));
                }
                step--;
            }
        }.runTaskTimer(this, 0, 20);
    }

    public boolean cancelCountdown() {
        if (isCountdownRunning()) {
            countdownTask.cancel();
            countdownTask = null;
            return true;
        }
        return false;
    }

    public boolean isCountdownRunning() {
        return countdownTask != null;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public int getRadius() {
        return radius;
    }

    public CommandSender getCountdownStarter() {
        return countdownStarter;
    }
}
