package dev.threevhy.client.module;

import dev.threevhy.client.Config;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A feature in the 3vhy mod menu: a card you click to switch on or off, with its own settings. */
public final class Module {
	public enum Category {
		HUD("HUD"),
		PVP("PvP"),
		VISUAL("Visual"),
		UTILITY("Utility");

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
	private boolean toggleable = true;
	private boolean isNew = false;
	private final List<Setting> settings = new ArrayList<>();

	Module(String id, String name, String description, Category category, boolean defaultOn) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.category = category;
		this.defaultOn = defaultOn;
	}

	/** Marks a module that's always on and only has settings (like Theme). */
	Module alwaysOn() {
		this.toggleable = false;
		return this;
	}

	/** Shows a NEW badge on the module's card. */
	Module markNew() {
		this.isNew = true;
		return this;
	}

	<T extends Setting> T add(T setting) {
		setting.owner = this;
		settings.add(setting);
		return setting;
	}

	public List<Setting> settings() {
		return Collections.unmodifiableList(settings);
	}

	public boolean toggleable() {
		return toggleable;
	}

	public boolean isNew() {
		return isNew;
	}

	public boolean enabled() {
		if (!toggleable) return true;
		Boolean value = Config.get().modules.get(id);
		return value != null ? value : defaultOn;
	}

	public void set(boolean enabled) {
		if (!toggleable) return;
		Config.get().modules.put(id, enabled);
		Config.saveSoon();
	}

	public void toggle() {
		set(!enabled());
	}

	public Setting.Toggle toggleSetting(String settingId) {
		return find(settingId, Setting.Toggle.class);
	}

	public Setting.Slider sliderSetting(String settingId) {
		return find(settingId, Setting.Slider.class);
	}

	public Setting.Color colorSetting(String settingId) {
		return find(settingId, Setting.Color.class);
	}

	private <T extends Setting> T find(String settingId, Class<T> type) {
		for (Setting setting : settings) {
			if (setting.id.equals(settingId) && type.isInstance(setting)) return type.cast(setting);
		}
		return null;
	}
}
