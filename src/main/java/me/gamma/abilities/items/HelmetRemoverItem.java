package me.gamma.abilities.items;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.gamma.abilities.Abilities;

public class HelmetRemoverItem {

	public static final String KEY = "helmet";

	public static ItemStack create(Abilities plugin) {
		ItemStack item = new ItemStack(Material.GOLD_AXE);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(
				plugin.color(plugin.getConfig().getString("Items.HelmetRemover.name", "&e&lHelmet Remover")));

		List<String> lore = new ArrayList<>();
		for (String line : plugin.getConfig().getStringList("Items.HelmetRemover.lore")) {
			lore.add(plugin.color(line));
		}
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	public static boolean matches(Abilities plugin, ItemStack item) {
		if (item == null || item.getType() != Material.GOLD_AXE)
			return false;
		if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName())
			return false;
		String expected = plugin.color(plugin.getConfig().getString("Items.HelmetRemover.name", "&e&lHelmet Remover"));
		return item.getItemMeta().getDisplayName().equals(expected);
	}
}