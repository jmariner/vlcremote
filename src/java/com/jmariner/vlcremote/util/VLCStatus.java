package com.jmariner.vlcremote.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jmariner.vlcremote.SongItem;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class VLCStatus {

	@Getter(AccessLevel.NONE)
	private Map<String, String> map;

	private String version, album, title, filename, artist, genre, artworkUrl, eqPreset;
	private boolean shuffle, loop, repeat;
	private int time, volume, length, currentID;
	private double position, rate;
	private State state;

	private List<String> eqPresets;
	private Map<Integer, SongItem> songMap;
	private LinkedHashMap<String, String> libraryFolders;

	@Getter(AccessLevel.NONE)
	private boolean playlistExists, libraryExists;

	private static final JsonParser PARSER = new JsonParser();

	public VLCStatus() {
		eqPresets = new ArrayList<>();
		songMap = new HashMap<>();
	}

	private String get(String key) {
		return map.get(key);
	}
	private int getInt(String key) {
		return Integer.parseInt(get(key));
	}
	private boolean getBoolean(String key) {
		return Boolean.parseBoolean(get(key));
	}
	private double getDouble(String key) {
		return Double.parseDouble(get(key));
	}

	public void loadStatus(String json) {
		loadMap(parseStatusJson(json));
		eqPresets = parseStatusForEqualizerOptions(json);
	}

	public void loadPlaylist(String json) {
		List<Map<String, String>> playlist = parsePlaylistJson(json);

		playlistExists = playlist.size() > 0;

		songMap.clear();
		if (playlistExists) {
			playlist.forEach(s -> {
				int id = Integer.parseInt(s.get("id"));
				assert !songMap.containsKey(id);

				songMap.put(id, new SongItem(
						id, s.get("title"), s.get("artist"), s.get("album"), Integer.parseInt(s.get("duration"))
				));

			});
		}
	}

	public void loadMediaLibrary(String json) {
		libraryFolders = parseLibraryJson(json);
		libraryExists = libraryFolders != null && libraryFolders.size() > 0;
	}

	public SongItem getCurrentSong() {
		return songMap.get(currentID);
	}

	public boolean playlistExists() { return playlistExists; }

	public boolean libraryExists() { return libraryExists; }

	public String getCurrentAlbum() {
		return songMap.values().iterator().next().getAlbum();
	}

	private void loadMap(Map<String, String> vlcStatus) {
		this.map = vlcStatus;

		this.version = get("version");
		this.album = get("album");
		this.title = get("title");
		this.filename = get("filename");
		this.artist = get("artist");
		this.genre = get("genre");
		this.artworkUrl = get("artwork_url");
		this.eqPreset = get("eqPreset");

		this.shuffle = getBoolean("random");
		this.loop = getBoolean("loop");
		this.repeat = getBoolean("repeat");

		this.time = getInt("time");
		this.volume = getInt("volume");
		this.length = getInt("length");
		this.currentID = getInt("currentID");

		this.position = getDouble("position");
		this.rate = getDouble("rate");

		String state = get("state").toUpperCase();
		this.state = State.keys().contains(state) ? State.valueOf(state) : State.UNKNOWN;
	}

	private static Map<String, String> parseStatusJson(String json)  {

		Map<String, String> out = new HashMap<>();

		JsonElement el = PARSER.parse(json);

		if (el.isJsonObject()) {
			JsonObject j = el.getAsJsonObject();
			Stream.of(
					"time", "volume", "length", "random", "rate", "state", "loop", "version", "position", "repeat", "currentplid"
			).forEach(s -> {
				String key = s.equals("currentplid") ? "currentID" : s;
				String val = j.get(s) == null ? "" : j.get(s).getAsString();
				out.put(key, val);
			});

			if (out.get("state").equals("stopped")) return out;

			JsonElement eq = j.get("equalizer");
			assert eq.isJsonObject();

			JsonElement preset = eq.getAsJsonObject().get("preset");
			out.put("eqPreset", preset == null ? null : preset.getAsString());

			JsonElement info = j.get("information");
			assert info.isJsonObject();

			JsonElement category = info.getAsJsonObject().get("category");
			assert category.isJsonObject();

			JsonElement meta = category.getAsJsonObject().get("meta");
			assert meta.isJsonObject();

			Stream.of("album", "title", "filename", "artist", "genre", "artwork_url").forEach(s -> {
				JsonElement e =  meta.getAsJsonObject().get(s);
				out.put(s, e == null ? null : e.getAsString());
			});
		}
		return out;
	}

	private static List<String> parseStatusForEqualizerOptions(String json) {

		List<String> out = new ArrayList<>();

		JsonElement el = PARSER.parse(json);

		if (el.isJsonObject()) {
			JsonElement eq = el.getAsJsonObject().get("equalizer");
			assert eq.isJsonObject();

			JsonElement presetsEl = eq.getAsJsonObject().get("presets");
			assert presetsEl.isJsonObject();

			JsonObject presets = presetsEl.getAsJsonObject();
			for (int i=0, l=presets.size(); i<l; i++) {
				out.add(presets.get(String.format("preset id=\"%d\"", i)).getAsString());
			}
		}
		return out;

	}

	private static List<Map<String, String>> parsePlaylistJson(String json) {
		List<Map<String, String>> out = new ArrayList<>();

		JsonElement root = PARSER.parse(json);
		assert root.isJsonObject();

		JsonElement playlist = root.getAsJsonObject().get("children");
		assert playlist.isJsonArray();

		playlist.getAsJsonArray().forEach(i -> {
			Map<String, String> item = new HashMap<>();
			JsonObject o = i.getAsJsonObject();

			String id = o.get("id").getAsString();
			item.put("id", id);

			Stream.of("duration", "title", "name", "artist", "album").forEach(
					s -> item.put(s, o.get(s).getAsString())
			);

			item.put("current", o.get("current") == null ? "false" : "true");

			out.add(item);
		});

		return out;
	}

	private static LinkedHashMap<String, String> parseLibraryJson(String json) {
		LinkedHashMap<String, String> out = new LinkedHashMap<>();

		JsonElement root = PARSER.parse(json);
		assert root.isJsonObject();
		assert root.getAsJsonObject().get("name").getAsString().equals("Media Library");

		JsonElement libraries = root.getAsJsonObject().get("children");
		if (libraries == null) return null;
		assert libraries.isJsonArray();

		libraries.getAsJsonArray().forEach(e -> {
			JsonObject o = e.getAsJsonObject();
			out.put(
					o.get("name").getAsString(),
					o.get("uri").getAsString().replaceFirst("directory", "file")
			);
		});

		return out;
	}

	public enum State {
		PLAYING,
		PAUSED,
		STOPPED,
		UNKNOWN;

		public static List<String> keys() {
			return Arrays.asList(State.values())
					.stream()
					.map(Enum::name)
					.collect(Collectors.toList());
		}
	}

}
