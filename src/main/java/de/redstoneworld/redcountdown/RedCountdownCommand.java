package de.redstoneworld.redcountdown;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

import static org.bukkit.Bukkit.getServer;

public class RedCountdownCommand implements CommandExecutor {
    private final RedCountdown plugin;

    public RedCountdownCommand(RedCountdown plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0) {
            if ("reload".equalsIgnoreCase(args[0]) && sender.hasPermission("rwm.redcountdown.reload")) {
                plugin.loadConfig();
                sender.sendMessage(ChatColor.YELLOW + "Config reloaded!");
                return true;

            } else if ("cancel".equalsIgnoreCase(args[0]) || "stop".equalsIgnoreCase(args[0]) || "stopp".equalsIgnoreCase(args[0])) {
                String starterName = args.length > 1 && sender.hasPermission("rwm.redcountdown.cancel.others") ? args[1] : sender.getName();
                if (!plugin.cancelCountdown(sender, starterName)) {
                    sender.sendMessage(plugin.getLang("error.no-countdown-running"));
                }
                return true;

            } else {
                if (plugin.hasCountdownRunning(sender.getName())) {
                    sender.sendMessage(plugin.getLang("error.countdown-already-running"));
                    return true;
                }

                final int length;
                try {
                    length = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.getLang("error.not-a-number", "input", args[0]));
                    return false;
                }

                if (length > plugin.getMaxLength()) {
                    sender.sendMessage(plugin.getLang("error.countdown-too-long",
                            "input", String.valueOf(length),
                            "min-length", String.valueOf(plugin.getMinLength()),
                            "max-length", String.valueOf(plugin.getMaxLength())
                    ));
                    return true;
                } else if (length < plugin.getMinLength()) {
                    sender.sendMessage(plugin.getLang("error.countdown-too-short",
                            "input", String.valueOf(length),
                            "min-length", String.valueOf(plugin.getMinLength()),
                            "max-length", String.valueOf(plugin.getMaxLength())
                    ));
                    return true;
                }

                Location senderLocation;

                if (sender instanceof Entity) {
                    senderLocation = ((Entity) sender).getLocation();
                } else if (sender instanceof BlockCommandSender) {
                    senderLocation = ((BlockCommandSender) sender).getBlock().getLocation();
                } else {
                    sender.sendMessage(plugin.getLang("error.unsupported-sender", "type", sender.getClass().getSimpleName()));
                    return true;
                }

                final List<Player> players = getServer().getOnlinePlayers().stream().filter(
                        player -> player.getWorld() == senderLocation.getWorld()
                                && player.getLocation().distanceSquared(senderLocation) <= plugin.getRadius() * plugin.getRadius()
                ).collect(Collectors.toList());

                plugin.startCountdown(sender, players, length);
                return true;
            }
        }
        return false;
    }
}
