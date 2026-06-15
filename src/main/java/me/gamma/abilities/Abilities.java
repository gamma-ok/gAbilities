package me.gamma.abilities;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Abilities extends JavaPlugin {

	// Cooldowns individuales por item: Map<itemKey, Map<UUID, expireTime>>
	private final Map<String, Map<UUID, Long>> cooldowns = new HashMap<>();

	// Cooldown global: Map<UUID, expireTime>
	private final Map<UUID, Long> globalCooldowns = new HashMap<>();

	// Anti Fall activos: Map<UUID, expireTime>
	private final Map<UUID, Long> antiFallActive = new HashMap<>();

	// Cascos guardados: Map<UUID víctima, ItemStack casco>
	private final Map<UUID, ItemStack> storedHelmets = new HashMap<>();

	// Contador de hits: Map<itemKey, Map<UUID atacante, Map<UUID víctima,
	// HitData>>>
	private final Map<String, Map<UUID, Map<UUID, HitData>>> hitCounters = new HashMap<>();

	@Override
	public void onEnable() {
		saveDefaultConfig();
		getServer().getPluginManager().registerEvents(new me.gamma.abilities.listeners.ItemListener(this), this);
		getCommand("abilities").setExecutor(new CommandManager(this));
		getLogger().info("Abilities activado correctamente.");
	}

	@Override
	public void onDisable() {
		for (Map.Entry<UUID, ItemStack> entry : storedHelmets.entrySet()) {
			Player p = getServer().getPlayer(entry.getKey());
			if (p != null && p.isOnline()) {
				if (p.getInventory().getHelmet() == null) {
					p.getInventory().setHelmet(entry.getValue());
				} else {
					p.getWorld().dropItemNaturally(p.getLocation(), entry.getValue());
				}
			}
		}
		storedHelmets.clear();
		getLogger().info("PartnerItems desactivado.");
	}

	// ─── Clase interna para datos de hits ────────────────────────

	public static class HitData {
		public int count;
		public long lastHitTime; // System.currentTimeMillis()

		public HitData() {
			this.count = 1;
			this.lastHitTime = System.currentTimeMillis();
		}
	}

	// ─── Hit Counter API ─────────────────────────────────────────

	/**
	 * Registra un hit del atacante sobre la víctima para un item. Devuelve el
	 * número de hits acumulados válidos, o 0 si se reinició.
	 */
	public int registerHit(Player attacker, Player victim, String itemKey) {
		int windowSeconds = getConfig().getInt("Items." + getConfigKey(itemKey) + ".hits-window", 30);
		long windowMs = windowSeconds * 1000L;

		hitCounters.computeIfAbsent(itemKey, k -> new HashMap<>());
		Map<UUID, Map<UUID, HitData>> byAttacker = hitCounters.get(itemKey);

		// Si el atacante golpeó a alguien distinto, limpiar su contador anterior
		byAttacker.computeIfAbsent(attacker.getUniqueId(), k -> new HashMap<>());
		Map<UUID, HitData> attackerMap = byAttacker.get(attacker.getUniqueId());

		// Verificar si hay hits a otra víctima — si es así, limpiar
		for (UUID otherVictim : new java.util.HashSet<>(attackerMap.keySet())) {
			if (!otherVictim.equals(victim.getUniqueId())) {
				attackerMap.remove(otherVictim);
			}
		}

		HitData data = attackerMap.get(victim.getUniqueId());

		if (data == null) {
			data = new HitData();
			attackerMap.put(victim.getUniqueId(), data);

			final HitData hitDataRef = data;
			final UUID attackerUUID = attacker.getUniqueId();
			final UUID victimUUID = victim.getUniqueId();

			getServer().getScheduler().runTaskLater(this, () -> {
				Player attackerOnline = getServer().getPlayer(attackerUUID);
				if (attackerOnline == null || !attackerOnline.isOnline())
					return;

				if (!hitCounters.containsKey(itemKey))
					return;

				// ← Nombre cambiado de byAttacker a innerByAttacker
				Map<UUID, Map<UUID, HitData>> innerByAttacker = hitCounters.get(itemKey);
				if (!innerByAttacker.containsKey(attackerUUID))
					return;

				Map<UUID, HitData> aMap = innerByAttacker.get(attackerUUID);
				if (!aMap.containsKey(victimUUID))
					return;

				if (aMap.get(victimUUID) != hitDataRef)
					return;

				String configKey = getConfigKey(itemKey);
				String itemDisplayName = getConfig().getString("Items." + configKey + ".name", itemKey);
				String msg = getConfig()
						.getString("Messages.HitsExpired",
								"&7[<item>] &cEl tiempo para activar el efecto expiró. Contador reiniciado.")
						.replace("<item>", color(itemDisplayName));
				attackerOnline.sendMessage(color(msg));

				aMap.remove(victimUUID);

			}, windowSeconds * 20L);

			return 1;
		}

		// Verificar si el hit está dentro de la ventana de tiempo
		if (System.currentTimeMillis() - data.lastHitTime > windowMs) {
			// Ventana expirada, reiniciar
			data.count = 1;
			data.lastHitTime = System.currentTimeMillis();
			return 1;
		}

		// Acumular hit
		data.count++;
		data.lastHitTime = System.currentTimeMillis();
		return data.count;
	}

	public void resetHits(Player attacker, Player victim, String itemKey) {
		if (!hitCounters.containsKey(itemKey))
			return;
		Map<UUID, Map<UUID, HitData>> byAttacker = hitCounters.get(itemKey);
		if (!byAttacker.containsKey(attacker.getUniqueId()))
			return;
		byAttacker.get(attacker.getUniqueId()).remove(victim.getUniqueId());
	}

	private String getConfigKey(String itemKey) {
		switch (itemKey) {
		case "vampire":
			return "Vampire";
		case "mixer":
			return "Mixer";
		case "helmet":
			return "HelmetRemover";
		case "grappling":
			return "GrapplingHook";
		case "antifall":
			return "AntiFall";
		case "rocket":
			return "Rocket";
		case "snowball":
			return "Snowball";
		case "switcher":
			return "Switcher";
		default:
			return itemKey;
		}
	}

	// ─── Cooldown individual API ──────────────────────────────────

	public boolean isOnCooldown(Player player, String itemKey) {
		if (!cooldowns.containsKey(itemKey))
			return false;
		Map<UUID, Long> map = cooldowns.get(itemKey);
		UUID uuid = player.getUniqueId();
		if (!map.containsKey(uuid))
			return false;
		if (System.currentTimeMillis() > map.get(uuid)) {
			map.remove(uuid);
			return false;
		}
		return true;
	}

	public long getRemainingCooldown(Player player, String itemKey) {
		if (!cooldowns.containsKey(itemKey))
			return 0;
		Map<UUID, Long> map = cooldowns.get(itemKey);
		UUID uuid = player.getUniqueId();
		if (!map.containsKey(uuid))
			return 0;
		return (map.get(uuid) - System.currentTimeMillis()) / 1000;
	}

	public void setCooldown(Player player, String itemKey, int seconds) {
		cooldowns.computeIfAbsent(itemKey, k -> new HashMap<>()).put(player.getUniqueId(),
				System.currentTimeMillis() + seconds * 1000L);

		// Programar mensaje de cooldown expirado
		if (seconds > 0) {
			String configKey = getConfigKey(itemKey);
			String itemDisplayName = getConfig().getString("Items." + configKey + ".name", itemKey);
			String coloredItemName = color(itemDisplayName);

			getServer().getScheduler().runTaskLater(this, () -> {
				Player p = getServer().getPlayer(player.getUniqueId());
				if (p != null && p.isOnline()) {
					String msg = getConfig()
							.getString("Messages.CooldownExpired",
									"&a[PartnerItems] Ya puedes usar &e<item> &anuevamente.")
							.replace("<item>", coloredItemName);
					p.sendMessage(color(msg));
				}
			}, seconds * 20L);
		}
	}

	// ─── Cooldown global API ──────────────────────────────────────

	public boolean isOnGlobalCooldown(Player player) {
		UUID uuid = player.getUniqueId();
		if (!globalCooldowns.containsKey(uuid))
			return false;
		if (System.currentTimeMillis() > globalCooldowns.get(uuid)) {
			globalCooldowns.remove(uuid);
			return false;
		}
		return true;
	}

	public long getRemainingGlobalCooldown(Player player) {
		UUID uuid = player.getUniqueId();
		if (!globalCooldowns.containsKey(uuid))
			return 0;
		return (globalCooldowns.get(uuid) - System.currentTimeMillis()) / 1000;
	}

	public void setGlobalCooldown(Player player) {
		int seconds = getConfig().getInt("Global-Cooldown", 5);
		if (seconds <= 0)
			return;

		globalCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);

		// Mensaje de cooldown global expirado
		getServer().getScheduler().runTaskLater(this, () -> {
			Player p = getServer().getPlayer(player.getUniqueId());
			if (p != null && p.isOnline()) {
				p.sendMessage(color(getConfig().getString("Messages.GlobalCooldownExpired",
						"&a[PartnerItems] Ya puedes usar partner items nuevamente.")));
			}
		}, seconds * 20L);
	}

	// ─── Anti Fall API ───────────────────────────────────────────

	public void setAntiFall(Player player, int seconds) {
		antiFallActive.put(player.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
	}

	public boolean hasAntiFall(Player player) {
		UUID uuid = player.getUniqueId();
		if (!antiFallActive.containsKey(uuid))
			return false;
		if (System.currentTimeMillis() > antiFallActive.get(uuid)) {
			antiFallActive.remove(uuid);
			return false;
		}
		return true;
	}

	public void removeAntiFall(Player player) {
		antiFallActive.remove(player.getUniqueId());
	}

	// ─── Helmet API ──────────────────────────────────────────────

	public Map<UUID, ItemStack> getStoredHelmets() {
		return storedHelmets;
	}

	public boolean hasStoredHelmet(Player player) {
		return storedHelmets.containsKey(player.getUniqueId());
	}

	// ─── Utilidades ──────────────────────────────────────────────

	public String color(String text) {
		return ChatColor.translateAlternateColorCodes('&', text);
	}

	// ─── Método que elimina el cooldown individual de un item de un jugador

	public void removeCooldown(Player player, String itemKey) {
		if (!cooldowns.containsKey(itemKey))
			return;
		cooldowns.get(itemKey).remove(player.getUniqueId());
	}

	public void removeAllCooldowns(Player player) {
		for (Map<UUID, Long> map : cooldowns.values()) {
			map.remove(player.getUniqueId());
		}
		globalCooldowns.remove(player.getUniqueId());
	}

	// Verifica si hay hits previos registrados para ese par atacante-víctima
	public boolean hasHits(Player attacker, Player victim, String itemKey) {
		if (!hitCounters.containsKey(itemKey))
			return false;
		Map<UUID, Map<UUID, HitData>> byAttacker = hitCounters.get(itemKey);
		if (!byAttacker.containsKey(attacker.getUniqueId()))
			return false;
		return byAttacker.get(attacker.getUniqueId()).containsKey(victim.getUniqueId());
	}

	// Verifica si la ventana de tiempo para los hits ya expiró
	public boolean isHitWindowExpired(Player attacker, Player victim, String itemKey) {
		if (!hasHits(attacker, victim, itemKey))
			return false;
		int windowSeconds = getConfig().getInt("Items." + getConfigKey(itemKey) + ".hits-window", 30);
		long windowMs = windowSeconds * 1000L;
		HitData data = hitCounters.get(itemKey).get(attacker.getUniqueId()).get(victim.getUniqueId());
		return System.currentTimeMillis() - data.lastHitTime > windowMs;
	}
}