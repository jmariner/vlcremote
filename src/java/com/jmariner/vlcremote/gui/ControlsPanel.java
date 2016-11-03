package com.jmariner.vlcremote.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;

import org.apache.commons.lang3.StringUtils;

import com.jmariner.vlcremote.MyVLCRemote;
import com.jmariner.vlcremote.MyVLCRemote.Command;
import com.jmariner.vlcremote.util.GuiUtils;
import com.jmariner.vlcremote.util.SimpleIcon;
import com.jmariner.vlcremote.util.UserSettings;
import com.jmariner.vlcremote.util.VLCStatus;
import com.jmariner.vlcremote.util.VLCStatus.State;

public class ControlsPanel extends JPanel {
	
	private RemoteInterface gui;
	
	private JButton nextButton, playPauseButton, prevButton;
	private JToggleButton repeatToggleButton, loopToggleButton, shuffleToggleButton;
	private JToggleButton togglePlaylistButton;

	private JButton toggleMuteButton;
	private JSlider volumeSlider;
	private JTextField volumeTextField;
	
	private List<AbstractButton> vlcControlButtons = new ArrayList<>();
	
	protected ControlsPanel(RemoteInterface gui) {
		super(new BorderLayout(0, 0));
		
		this.gui = gui;
		
		init();
		
		gui.getControlComponents().addAll(
				Arrays.asList(togglePlaylistButton, toggleMuteButton, volumeSlider, volumeTextField)
		);
		gui.getControlComponents().addAll(vlcControlButtons);
		
		initListeners();
		
		Arrays.asList(ControlsPanel.class.getDeclaredFields()).forEach(f -> {
			try {
				if (f.getType() == JButton.class || f.getType() == JToggleButton.class) {
				//	f.setAccessible(true);
					AbstractButton b = (AbstractButton) f.get(this);
					gui.getButtons().put(f.getName().replace("Button", ""), b);
				}
			}
			catch (ReflectiveOperationException e) {
				e.printStackTrace();
			}
		});
	}
	
	private void init() {
		
		Dimension buttonSize = GuiUtils.squareDim((int) (SimpleIcon.ICON_SIZE * 1.25));
		
		playPauseButton = new JButton(SimpleIcon.PLAY.get());
		playPauseButton.setActionCommand("PLAY");

		nextButton = new JButton(SimpleIcon.NEXT.get());
		nextButton.setActionCommand("NEXT");

		prevButton = new JButton(SimpleIcon.PREV.get());
		prevButton.setActionCommand("PREV");

		repeatToggleButton = new JToggleButton(SimpleIcon.REPEAT.get());
		repeatToggleButton.setActionCommand("TOGGLE_REPEAT");

		loopToggleButton = new JToggleButton(SimpleIcon.LOOP.get());
		loopToggleButton.setActionCommand("TOGGLE_LOOP");

		shuffleToggleButton = new JToggleButton(SimpleIcon.SHUFFLE.get());
		shuffleToggleButton.setActionCommand("TOGGLE_RANDOM");
		
		vlcControlButtons = Arrays.asList(prevButton, playPauseButton, nextButton,
				repeatToggleButton, loopToggleButton, shuffleToggleButton);
		
		togglePlaylistButton = new JToggleButton(SimpleIcon.PLAYLIST.get());
		togglePlaylistButton.setToolTipText("Show playlist");
		togglePlaylistButton.setPreferredSize(buttonSize);
		
		JPanel leftHalf = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		vlcControlButtons.stream().forEachOrdered(b -> {
			b.setPreferredSize(buttonSize);
			b.setToolTipText(Command.valueOf(b.getActionCommand()).getDescription());
			leftHalf.add(b);
		});
		leftHalf.add(togglePlaylistButton);
		
		toggleMuteButton = new JButton(SimpleIcon.VOLUME_HIGH.get());
		toggleMuteButton.setPreferredSize(buttonSize);
		
		volumeSlider = new JSlider(0, 200, 100);
		volumeSlider.setToolTipText("Click or drag to set volume; double click to reset");
		
		volumeTextField = new JTextField("100%", 5);
		volumeTextField.setToolTipText("Enter volume from 0 to 200 percent");
		
		JPanel rightHalf = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		Stream.of(toggleMuteButton, volumeSlider, volumeTextField).forEachOrdered(rightHalf::add);
		
		this.add(leftHalf, BorderLayout.WEST);
		this.add(rightHalf, BorderLayout.EAST);
	}
	
	private void initListeners() {
		
		playPauseButton.addActionListener(this::playPausePressed);
		vlcControlButtons.forEach(b -> b.addActionListener(this::controlButtonPressed));
		
		VolumeSliderMouseListener listener = new VolumeSliderMouseListener();
		volumeSlider.addMouseListener(listener);
		volumeSlider.addMouseWheelListener(listener);
		volumeSlider.addChangeListener(this::volumeChanged);
		
		toggleMuteButton.addActionListener(this::toggleMute);
		volumeTextField.addActionListener(this::volumeInputted);
		
		togglePlaylistButton.addActionListener(gui::togglePlaylistArea);
	}
	
	protected void togglePlaying() {
		playPauseButton.doClick();
	}
	
	protected void next() {
		nextButton.doClick();
	}
	
	protected void previous() {
		prevButton.doClick();
	}
	
	protected void updateVolume() {
		int volume = volumeSlider.getValue();
		if (!volumeTextField.hasFocus())
			volumeTextField.setText(volume + "%");
		SimpleIcon volumeIcon =
				gui.isMuted() ? SimpleIcon.VOLUME_OFF :
				volume == 0 ? SimpleIcon.VOLUME_NONE :
				volume < 100 ? SimpleIcon.VOLUME_LOW :
				SimpleIcon.VOLUME_HIGH;

		toggleMuteButton.setIcon(volumeIcon.get());
		toggleMuteButton.setToolTipText("Click to " + (gui.isMuted() ? "unmute" : "mute"));
	}
	
	protected void update(VLCStatus status) {
		
		togglePlaylistButton.setSelected(gui.isPlaylistAreaShowing());
		togglePlaylistButton.setToolTipText(gui.isPlaylistAreaShowing() ? "Hide playlist" : "Show playlist");
		
		if (status == null) return;
		
		// state will be either playing or paused at this point
		String newState = status.getState() == State.PAUSED ? "PLAY" : "PAUSE";
		
		if (!playPauseButton.getActionCommand().equals(newState)) {
			playPauseButton.setActionCommand(newState);
			playPauseButton.setIcon(SimpleIcon.valueOf(newState).get());
			playPauseButton.setToolTipText(Command.valueOf(newState).getDescription());
		}

		boolean loop = status.isLoop();
		boolean repeat = status.isRepeat();
		boolean shuffle = status.isShuffle();

		if (loopToggleButton.isSelected() != loop)
			loopToggleButton.setSelected(loop);
		if (repeatToggleButton.isSelected() != repeat)
			repeatToggleButton.setSelected(repeat);
		if (shuffleToggleButton.isSelected() != shuffle)
			shuffleToggleButton.setSelected(shuffle);
	}
	
	private void playPausePressed(ActionEvent e) {
		// actual playing or pausing is handled by controlButtonPressed
		// before the pause or play request goes through, stop or start the stream locally first

		if (!UserSettings.getBoolean("instantPause", false)) return;

		MyVLCRemote remote = gui.getRemote();
		String cmd = e.getActionCommand();
		if (cmd.equals("PLAY") && !remote.isPlayingStream())
			remote.playStream();
		else if (cmd.equals("PAUSE") && remote.isPlayingStream())
			remote.stopStream();
	}

	private void controlButtonPressed(ActionEvent e) {
		assert gui.isConnected();

		String cmd = e.getActionCommand();
		assert Command.keys().contains(cmd);

		VLCStatus status = gui.getRemote().sendCommand(Command.valueOf(cmd));
		gui.updateInterface(status);
	}

	private void volumeChanged(ChangeEvent e) {
		double percentVolume = volumeSlider.getValue() / 100.0;
		if (gui.isMuted()) gui.setMuted(false);
		gui.getRemote().setPlaybackVolume(percentVolume);
	}

	private void volumeInputted(ActionEvent e) {
		String input = volumeTextField.getText();
		if (!StringUtils.isNumeric(input)) {
			Matcher match = Pattern.compile("^(\\d+)%$").matcher(input);
			if (!match.find()) {
				gui.handleException(new IllegalArgumentException("Input value must be an integer or percentage. Entered: " + input));
				return;
			}
			input = match.group(1);
		}

		assert StringUtils.isNumeric(input);
		int volume = Integer.parseInt(input);
		if (volume > 200) volume = 200;
		if (volume < 0) volume = 0;

		gui.clearFocus();
		volumeSlider.setValue(volume);
		gui.updateInterface();
	}

	private void resetVolume() {
		volumeSlider.setValue(100);
		gui.updateInterface();
	}

	private void toggleMute(EventObject e) {
		gui.setMuted(!gui.isMuted());
		if (gui.isMuted())
			gui.getRemote().setPlaybackVolume(0);
		else
			volumeChanged(null); // updates volume from slider

		gui.updateInterface();
	}
	
	private class VolumeSliderMouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				resetVolume();
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			double percent = e.getPoint().x / ((double) volumeSlider.getWidth());
			int newVal = (int) (volumeSlider.getMinimum() + ((volumeSlider.getMaximum() - volumeSlider.getMinimum()) * percent));
			volumeSlider.setValue(newVal);
			gui.updateInterface();
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {

				int changeBy = 2;
				int change = e.getWheelRotation() < 0 ? changeBy * -1 : changeBy;
				int newValue = volumeSlider.getValue() - change;
				if (newValue > volumeSlider.getMaximum())
					newValue = volumeSlider.getMaximum();
				if (newValue < volumeSlider.getMinimum())
					newValue = volumeSlider.getMinimum();

				volumeSlider.setValue(newValue);
				gui.updateInterface();
			}
		}
	}

}
