package me.gamma.abilities.items;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.gamma.abilities.Abilities;

public class RocketItem {

	public static final String KEY = "rocket";

	@SuppressWarnings("deprecation")
	public static ItemStack create(Abilities plugin) {
		// ID 401 = Fireworks Rocket en 1.8
		ItemStack item = new ItemStack(401);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(plugin.color(plugin.getConfig().getString("Items.Rocket.name", "&c&lRocket")));

		List<String> lore = new ArrayList<>();
		for (String line : plugin.getConfig().getStringList("Items.Rocket.lore")) {
			lore.add(plugin.color(line));
		}
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	@SuppressWarnings("deprecation")
	public static boolean matches(Abilities plugin, ItemStack item) {
		if (item == null || item.getTypeId() != 401)
			return false;
		if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName())
			return false;
		String expected = plugin.color(plugin.getConfig().getString("Items.Rocket.name", "&c&lRocket"));
		return item.getItemMeta().getDisplayName().equals(expected);
	}
}