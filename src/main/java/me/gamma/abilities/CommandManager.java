package me.gamma.abilities;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.gamma.abilities.items.*;

public class CommandManager implements CommandExecutor {

	private final Abilities plugin;

	public CommandManager(Abilities plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!cmd.getName().equalsIgnoreCase("abilities"))
			return true;

		if (!sender.hasPermission("abilities.admin")) {
			sendConfigMessage(sender, "Messages.NoPermission");
			return true;
		}

		if (args.length == 0) {
			sendConfigMessage(sender, "Messages.Help");
			return true;
		}

		switch (args[0].toLowerCase()) {

		case "reload":
			if (!checkPermission(sender, "abilities.admin"))
				return true;
			plugin.reloadConfig();
			sendConfigMessage(sender, "Messages.ReloadSuccess");
			break;

		case "give":
			if (!checkPermission(sender, "abilities.admin"))
				return true;
			if (args.length < 3) {
				sendConfigMessage(sender, "Messages.InvalidSyntax");
				return true;
			}

			Player target = Bukkit.getPlayer(args[1]);
			if (target == null) {
				sendConfigMessage(sender, "Messages.PlayerNotFound");
				return true;
			}

			int amount = 1;
			if (args.length >= 4) {
				try {
					amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
				} catch (NumberFormatException e) {
					sendConfigMessage(sender, "Messages.InvalidAmount");
					return true;
				}
			}

			ItemStack item = getItemByName(args[2]);
			if (item == null) {
				sendConfigMessage(sender, "Messages.ItemUnknown");
				return true;
			}

			item.setAmount(amount);
			target.getInventory().addItem(item);
			target.updateInventory();

			sendFormattedMessage(sender, "Messages.GiveSuccess", args[2], amount, target.getName());
			sendFormattedMessage(target, "Messages.ReceiveSuccess", args[2], amount, null);
			break;

		case "list":
			sendConfigMessage(sender, "Messages.ItemList");
			break;

		case "resetcd":
			if (!checkPermission(sender, "abilities.admin"))
				return true;
			if (args.length < 2) {
				sendConfigMessage(sender, "Messages.InvalidSyntax");
				return true;
			}

			Player resetTarget = Bukkit.getPlayer(args[1]);
			if (resetTarget == null) {
				sendConfigMessage(sender, "Messages.PlayerNotFound");
				return true;
			}

			if (args.length >= 3) {
				String itemKey = args[2].toLowerCase();
				if (getItemByName(itemKey) == null) {
					sendConfigMessage(sender, "Messages.ItemUnknown");
					return true;
				}
				plugin.removeCooldown(resetTarget, itemKey);
				sendFormattedMessage(sender, "Messages.ResetCooldown", itemKey, 0, resetTarget.getName());
				sendFormattedMessage(resetTarget, "Messages.ResetCooldownSelf", itemKey, 0, null);
			} else {
				plugin.removeAllCooldowns(resetTarget);
				sendFormattedMessage(sender, "Messages.ResetAllCooldowns", null, 0, resetTarget.getName());
				sendConfigMessage(resetTarget, "Messages.ResetAllCooldownsSelf");
			}
			break;

		default:
			sendConfigMessage(sender, "Messages.Help");
			break;
		}
		return true;
	}

	private void sendConfigMessage(CommandSender sender, String path) {
		if (plugin.getConfig().isList(path)) {
			for (String line : plugin.getConfig().getStringList(path)) {
				sender.sendMessage(plugin.color(line));
			}
		} else {
			sender.sendMessage(plugin
					.color(plugin.getConfig().getString(path, "&cError: Mensaje no encontrado en config: " + path)));
		}
	}

	private void sendFormattedMessage(CommandSender sender, String path, String item, int amount, String targetName) {
		String msg = plugin.getConfig().getString(path, "");
		if (item != null)
			msg = msg.replace("<item>", item);
		if (amount > 0)
			msg = msg.replace("<amount>", String.valueOf(amount));
		if (targetName != null)
			msg = msg.replace("<target>", targetName).replace("<player>", targetName);
		sender.sendMessage(plugin.color(msg));
	}

	private boolean checkPermission(CommandSender sender, String perm) {
		if (!sender.hasPermission(perm)) {
			sendConfigMessage(sender, "Messages.NoPermission");
			return false;
		}
		return true;
	}

	private ItemStack getItemByName(String name) {
		switch (name.toLowerCase()) {
		case "vampire":
			return VampireItem.create(plugin);
		case "mixer":
			return MixerItem.create(plugin);
		case "antifall":
			return AntiFallItem.create(plugin);
		case "helmet":
			return HelmetRemoverItem.create(plugin);
		case "rocket":
			return RocketItem.create(plugin);
		case "snowball":
			return SnowballItem.create(plugin);
		case "switcher":
			return SwitcherItem.create(plugin);
		default:
			return null;
		}
	}
}