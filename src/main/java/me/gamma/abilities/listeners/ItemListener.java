package me.gamma.abilities.listeners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import me.gamma.abilities.Abilities;
import me.gamma.abilities.items.AntiFallItem;
import me.gamma.abilities.items.SwitcherItem;
import me.gamma.abilities.items.HelmetRemoverItem;
import me.gamma.abilities.items.MixerItem;
import me.gamma.abilities.items.RocketItem;
import me.gamma.abilities.items.SnowballItem;
import me.gamma.abilities.items.VampireItem;

public class ItemListener implements Listener {

	private final Abilities plugin;

	private final Map<Integer, UUID> snowballThrowers = new HashMap<>();
	private final Map<Integer, UUID> switcherThrowers = new HashMap<>();

	private final java.util.Set<UUID> rocketUsers = new java.util.HashSet<>();

	private final java.util.Set<UUID> snowballCooldownFlag = new java.util.HashSet<>();
	private final java.util.Set<UUID> switcherCooldownFlag = new java.util.HashSet<>();

	public ItemListener(Abilities plugin) {
		this.plugin = plugin;
	}

	// Utilidades Generales

	private boolean checkAndApplyCooldowns(Player player, String itemKey, int itemCooldownSeconds) {
		if (plugin.isOnGlobalCooldown(player)) {
			long remaining = plugin.getRemainingGlobalCooldown(player);
			String msg = plugin.getConfig()
					.getString("Messages.GlobalCooldown",
							"&c[PartnerItems] Debes esperar &e<time>s &cpara usar otro partner item.")
					.replace("<time>", String.valueOf(remaining));
			player.sendMessage(plugin.color(msg));
			return false;
		}

		if (plugin.isOnCooldown(player, itemKey)) {
			showCooldown(player, player.getItemInHand());
			return false;
		}

		plugin.setCooldown(player, itemKey, itemCooldownSeconds);
		plugin.setGlobalCooldown(player);
		return true;
	}

	private void consumeItem(Player player) {
		ItemStack hand = player.getItemInHand();
		if (hand.getAmount() > 1) {
			hand.setAmount(hand.getAmount() - 1);
		} else {
			player.setItemInHand(null);
		}
		player.updateInventory();
	}

	private void sendMessage(Player player, String configPath, String... replacements) {
		if (plugin.getConfig().isList(configPath)) {
			for (String line : plugin.getConfig().getStringList(configPath)) {
				String msg = line;
				for (int i = 0; i + 1 < replacements.length; i += 2) {
					msg = msg.replace(replacements[i], replacements[i + 1]);
				}
				player.sendMessage(plugin.color(msg));
			}
		} else {
			String msg = plugin.getConfig().getString(configPath, "");
			for (int i = 0; i + 1 < replacements.length; i += 2) {
				msg = msg.replace(replacements[i], replacements[i + 1]);
			}
			player.sendMessage(plugin.color(msg));
		}
	}

	private boolean isAnyPartnerItem(Abilities plugin, ItemStack item) {
		return VampireItem.matches(plugin, item) || MixerItem.matches(plugin, item)
				|| AntiFallItem.matches(plugin, item) || HelmetRemoverItem.matches(plugin, item)
				|| RocketItem.matches(plugin, item) || SnowballItem.matches(plugin, item)
				|| SwitcherItem.matches(plugin, item);
	}

	private int registerHitWithExpireCheck(Player attacker, Player victim, String itemKey, String coloredItemName) {
		return plugin.registerHit(attacker, victim, itemKey);
	}

	private String getColoredItemName(String configKey, String defaultName) {
		String itemDisplayName = plugin.getConfig().getString("Items." + configKey + ".name", defaultName);
		return plugin.color(itemDisplayName);
	}

	// Eventos de Habilidades (Vampire, Mixer, AntiFall, Helmet, Rocket)

	@EventHandler
	public void onVampireHit(EntityDamageByEntityEvent e) {
		if (!(e.getDamager() instanceof Player) || !(e.getEntity() instanceof Player))
			return;

		Player attacker = (Player) e.getDamager();
		Player victim = (Player) e.getEntity();

		if (!VampireItem.matches(plugin, attacker.getItemInHand()))
			return;

		if (plugin.isOnGlobalCooldown(attacker)) {
			long r = plugin.getRemainingGlobalCooldown(attacker);
			attacker.sendMessage(plugin.color(
					plugin.getConfig().getString("Messages.GlobalCooldown", "&c[PartnerItems] Debes esperar &e<time>s.")
							.replace("<time>", String.valueOf(r))));
			return;
		}

		if (plugin.isOnCooldown(attacker, VampireItem.KEY)) {
			showCooldown(attacker, attacker.getItemInHand());
			return;
		}

		int hitsRequired = plugin.getConfig().getInt("Items.Vampire.hits-required", 3);
		String coloredItemName = getColoredItemName("Vampire", "Vampire");
		int currentHits = registerHitWithExpireCheck(attacker, victim, VampireItem.KEY, coloredItemName);

		if (currentHits < hitsRequired) {
			String msg = plugin.getConfig()
					.getString("Messages.HitsRemaining", "&7[<item>] &e<current>&7/&e<total> &7hits en &e<target>&7.")
					.replace("<item>", coloredItemName).replace("<current>", String.valueOf(currentHits))
					.replace("<total>", String.valueOf(hitsRequired)).replace("<target>", victim.getName());
			attacker.sendMessage(plugin.color(msg));
			return;
		}

		plugin.resetHits(attacker, victim, VampireItem.KEY);

		java.util.Collection<PotionEffect> stolenEffects = new java.util.ArrayList<>(victim.getActivePotionEffects());
		for (PotionEffect effect : stolenEffects)
			victim.removePotionEffect(effect.getType());
		for (PotionEffect effect : new java.util.ArrayList<>(attacker.getActivePotionEffects()))
			attacker.removePotionEffect(effect.getType());
		for (PotionEffect effect : stolenEffects)
			attacker.addPotionEffect(effect, true);

		consumeItem(attacker);
		plugin.setCooldown(attacker, VampireItem.KEY, plugin.getConfig().getInt("Items.Vampire.cooldown", 15));
		plugin.setGlobalCooldown(attacker);

		sendMessage(attacker, "Messages.Vampire.AttackerMessage", "<n>", victim.getName(), "<player>",
				attacker.getName());
		sendMessage(victim, "Messages.Vampire.VictimMessage", "<n>", attacker.getName(), "<player>",
				attacker.getName());
	}

	@EventHandler
	public void onMixerHit(EntityDamageByEntityEvent e) {
		if (!(e.getDamager() instanceof Player) || !(e.getEntity() instanceof Player))
			return;

		Player attacker = (Player) e.getDamager();
		Player victim = (Player) e.getEntity();

		if (!MixerItem.matches(plugin, attacker.getItemInHand()))
			return;

		if (plugin.isOnGlobalCooldown(attacker)) {
			long r = plugin.getRemainingGlobalCooldown(attacker);
			attacker.sendMessage(plugin.color(
					plugin.getConfig().getString("Messages.GlobalCooldown", "&c[PartnerItems] Debes esperar &e<time>s.")
							.replace("<time>", String.valueOf(r))));
			return;
		}

		if (plugin.isOnCooldown(attacker, MixerItem.KEY)) {
			showCooldown(attacker, attacker.getItemInHand());
			return;
		}

		int hitsRequired = plugin.getConfig().getInt("Items.Mixer.hits-required", 3);
		String coloredItemName = getColoredItemName("Mixer", "Mixer");
		int currentHits = registerHitWithExpireCheck(attacker, victim, MixerItem.KEY, coloredItemName);

		if (currentHits < hitsRequired) {
			String msg = plugin.getConfig()
					.getString("Messages.HitsRemaining", "&7[<item>] &e<current>&7/&e<total> &7hits en &e<target>&7.")
					.replace("<item>", coloredItemName).replace("<current>", String.valueOf(currentHits))
					.replace("<total>", String.valueOf(hitsRequired)).replace("<target>", victim.getName());
			attacker.sendMessage(plugin.color(msg));
			return;
		}

		plugin.resetHits(attacker, victim, MixerItem.KEY);

		ItemStack[] hotbar = new ItemStack[9];
		for (int i = 0; i < 9; i++)
			hotbar[i] = victim.getInventory().getItem(i);
		List<ItemStack> list = new ArrayList<>();
		for (ItemStack is : hotbar)
			list.add(is);
		Collections.shuffle(list);
		for (int i = 0; i < 9; i++)
			victim.getInventory().setItem(i, list.get(i));
		victim.updateInventory();

		consumeItem(attacker);
		plugin.setCooldown(attacker, MixerItem.KEY, plugin.getConfig().getInt("Items.Mixer.cooldown", 20));
		plugin.setGlobalCooldown(attacker);

		sendMessage(attacker, "Messages.Mixer.AttackerMessage", "<n>", victim.getName(), "<player>",
				attacker.getName());
		sendMessage(victim, "Messages.Mixer.VictimMessage", "<n>", attacker.getName(), "<player>", attacker.getName());
	}

	@EventHandler
	public void onAntiFallUse(PlayerInteractEvent e) {
		Player player = e.getPlayer();
		ItemStack hand = player.getItemInHand();
		if (!AntiFallItem.matches(plugin, hand))
			return;

		org.bukkit.event.block.Action action = e.getAction();
		if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
				&& action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
			return;

		if (plugin.hasAntiFall(player)) {
			player.sendMessage(plugin.color(plugin.getConfig().getString("Messages.AntiFall.AlreadyActive",
					"&f[Anti Fall] &7Ya tienes el efecto activo.")));
			return;
		}

		if (!checkAndApplyCooldowns(player, AntiFallItem.KEY, plugin.getConfig().getInt("Items.AntiFall.cooldown", 60)))
			return;

		int duration = plugin.getConfig().getInt("Items.AntiFall.duration", 10);
		plugin.setAntiFall(player, duration);
		consumeItem(player);

		sendMessage(player, "Messages.AntiFall.Activated", "<time>", String.valueOf(duration), "<player>",
				player.getName());

		plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			if (plugin.hasAntiFall(player)) {
				plugin.removeAntiFall(player);
				if (player.isOnline())
					sendMessage(player, "Messages.AntiFall.Expired", "<player>", player.getName());
			}
		}, duration * 20L);
	}

	@EventHandler
	public void onFallDamage(EntityDamageEvent e) {
		if (!(e.getEntity() instanceof Player))
			return;
		if (e.getCause() != EntityDamageEvent.DamageCause.FALL)
			return;
		Player player = (Player) e.getEntity();
		if (plugin.hasAntiFall(player)) {
			e.setCancelled(true);
			return;
		}
		if (rocketUsers.contains(player.getUniqueId())) {
			rocketUsers.remove(player.getUniqueId());
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onHelmetRemoverHit(EntityDamageByEntityEvent e) {
		if (!(e.getDamager() instanceof Player) || !(e.getEntity() instanceof Player))
			return;
		Player attacker = (Player) e.getDamager();
		Player victim = (Player) e.getEntity();

		if (!HelmetRemoverItem.matches(plugin, attacker.getItemInHand()))
			return;
		if (plugin.hasStoredHelmet(victim))
			return;

		if (plugin.isOnGlobalCooldown(attacker)) {
			long r = plugin.getRemainingGlobalCooldown(attacker);
			attacker.sendMessage(plugin.color(
					plugin.getConfig().getString("Messages.GlobalCooldown", "&c[PartnerItems] Debes esperar &e<time>s.")
							.replace("<time>", String.valueOf(r))));
			return;
		}

		if (plugin.isOnCooldown(attacker, HelmetRemoverItem.KEY)) {
			showCooldown(attacker, attacker.getItemInHand());
			return;
		}

		int hitsRequired = plugin.getConfig().getInt("Items.HelmetRemover.hits-required", 3);
		String coloredItemName = getColoredItemName("HelmetRemover", "Helmet Remover");
		int currentHits = registerHitWithExpireCheck(attacker, victim, HelmetRemoverItem.KEY, coloredItemName);

		if (currentHits < hitsRequired) {
			String msg = plugin.getConfig()
					.getString("Messages.HitsRemaining", "&7[<item>] &e<current>&7/&e<total> &7hits en &e<target>&7.")
					.replace("<item>", coloredItemName).replace("<current>", String.valueOf(currentHits))
					.replace("<total>", String.valueOf(hitsRequired)).replace("<target>", victim.getName());
			attacker.sendMessage(plugin.color(msg));
			return;
		}

		plugin.resetHits(attacker, victim, HelmetRemoverItem.KEY);
		ItemStack helmet = victim.getInventory().getHelmet();
		if (helmet == null) {
			attacker.sendMessage(plugin.color(plugin.getConfig().getString("Messages.HelmetRemover.NoHelmet",
					"&e[Helmet Remover] &7El enemigo no tiene casco.")));
			return;
		}

		plugin.getStoredHelmets().put(victim.getUniqueId(), helmet.clone());
		victim.getInventory().setHelmet(null);
		victim.updateInventory();
		consumeItem(attacker);

		int duration = plugin.getConfig().getInt("Items.HelmetRemover.duration", 8);
		plugin.setCooldown(attacker, HelmetRemoverItem.KEY,
				plugin.getConfig().getInt("Items.HelmetRemover.cooldown", 30));
		plugin.setGlobalCooldown(attacker);

		sendMessage(attacker, "Messages.HelmetRemover.AttackerMessage", "<n>", victim.getName(), "<time>",
				String.valueOf(duration), "<player>", attacker.getName());
		sendMessage(victim, "Messages.HelmetRemover.VictimMessage", "<n>", attacker.getName(), "<time>",
				String.valueOf(duration), "<player>", attacker.getName());

		plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			if (!victim.isOnline())
				return;
			ItemStack stored = plugin.getStoredHelmets().remove(victim.getUniqueId());
			if (stored == null)
				return;
			if (victim.getInventory().getHelmet() == null)
				victim.getInventory().setHelmet(stored);
			else
				victim.getWorld().dropItemNaturally(victim.getLocation(), stored);
			victim.updateInventory();
		}, duration * 20L);
	}

	@EventHandler
	public void onRocketUse(PlayerInteractEvent e) {
		Player player = e.getPlayer();
		ItemStack hand = player.getItemInHand();
		if (!RocketItem.matches(plugin, hand))
			return;
		e.setCancelled(true);
		org.bukkit.event.block.Action action = e.getAction();
		if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
				&& action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
			return;

		if (!checkAndApplyCooldowns(player, RocketItem.KEY, plugin.getConfig().getInt("Items.Rocket.cooldown", 25)))
			return;

		int blocks = plugin.getConfig().getInt("Items.Rocket.blocks", 20);
		player.setVelocity(new Vector(0, Math.min(Math.sqrt(2 * 0.08 * blocks), 4.0), 0));
		rocketUsers.add(player.getUniqueId());
		consumeItem(player);
		sendMessage(player, "Messages.Rocket.Launched", "<player>", player.getName());
	}

	// ─── Snowball y Switcher - Lógica de Impacto ───

	@EventHandler
	public void onSnowballThrow(PlayerInteractEvent e) {
		Player player = e.getPlayer();
		ItemStack hand = player.getItemInHand();
		if (!SnowballItem.matches(plugin, hand))
			return;
		org.bukkit.event.block.Action action = e.getAction();
		if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
				&& action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
			return;
		e.setCancelled(true);

		if (snowballCooldownFlag.contains(player.getUniqueId()))
			return;
		snowballCooldownFlag.add(player.getUniqueId());
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> snowballCooldownFlag.remove(player.getUniqueId()),
				2L);

		if (!checkAndApplyCooldowns(player, SnowballItem.KEY, plugin.getConfig().getInt("Items.Snowball.cooldown", 20)))
			return;

		Snowball snowball = player.launchProjectile(Snowball.class);
		snowballThrowers.put(snowball.getEntityId(), player.getUniqueId());
		consumeItem(player);
	}

	@EventHandler
	public void onSnowballHit(ProjectileHitEvent e) {
		if (!(e.getEntity() instanceof Snowball))
			return;
		final int id = e.getEntity().getEntityId();
		// Cleanup por si falla el tiro
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> snowballThrowers.remove(id), 5L);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onSnowballDamage(EntityDamageByEntityEvent e) {
		if (!(e.getDamager() instanceof Snowball) || !(e.getEntity() instanceof Player))
			return;
		Snowball snowball = (Snowball) e.getDamager();
		Player victim = (Player) e.getEntity();

		UUID throwerUUID = snowballThrowers.remove(snowball.getEntityId());
		if (throwerUUID == null)
			return;

		Player thrower = plugin.getServer().getPlayer(throwerUUID);
		if (thrower != null && victim.equals(thrower))
			return;

		int maxDistance = plugin.getConfig().getInt("Items.Snowball.max-distance", 8);
		if (thrower != null && thrower.getLocation().distance(victim.getLocation()) > maxDistance) {
			thrower.sendMessage(plugin.color(plugin.getConfig()
					.getString("Messages.Snowball.TooFar", "&c[Snowball] &7Demasiado lejos.").replace("<distance>",
							String.format("%.1f", thrower.getLocation().distance(victim.getLocation())))));
			return;
		}

		try {
			int durationTicks = plugin.getConfig().getInt("Items.Snowball.effect-duration", 5) * 20;
			for (String effectEntry : plugin.getConfig().getStringList("Items.Snowball.effects")) {
				String[] parts = effectEntry.split(":");
				if (parts.length < 2)
					continue;
				PotionEffectType type = PotionEffectType.getByName(parts[0]);
				if (type != null)
					victim.addPotionEffect(new PotionEffect(type, durationTicks, Integer.parseInt(parts[1])), true);
			}
			if (thrower != null)
				sendMessage(thrower, "Messages.Snowball.AttackerMessage", "<n>", victim.getName(), "<player>",
						thrower.getName());
			sendMessage(victim, "Messages.Snowball.VictimMessage", "<n>",
					thrower != null ? thrower.getName() : "Alguien", "<player>",
					thrower != null ? thrower.getName() : "Alguien");
		} catch (Exception ex) {
			plugin.getLogger().warning("[Snowball] Error: " + ex.getMessage());
		}
	}

	// SWITCHER — intercambio de posición
	@EventHandler
	public void onSwitcherThrow(PlayerInteractEvent e) {
		Player player = e.getPlayer();
		ItemStack hand = player.getItemInHand();
		if (!SwitcherItem.matches(plugin, hand))
			return;

		org.bukkit.event.block.Action action = e.getAction();
		if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
				&& action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
			return;
		e.setCancelled(true);

		if (switcherCooldownFlag.contains(player.getUniqueId()))
			return;
		switcherCooldownFlag.add(player.getUniqueId());
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> switcherCooldownFlag.remove(player.getUniqueId()),
				2L);

		if (!checkAndApplyCooldowns(player, SwitcherItem.KEY, plugin.getConfig().getInt("Items.Switcher.cooldown", 25)))
			return;

		Egg egg = player.launchProjectile(Egg.class);
		switcherThrowers.put(egg.getEntityId(), player.getUniqueId());
		consumeItem(player);
	}

	@EventHandler
	public void onSwitcherHit(ProjectileHitEvent e) {
		if (!(e.getEntity() instanceof Egg))
			return;
		final int id = e.getEntity().getEntityId();
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> switcherThrowers.remove(id), 5L);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onSwitcherDamage(EntityDamageByEntityEvent e) {
		if (!(e.getDamager() instanceof Egg) || !(e.getEntity() instanceof Player))
			return;
		Egg egg = (Egg) e.getDamager();
		Player victim = (Player) e.getEntity();

		UUID throwerUUID = switcherThrowers.remove(egg.getEntityId());
		if (throwerUUID == null)
			return;

		Player thrower = plugin.getServer().getPlayer(throwerUUID);
		if (thrower == null || victim.equals(thrower))
			return;

		double realDistance = thrower.getLocation().distance(victim.getLocation());
		int maxDistance = plugin.getConfig().getInt("Items.Switcher.max-distance", 8);

		if (realDistance > maxDistance) {
			String tooFarMsg = plugin.getConfig()
					.getString("Messages.Switcher.TooFar",
							"&c[Swap] &7El jugador está demasiado lejos &e(<distance> bloques)&7.")
					.replace("<distance>", String.format("%.1f", realDistance));
			thrower.sendMessage(plugin.color(tooFarMsg));
			return;
		}

		try {
			Location tLoc = thrower.getLocation().clone();
			Location vLoc = victim.getLocation().clone();

			thrower.teleport(vLoc);
			victim.teleport(tLoc);

			sendMessage(thrower, "Messages.Switcher.AttackerMessage", "<n>", victim.getName(), "<player>",
					thrower.getName());
			sendMessage(victim, "Messages.Switcher.VictimMessage", "<n>", thrower.getName(), "<player>",
					thrower.getName());
		} catch (Exception ex) {
			plugin.getLogger().warning("[Switcher] Error al teletransportar: " + ex.getMessage());
		}
	}

	// Otros Eventos y Limpieza

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		Player player = e.getPlayer();
		plugin.removeAntiFall(player);
		ItemStack stored = plugin.getStoredHelmets().remove(player.getUniqueId());
		if (stored != null)
			player.getWorld().dropItemNaturally(player.getLocation(), stored);
	}

	@EventHandler
	public void onItemPlace(PlayerInteractEvent e) {
		ItemStack hand = e.getPlayer().getItemInHand();
		if (hand != null && hand.getType() != org.bukkit.Material.AIR && isAnyPartnerItem(plugin, hand))
			e.setCancelled(true);
	}

	@EventHandler
	public void onSwitcherSpawnChicken(org.bukkit.event.entity.CreatureSpawnEvent e) {
		if (e.getSpawnReason() == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.EGG)
			e.setCancelled(true);
	}

	@EventHandler
	public void onCheckCooldown(PlayerInteractEvent e) {
		Player player = e.getPlayer();
		ItemStack hand = player.getItemInHand();
		if (hand != null && hand.getType() != org.bukkit.Material.AIR && isAnyPartnerItem(plugin, hand)) {
			if (e.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_AIR
					|| e.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK)
				showCooldown(player, hand);
		}
	}

	@EventHandler
	public void onCheckCooldownEntity(EntityDamageByEntityEvent e) {
		if (!(e.getDamager() instanceof Player))
			return;
		Player player = (Player) e.getDamager();
		ItemStack hand = player.getItemInHand();
		if (hand != null && hand.getType() != org.bukkit.Material.AIR && isAnyPartnerItem(plugin, hand)) {
			if (!VampireItem.matches(plugin, hand) && !MixerItem.matches(plugin, hand)
					&& !HelmetRemoverItem.matches(plugin, hand))
				showCooldown(player, hand);
		}
	}

	private void showCooldown(Player player, ItemStack hand) {
		String itemKey = null, configKey = null;
		if (VampireItem.matches(plugin, hand)) {
			itemKey = VampireItem.KEY;
			configKey = "Vampire";
		} else if (MixerItem.matches(plugin, hand)) {
			itemKey = MixerItem.KEY;
			configKey = "Mixer";
		} else if (AntiFallItem.matches(plugin, hand)) {
			itemKey = AntiFallItem.KEY;
			configKey = "AntiFall";
		} else if (HelmetRemoverItem.matches(plugin, hand)) {
			itemKey = HelmetRemoverItem.KEY;
			configKey = "HelmetRemover";
		} else if (RocketItem.matches(plugin, hand)) {
			itemKey = RocketItem.KEY;
			configKey = "Rocket";
		} else if (SnowballItem.matches(plugin, hand)) {
			itemKey = SnowballItem.KEY;
			configKey = "Snowball";
		} else if (SwitcherItem.matches(plugin, hand)) {
			itemKey = SwitcherItem.KEY;
			configKey = "Switcher";
		}

		if (itemKey != null && plugin.isOnCooldown(player, itemKey)) {
			String coloredName = plugin.color(plugin.getConfig().getString("Items." + configKey + ".name", itemKey));
			player.sendMessage(plugin.color(
					plugin.getConfig().getString("Messages.CooldownCheck", "&7[<item>] &cCooldown restante: &e<time>s")
							.replace("<item>", coloredName)
							.replace("<time>", String.valueOf(plugin.getRemainingCooldown(player, itemKey)))));
		}
	}
}