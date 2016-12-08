package com.jmariner.vlcremote.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import lombok.Getter;

public class MediaStreamPlayer {
	
	@Getter
	private boolean playing, muted;
	private boolean waitingDelayRestart;

	@Getter
	private int volume;
	private SourceDataLine playbackLine;
	private FloatControl gainControl;
	
	private Thread playbackThread;
	
	private URL url;
	
	public MediaStreamPlayer(String mediaURL) {
		this.playing = false;
		this.muted = false;
		this.volume = 100;
		
		try {
			this.url = new URL(mediaURL);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void start(int delay) {
		
		if (playing) return;
		
		if (waitingDelayRestart)
			playbackThread.interrupt();

		playbackThread = new Thread(() -> {
			boolean interrupted = false;
			if (delay > 0) {
				try {
					waitingDelayRestart = true;
					Thread.sleep(delay);
					waitingDelayRestart = false;
				}
				catch (InterruptedException e) {
					interrupted = true;
				}
			}
			if (!interrupted)
				play();
		}, "Playback");

		playbackThread.start();
	}
	
	public void start() { start(0); }
	
	public void stop() {
		playing = false;
		playbackLine.stop();
		playbackLine.close();
	}
	
	// TODO improve this to where we don't need to close and reopen the stream every "restart"
	// or fix how playback becomes out of sync with source stream
	public void restart(int delay) {
		stop();
		start(delay);
	}
	
	public void toggleMute() { setMuted(!muted); }

	public void setMuted(boolean m) {
		muted = m;
		int vol = muted ? 0 : volume;
		gainControl.setValue(toGain(vol));
	}
	
	public void setVolume(int vol) {
		this.volume = vol;
		if (volume > 200) volume = 200;
		if (volume < 0) volume = 0;
		
		if (gainControl.getValue() != toGain(volume))
			gainControl.setValue(toGain(volume));
	}

	public void incrementVolume(int change) {
		setVolume(volume + change);
	}
	
	private void play() {
		try (AudioInputStream audioIn = AudioSystem.getAudioInputStream(url)) {
			
			AudioFormat outFormat = transcodePCM(audioIn.getFormat());
			Info outInfo = new Info(SourceDataLine.class, outFormat);
			
			try (AudioInputStream transcodedIn = AudioSystem.getAudioInputStream(outFormat, audioIn)) {
			
				playbackLine = (SourceDataLine) AudioSystem.getLine(outInfo);
				playbackLine.open(outFormat);
				
				gainControl = (FloatControl) playbackLine.getControl(FloatControl.Type.MASTER_GAIN);
				
				playbackLine.start();
				runPlaybackLoop(transcodedIn);
				playbackLine.drain();
				playbackLine.stop();
				playbackLine.close();
				
			}
		}
		catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
			e.printStackTrace();
		}
	}
	
	private AudioFormat transcodePCM(AudioFormat original) {
		int channels = original.getChannels();
		float rate = original.getSampleRate();
		return new AudioFormat(
				rate, 16, channels, true, false);
	}
	
	private void runPlaybackLoop(AudioInputStream in) throws IOException {
		
		int numBytes = 0;
		int frameSize = in.getFormat().getFrameSize();
		int bufferSize = (64 - (64 % frameSize));
		byte[] buffer = new byte[bufferSize * 1024];
		
		playing = true;
		for (int bytesRead = 0;
				playing && bytesRead > -1;
				bytesRead = in.read(buffer, numBytes, buffer.length - numBytes)) {
			
			numBytes += bytesRead;
			
			int extra = numBytes % frameSize;
			int toWrite = numBytes - extra;
			
			playbackLine.write(buffer, 0, toWrite);
			
			if (extra > 0)
				System.arraycopy(buffer, toWrite, buffer, 0, extra);
			
			numBytes = extra;
		}
	}
	
	private static float toGain(int percentVolume) {
		double percentVol = percentVolume / 100.0;
		return (float) (Math.log(percentVol) / Math.log(10.0) * 20.0);
	}
	
}
