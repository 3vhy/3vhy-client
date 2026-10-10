package dev.threevhy.client.music;

import dev.threevhy.client.Hooks;
import dev.threevhy.client.compat.Compat;
import net.minecraft.resources.Identifier;

/** The current song's cover as a texture. Uploaded once per song, on the render thread. */
public final class MusicArt {
	private static final Identifier ID = Identifier.fromNamespaceAndPath("threevhy", "music_art");
	private static long uploadedVersion = -1;
	private static int[] size = null;

	private MusicArt() {
	}

	/** The cover texture for this song, or null when it has none (yet). */
	public static Identifier texture(NowPlaying song) {
		if (song == null || song.art() == null) return null;
		if (song.artVersion() != uploadedVersion) {
			uploadedVersion = song.artVersion();
			size = Compat.uploadTexture(ID, song.art());
			if (size != null) Hooks.musicArtUploaded = true;
		}
		return size != null ? ID : null;
	}

	/** The uploaded cover's {width, height}; only meaningful after {@link #texture} returned an id. */
	public static int[] size() {
		return size;
	}
}
