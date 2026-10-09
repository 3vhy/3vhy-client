package dev.threevhy.client.discord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A tiny client for Discord's local RPC: a named pipe on Windows, a Unix socket elsewhere.
 * Frames are [opcode int32 LE][length int32 LE][JSON].
 */
final class DiscordIpc implements AutoCloseable {
	private static final int OP_HANDSHAKE = 0;
	private static final int OP_FRAME = 1;
	private static final int OP_CLOSE = 2;

	private interface Pipe extends AutoCloseable {
		void write(byte[] data) throws IOException;

		void readFully(byte[] into) throws IOException;

		@Override
		void close() throws IOException;
	}

	private final Pipe pipe;

	private DiscordIpc(Pipe pipe) {
		this.pipe = pipe;
	}

	/** Connects and says hello. Throws if Discord isn't running. */
	static DiscordIpc connect(String clientId) throws IOException {
		IOException last = new IOException("Discord isn't running");
		for (Pipe pipe : candidates()) {
			try {
				DiscordIpc ipc = new DiscordIpc(pipe);
				JsonObject hello = new JsonObject();
				hello.addProperty("v", 1);
				hello.addProperty("client_id", clientId);
				ipc.send(OP_HANDSHAKE, hello);
				ipc.receive();
				return ipc;
			} catch (IOException e) {
				last = e;
				try {
					pipe.close();
				} catch (IOException ignored) {
					// already broken
				}
			}
		}
		throw last;
	}

	void setActivity(JsonObject activity) throws IOException {
		JsonObject args = new JsonObject();
		args.addProperty("pid", ProcessHandle.current().pid());
		if (activity != null) {
			args.add("activity", activity);
		}
		JsonObject frame = new JsonObject();
		frame.addProperty("cmd", "SET_ACTIVITY");
		frame.add("args", args);
		frame.addProperty("nonce", UUID.randomUUID().toString());
		send(OP_FRAME, frame);
		receive();
	}

	private void send(int opcode, JsonObject json) throws IOException {
		byte[] body = json.toString().getBytes(StandardCharsets.UTF_8);
		ByteBuffer buffer = ByteBuffer.allocate(8 + body.length).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(opcode).putInt(body.length).put(body);
		pipe.write(buffer.array());
	}

	private JsonObject receive() throws IOException {
		byte[] header = new byte[8];
		pipe.readFully(header);
		ByteBuffer h = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
		int opcode = h.getInt();
		int length = h.getInt();
		if (length < 0 || length > 1 << 20) {
			throw new IOException("Bad frame from Discord");
		}
		byte[] body = new byte[length];
		pipe.readFully(body);
		if (opcode == OP_CLOSE) {
			throw new IOException("Discord closed the connection: " + new String(body, StandardCharsets.UTF_8));
		}
		try {
			return JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (RuntimeException e) {
			return new JsonObject();
		}
	}

	@Override
	public void close() {
		try {
			pipe.close();
		} catch (IOException ignored) {
			// nothing to do
		}
	}

	// ---------- Finding Discord ----------

	private static List<Pipe> candidates() {
		List<Pipe> pipes = new ArrayList<>();
		boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
		for (int i = 0; i < 10; i++) {
			if (windows) {
				try {
					pipes.add(new WindowsPipe("\\\\.\\pipe\\discord-ipc-" + i));
				} catch (IOException ignored) {
					// not this one
				}
			} else {
				for (Path dir : unixDirs()) {
					for (String sub : new String[] {"", "app/com.discordapp.Discord/", "snap.discord/",
							".flatpak/dev.vencord.Vesktop/xdg-run/"}) {
						Path socket = dir.resolve(sub + "discord-ipc-" + i);
						if (Files.exists(socket)) {
							try {
								pipes.add(new UnixPipe(socket));
							} catch (IOException ignored) {
								// not this one
							}
						}
					}
				}
			}
			if (!pipes.isEmpty()) break;
		}
		return pipes;
	}

	private static List<Path> unixDirs() {
		List<Path> dirs = new ArrayList<>();
		for (String env : new String[] {"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"}) {
			String value = System.getenv(env);
			if (value != null && !value.isEmpty()) dirs.add(Path.of(value));
		}
		dirs.add(Path.of("/tmp"));
		return dirs;
	}

	private static final class WindowsPipe implements Pipe {
		private final RandomAccessFile file;

		WindowsPipe(String name) throws IOException {
			this.file = new RandomAccessFile(name, "rw");
		}

		@Override
		public void write(byte[] data) throws IOException {
			file.write(data);
		}

		@Override
		public void readFully(byte[] into) throws IOException {
			file.readFully(into);
		}

		@Override
		public void close() throws IOException {
			file.close();
		}
	}

	private static final class UnixPipe implements Pipe {
		private final SocketChannel channel;

		UnixPipe(Path path) throws IOException {
			this.channel = SocketChannel.open(UnixDomainSocketAddress.of(path));
		}

		@Override
		public void write(byte[] data) throws IOException {
			ByteBuffer buffer = ByteBuffer.wrap(data);
			while (buffer.hasRemaining()) channel.write(buffer);
		}

		@Override
		public void readFully(byte[] into) throws IOException {
			ByteBuffer buffer = ByteBuffer.wrap(into);
			while (buffer.hasRemaining()) {
				if (channel.read(buffer) < 0) throw new IOException("Discord went away");
			}
		}

		@Override
		public void close() throws IOException {
			channel.close();
		}
	}
}
