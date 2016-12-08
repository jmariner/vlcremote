package com.jmariner.vlcremote;

import com.jmariner.vlcremote.util.MediaStreamPlayer;
import com.jmariner.vlcremote.util.VLCStatus;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MyVLCRemote {

	@Getter
	private VLCStatus status;

	private String baseURL;
	private String streamURL;
	private String httpPassword;
	
	@Getter
	private boolean connected;
	
	@Getter
	private MediaStreamPlayer player;

	@Setter
	private Consumer<Throwable> exceptionHandler;

	private static final String STATUS_REQUEST = 	"custom/status.json";
	private static final String PLAYLIST_REQUEST = 	"custom/playlist.json";
	private static final String LIBRARY_REQUEST = 	"custom/library.json";
	
	public MyVLCRemote(String host, int webPort, String password, int streamPort, Consumer<Throwable> handler) {
		baseURL = String.format("http://%s:%s/", host, webPort);
		streamURL = String.format("http://%s:%s/", host, streamPort);
		httpPassword = password;
		exceptionHandler = handler;
		
		connected = false;
		
		player = new MediaStreamPlayer(streamURL);

		status = new VLCStatus();

		if (testConnection()) {
			getNewStatus();
			updatePlaylist();
			updateLibrary();
			if (status.libraryExists() && !status.playlistExists()) {
				String first = status.getLibraryFolders().keySet().iterator().next();
				switchAlbum(first);
			}
			
			connected = true;
		}
	}

	public boolean testConnection() {
		connected = connect("") != null;
		return connected;
		
	}

	private String connect(String location) {
		try {

			HttpResponse<String> response =
					Unirest.get(baseURL + location)
					.basicAuth("", httpPassword)
					.asString();

			int status = response.getStatus();
			if (status == 401)
				throw new ConnectException("HTTP 401 Exception: Invalid credentials.");
			if (status == 404)
				throw new ConnectException("HTTP 404 Not Found: " + baseURL + location);

			return response.getBody();
		}
		catch (UnirestException | ConnectException e) {
			ConnectException conEx;
			if (e instanceof UnirestException) {
	
				Throwable cause = e.getCause();
				String msg = (cause instanceof UnknownHostException) ? 
							"Unknown host: " + baseURL :
							cause.getMessage();
	
				conEx = new ConnectException(msg);
				conEx.initCause(e.getCause());
			}
			else
				conEx = (ConnectException) e;
			
			conEx.printStackTrace();
			if (exceptionHandler != null)
				exceptionHandler.accept(conEx);
		}
		return null;
	}
	
	private void updateLibrary() {
		status.loadMediaLibrary(connect(LIBRARY_REQUEST));
	}

	private void updatePlaylist() {
		status.loadPlaylist(connect(PLAYLIST_REQUEST));
	}

	private void updateStatus() {
		status.loadStatus(connect(STATUS_REQUEST));
	}

	public VLCStatus getNewStatus() {
		updateStatus();
		return status;
	}

	public VLCStatus switchAlbum(String newAlbum) {
		String album = status.getLibraryFolders().get(newAlbum);

		connect(STATUS_REQUEST + "?command=pl_empty");
		connect(STATUS_REQUEST + "?command=in_play&input=" + album);

		try {
			do {
				Thread.sleep(500);
				updatePlaylist();
			} while (!status.playlistExists());
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		return getNewStatus();
	}

	public VLCStatus sendCommand(Command cmd) {
		return sendCommand(cmd, null);
	}

	public VLCStatus sendCommand(Command cmd, String val) {

		String append = val == null ? "" : String.format("&%s=%s", cmd.getParamName(), encodeUrlParam(val));
		connect(STATUS_REQUEST + "?command=" + cmd + append);
		return getNewStatus();
	}

	public VLCStatus setSourceVolume(double percentVolume) {
		if (percentVolume < 0) percentVolume = 0;
		if (percentVolume > 1.25) percentVolume = 1.25;

		return sendCommand(Command.SET_VOLUME, ""+(percentVolume * 256));
	}

	public VLCStatus switchSong(int playlistID) {
		if (!player.isPlaying())
			player.start();
		return sendCommand(Command.PLAY_ITEM, ""+playlistID);
	}

	private static String encodeUrlParam(String s) {
		try {
			return URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Getter
	@AllArgsConstructor
	public enum Command {
		PLAY			("pl_play", 	"Play: Start or resume playback"),
		TOGGLE_PLAY		("pl_pause",	"Toggle playback"),
		PAUSE			("pl_pause", 	"Pause: Pause playback"),
		STOP			("pl_stop", 	"Stop: Stop playback"),
		NEXT			("pl_next", 	"Next: Jump to next playlist item"),
		PREV			("pl_previous", "Previous: Jump to previous playlist item"),
		PREVIOUS		("pl_previous", "Previous: Jump to previous playlist item"),
		TOGGLE_RANDOM 	("pl_random", 	"Shuffle: Toggle random (shuffled) playback"),
		TOGGLE_LOOP		("pl_loop", 	"Loop: Toggle looped playback"),
		TOGGLE_REPEAT	("pl_repeat", 	"Repeat: Toggle single-song repeated playback"),
		SET_VOLUME		("volume", 	"Set Volume: Set or change (with +<int>, -<int>, or <int>%) the volume level", "val"),
		SET_RATE		("rate", 	"Set Rate: Set the playback rate. Default is 1", "val"),
		SET_EQ_ENABLED	("enableeq",	"Turn on or off the equalizer. 0=off, 1=on", "val"),
		SET_EQ_PRESET	("setpreset",	"Set the equalizer preset by ID", "val"),
		SEEK_TO			("seek", 	"Seek To: Seek to a point in playback. Supported: +<val>, -<val>, <val> where <val> is seconds or #h#m#s", "val"),
		PLAY_ITEM		("pl_play",	"Play Item: Play a playlist item by it's ID", "id");

		String cmd, description, paramName;
		
		private static final List<String> KEYS =
				Arrays.stream(Command.values())
				.map(Command::name)
				.collect(Collectors.toList());

		Command(String cmd, String desc) {
			this(cmd, desc, null);
		}

		@Override
		public String toString() {
			return cmd;
		}
		
		public static Command forName(String name) {
			if (KEYS.contains(name))
				return Command.valueOf(name);
			else
				return null;
		}
	}

}
