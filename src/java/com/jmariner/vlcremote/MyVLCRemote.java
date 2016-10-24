package com.jmariner.vlcremote;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.conn.ConnectTimeoutException;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Slf4j
public class MyVLCRemote {

	private String baseURL;
	private String streamURL;

	private String httpPassword;

	private int firstID;
	private int playlistLength;

	private boolean playingStream;

	@Setter
	private double playbackVolume;

	@Setter
	private Consumer<Throwable> exceptionHandler;

	@Getter
	private Map<Integer, SongItem> songMap;

	public MyVLCRemote(String host, int webPort, String password, int streamPort) {
		baseURL = String.format("http://%s:%s/", host, webPort);
		streamURL = String.format("http://%s:%s/", host, streamPort);
		httpPassword = password;

		playbackVolume = 1;

		loadSongList();

	}

	private void loadSongList() {

		songMap = new HashMap<>();

		getPlaylist().forEach(s -> {
			int id = Integer.parseInt(s.get("id"));
			assert !songMap.containsKey(id);

			songMap.put(id, new SongItem(
					id, s.get("title"), s.get("artist"), s.get("album"), Integer.parseInt(s.get("duration"))
			));

		});
	}

	public void playStream() {

		Thread playbackThread = new Thread(() -> {
			streamSampledAudio(streamURL);
		});

		playbackThread.start();

	}

	public void stopStream() {
		playingStream = false;
	}

	/** Adapted from http://archive.oreilly.com/onjava/excerpt/jenut3_ch17/examples/PlaySoundStream.java
	 *  Explained at http://archive.oreilly.com/onjava/excerpt/jenut3_ch17/
	 */
	private void streamSampledAudio(String urlString)  {

		AudioInputStream ain = null;  // We read audio data from here
		SourceDataLine line = null;   // And write it here.

		try {

			URL url = new URL(urlString);

			// Get an audio input stream from the URL
			ain = AudioSystem.getAudioInputStream(url);

			// Get information about the format of the stream
			AudioFormat format = ain.getFormat();
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

			// If the format is not supported directly (i.e. if it is not PCM
			// encoded), then try to transcode it to PCM.
			if (!AudioSystem.isLineSupported(info)) {
				// This is the PCM format we want to transcode to.
				// The parameters here are audio format details that you
				// shouldn't need to understand for casual use.
				AudioFormat pcm = new AudioFormat(format.getSampleRate(), 16, format.getChannels(), true, false);

				// Get a wrapper stream around the input stream that does the transcoding for us.
				ain = AudioSystem.getAudioInputStream(pcm, ain);

				// Update the format and info variables for the transcoded data
				format = ain.getFormat();
				info = new DataLine.Info(SourceDataLine.class, format);
			}

			// Open the line through which we'll play the streaming audio.
			line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(format);

			FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);

			// Allocate a buffer for reading from the input stream and writing
			// to the line.  Make it large enough to hold 4k audio frames.
			// Note that the SourceDataLine also has its own internal buffer.
			int frameSize = format.getFrameSize();
			byte[] buffer = new byte[4 * 1024 * frameSize]; // the buffer
			int numBytes = 0;                               // how many bytes

			// We haven't started the line yet.
			boolean started = false;
			playingStream = true;

			while (playingStream) {  // We'll exit the loop when we reach the end of stream
				// First, read some bytes from the input stream.
				int bytesRead = ain.read(buffer, numBytes, buffer.length - numBytes);
				// If there were no more bytes to read, we're done.
				if (bytesRead == -1) break;
				numBytes += bytesRead;

				// Now that we've got some audio data to write to the line,
				// start the line, so it will play that data as we write it.
				if (!started) {
					line.start();
					started = true;
				}

				// We must write bytes to the line in an integer multiple of
				// the frameSize.  So figure out how many bytes we'll write.
				int bytesToWrite = (numBytes / frameSize) * frameSize;

				// Now write the bytes. The line will buffer them and play
				// them. This call will block until all bytes are written.
				line.write(buffer, 0, bytesToWrite);

				// If we didn't have an integer multiple of the frame size,
				// then copy the remaining bytes to the start of the buffer.
				int remaining = numBytes - bytesToWrite;
				if (remaining > 0)
					System.arraycopy(buffer, bytesToWrite, buffer, 0, remaining);
				numBytes = remaining;

				if (gainControl.getValue() != convertVolume(playbackVolume)) {
					if (playbackVolume > 2) playbackVolume = 2;
					if (playbackVolume < 0) playbackVolume = 0;
					gainControl.setValue(convertVolume(playbackVolume));
				}
				//log.info(gainControl.getValue()+"");

			}

			// Now block until all buffered sound finishes playing.
			line.drain();
		}
		catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
			e.printStackTrace();
		}
		finally { // Always relinquish the resources we use
			try {
				if (line != null) line.close();
				if (ain != null) ain.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static float convertVolume(double volumePercent) {
		return (float) (Math.log(volumePercent) / Math.log(10.0) * 20.0);
	}

	public boolean testConnection() {
		return connect("") != null;
	}

	private String connect(String location) {

		try {

			HttpResponse<String> response =
					Unirest.get(baseURL + location)
					.basicAuth("", httpPassword)
					.asString();

			int status = response.getStatus();
			if (status == 401) {
				String pass = new String(new char[httpPassword.length()]).replace("\0", "*");
				String msg = String.format("HTTP 401 Exception: Invalid credentials. (pass: %s)", StringUtils.isBlank(pass) ? "<none>" : pass);
				throw new ConnectException(msg);
			}
			if (status == 404) {
				throw new ConnectException("HTTP 404 Not Found: " + baseURL + location);
			}

			return response.getBody();
		}
		catch (ConnectException e) {
			log.error(e.getMessage());
			if (exceptionHandler != null)
				exceptionHandler.accept(e);
		}
		catch (UnirestException e) {
			if (e.getCause() instanceof UnknownHostException)
				log.error("Unknown host: " + baseURL);
			else if (e.getCause() instanceof ConnectTimeoutException)
				log.error(e.getCause().getMessage());
			else
				e.printStackTrace();

			if (exceptionHandler != null)
				exceptionHandler.accept(e.getCause());
		}
		return null;
	}

	public List<Map<String, String>> getPlaylist() {
		return parsePlaylistJson(connect("custom/playlist.json"));
	}

	public Map<String, String> getStatus() {
		return parseStatusJson(connect("requests/status.json"));
	}

	public Map<String, String> sendCommand(Command cmd) {
		return sendCommand(cmd, null);
	}

	public Map<String, String> sendCommand(Command cmd, String val) {

		String append = val == null ? "" : String.format("&%s=%s", cmd.getParamName(), encodeUrlParam(val));
		connect("requests/status.json?command=" + cmd + append);
		return getStatus();
	}

	public Map<String, String> setSourceVolume(double percentVolume) {
		if (percentVolume < 0) percentVolume = 0;
		if (percentVolume > 1.25) percentVolume = 1.25;

		return sendCommand(Command.SET_VOLUME, ""+(percentVolume * 256));
	}

	public Map<String, String> switchSongZeroBased(int zeroBasedID) {
		if (zeroBasedID < 0)
			zeroBasedID = 0;
		if (zeroBasedID > playlistLength-1)
			zeroBasedID = playlistLength-1;

		return sendCommand(Command.PLAY_ITEM, ""+transformZeroBasedID(zeroBasedID));
	}

	public Map<String, String> switchSong(int playlistID) {
		return switchSongZeroBased(transformPlaylistID(playlistID));
	}

	public int transformZeroBasedID(int zeroBasedSongID) {
		return zeroBasedSongID + firstID;
	}

	public int transformPlaylistID(int playlistID) {
		return playlistID - firstID;
	}

	private static Map<String, String> parseStatusJson(String json)  {

		Map<String, String> out = new HashMap<>();

		JsonElement el = new JsonParser().parse(json);

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

			JsonElement info = j.get("information");
			assert info.isJsonObject();

			JsonElement category = info.getAsJsonObject().get("category");
			assert category.isJsonObject();

			JsonElement meta = category.getAsJsonObject().get("meta");
			assert meta.isJsonObject();

			Stream.of("album", "title", "filename", "artist", "genre", "artwork_url").forEach(s ->
				out.put(s, meta.getAsJsonObject().get(s) == null ? null : meta.getAsJsonObject().get(s).getAsString())
			);
		}
		return out;
	}
	
	private List<Map<String, String>> parsePlaylistJson(String json) {
		List<Map<String, String>> out = new ArrayList<>();

		JsonElement root = new JsonParser().parse(json);
		assert root.isJsonArray();
		JsonArray items = root.getAsJsonArray();

		firstID = -1;

		items.getAsJsonArray().forEach(i -> {
			Map<String, String> item = new HashMap<>();
			JsonObject o = i.getAsJsonObject();

			String id = o.get("id").getAsString();
			item.put("id", id);
			if (firstID == -1)
				firstID = Integer.parseInt(id);

			Stream.of("duration", "title", "name", "artist", "album").forEach(
					s -> item.put(s, o.get(s).getAsString())
			);

			item.put("current", o.get("current") == null ? "false" : "true");

			out.add(item);
		});

		playlistLength = out.size();

		return out;
	}

	private static String encodeUrlParam(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static String decodeUrlParam(String s) {
		try {
			return URLDecoder.decode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Getter
	@AllArgsConstructor
	enum Command {
		PLAY			("pl_play", 	"Play: Start or resume playback"),
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
		SEEK_TO			("seek", 	"Seek To: Seek to a point in playback. Supported: +<val>, -<val>, <val> where <val> is seconds or #h#m#s", "val"),
		PLAY_ITEM		("pl_play",	"Play Item: Play a playlist item by it's ID", "id");

		String cmd;
		String description;
		String paramName;

		Command(String cmd, String desc) {
			this(cmd, desc, null);
		}

		@Override
		public String toString() {
			return cmd;
		}

		public static List<String> keys() {
			List<String> out = new ArrayList<>();
			for (Command c : Command.values()) {
				out.add(c.name());
			}
			return out;
		}
	}

}
