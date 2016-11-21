package com.jmariner.vlcremote;

import com.jmariner.vlcremote.util.VLCStatus;
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
import java.net.ConnectException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class MyVLCRemote {

	@Getter
	private VLCStatus status;

	private String baseURL;
	private String streamURL;

	private String httpPassword;

	@Getter
	private boolean playingStream, muted;

	@Getter @Setter
	private int playbackVolume;
	private SourceDataLine playbackLine;
	private FloatControl gainControl;

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

		playbackVolume = 100;
		
		playingStream = false;
		muted = false;

		status = new VLCStatus();

		if (testConnection()) {
			getNewStatus();
			updatePlaylist();
			if (!status.playlistExists()) {
				status.loadMediaLibrary(connect(LIBRARY_REQUEST));
				if (status.libraryExists()) {
					String first = status.getLibraryFolders().keySet().iterator().next();
					switchAlbum(first);
				}
			}
		}
	}

	private void playStream(int msDelay) {

		Thread playbackThread = new Thread(() -> {
			if (msDelay > 0) {
				try { Thread.sleep(msDelay); }
				catch (InterruptedException ignored) {}
			}
			streamSampledAudio(streamURL);
		});

		playbackThread.start();
	}

	public void playStream() { playStream(0); }

	public void stopStream() {
		playingStream = false;
		playbackLine.stop();
	}

	public void restartStream() {
		stopStream();
		playStream(1000);
	}

	/** Adapted from http://archive.oreilly.com/onjava/excerpt/jenut3_ch17/examples/PlaySoundStream.java
	 *  Explained at http://archive.oreilly.com/onjava/excerpt/jenut3_ch17/
	 */
	private void streamSampledAudio(String urlString)  {

		AudioInputStream ain = null;  // We read audio data from here
		playbackLine = null;   // And write it here.

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
			playbackLine = (SourceDataLine) AudioSystem.getLine(info);
			playbackLine.open(format);
			
			gainControl = (FloatControl) playbackLine.getControl(FloatControl.Type.MASTER_GAIN);

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
					playbackLine.start();
					started = true;
				}

				// We must write bytes to the line in an integer multiple of
				// the frameSize.  So figure out how many bytes we'll write.
				int bytesToWrite = (numBytes / frameSize) * frameSize;

				// Now write the bytes. The line will buffer them and play
				// them. This call will block until all bytes are written.
				playbackLine.write(buffer, 0, bytesToWrite);

				// If we didn't have an integer multiple of the frame size,
				// then copy the remaining bytes to the start of the buffer.
				int remaining = numBytes - bytesToWrite;
				if (remaining > 0)
					System.arraycopy(buffer, bytesToWrite, buffer, 0, remaining);
				numBytes = remaining;

				if (!muted && gainControl.getValue() != convertVolume(playbackVolume)) {
					if (playbackVolume > 200) playbackVolume = 200;
					if (playbackVolume < 0) playbackVolume = 0;
					gainControl.setValue(convertVolume(playbackVolume));
				}

			}

			// Now block until all buffered sound finishes playing.
			playbackLine.drain();
		}
		catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
			e.printStackTrace();
		}
		finally { // Always relinquish the resources we use
			try {
				if (playbackLine != null) playbackLine.close();
				if (ain != null) ain.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void toggleMute() {
		setMuted(!muted);
	}
	
	public void setMuted(boolean m) {
		muted = m;
		gainControl.setValue(convertVolume(m ? playbackVolume : 0));
	}

	public void incrementVolume(int percentToChange) {
		playbackVolume += percentToChange;
	}
	
	private static float convertVolume(int percentVolume) {
		double percentVol = percentVolume / 100.0;
		return (float) (Math.log(percentVol) / Math.log(10.0) * 20.0);
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
		if (!playingStream)
			playStream();
		return sendCommand(Command.PLAY_ITEM, ""+playlistID);
	}

	private static String encodeUrlParam(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
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
