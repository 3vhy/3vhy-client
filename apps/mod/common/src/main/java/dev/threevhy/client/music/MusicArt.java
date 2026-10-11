package dev.threevhy.client.music;

import dev.threevhy.client.Hooks;
import dev.threevhy.client.compat.Compat;
import net.minecraft.resources.Identifier;

/** A picture from the music helper (the song's cover or the app's icon) as a texture, uploaded once per change. */
public final class MusicArt {
	public static final MusicArt COVER = new MusicArt("music_art");
	public static final MusicArt ICON = new MusicArt("music_icon");

	private final Identifier id;
	private long uploadedVersion = -1;
	private int[] size = null;

	private MusicArt(String path) {
		this.id = Identifier.fromNamespaceAndPath("threevhy", path);
	}

	/** The texture for this picture, or null when there isn't one (yet). Render thread only. */
	public Identifier texture(byte[] png, long version) {
		if (png == null) return null;
		if (version != uploadedVersion) {
			uploadedVersion = version;
			size = Compat.uploadTexture(id, png);
			if (size != null) {
				if (this == COVER) Hooks.musicArtUploaded = true;
				else Hooks.musicIconUploaded = true;
			}
		}
		return size != null ? id : null;
	}

	/** The uploaded picture's {width, height}; only meaningful after {@link #texture} returned an id. */
	public int[] size() {
		return size;
	}
}
