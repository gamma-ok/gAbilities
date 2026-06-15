package me.gamma.abilities.items;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.gamma.abilities.Abilities;

public class VampireItem {

	public static final String KEY = "vampire";

	@SuppressWarnings("deprecation")
	public static ItemStack create(Abilities plugin) {
		String matName = plugin.getConfig().getString("Items.Vampire.material", "REDSTONE");
		Material mat;
		try {
			mat = Material.valueOf(matName.toUpperCase());
		} catch (IllegalArgumentException e) {
			mat = Material.REDSTONE;
		}

		ItemStack item = new ItemStack(mat);
		ItemMeta meta = item.getItemMeta();

		meta.setDisplayName(plugin.color(plugin.getConfig().getString("Items.Vampire.name", "&4&lVampire")));

		List<String> lore = new ArrayList<>();
		for (String line : plugin.getConfig().getStringList("Items.Vampire.lore")) {
			lore.add(plugin.color(line));
		}
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	public static boolean matches(Abilities plugin, ItemStack item) {
		if (item == null || item.getType() == Material.AIR)
			return false;
		if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName())
			return false;
		String expected = plugin.color(plugin.getConfig().getString("Items.Vampire.name", "&4&lVampire"));
		return item.getItemMeta().getDisplayName().equals(expected);
	}
}