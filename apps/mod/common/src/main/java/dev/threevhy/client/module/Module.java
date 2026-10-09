package dev.threevhy.client.module;

import dev.threevhy.client.Config;

/** A feature the player can switch on and off in 3vhy Settings. */
public final class Module {
	public enum Category {
		HUD("HUD"),
		PVP("PvP"),
		UTILITY("Utility"),
		DISCORD("Discord");

		public final String label;

		Category(String label) {
			this.label = label;
		}
	}

	public final String id;
	public final String name;
	public final String description;
	public final Category category;
	private final boolean defaultOn;

	Module(String id, String name, String description, Category category, boolean defaultOn) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.category = category;
		this.defaultOn = defaultOn;
	}

	public boolean enabled() {
		Boolean value = Config.get().modules.get(id);
		return value != null ? value : defaultOn;
	}

	public void set(boolean enabled) {
		Config.get().modules.put(id, enabled);
		Config.saveSoon();
	}

	public void toggle() {
		set(!enabled());
	}
}
