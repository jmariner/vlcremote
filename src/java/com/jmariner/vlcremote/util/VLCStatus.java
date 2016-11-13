package com.jmariner.vlcremote.util;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class VLCStatus {

	@Getter(AccessLevel.NONE)
	private Map<String, String> map;

	private String version, album, title, filename, artist, genre, artworkUrl, eqPreset;
	private boolean shuffle, loop, repeat;
	private int time, volume, length, currentID;
	private double position, rate;
	private State state;
	
	@Setter
	private List<String> eqPresets;

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

	public void update(Map<String, String> vlcStatus) {
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
