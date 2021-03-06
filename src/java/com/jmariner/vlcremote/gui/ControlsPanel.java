package com.jmariner.vlcremote.gui;

import com.jmariner.vlcremote.MyVLCRemote;
import com.jmariner.vlcremote.MyVLCRemote.Command;
import com.jmariner.vlcremote.util.GuiUtils;
import com.jmariner.vlcremote.util.RegexFilter;
import com.jmariner.vlcremote.util.SVGIcon;
import com.jmariner.vlcremote.util.SimpleIcon;
import com.jmariner.vlcremote.util.UserSettings;
import com.jmariner.vlcremote.util.VLCStatus;
import com.jmariner.vlcremote.util.VLCStatus.State;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.text.PlainDocument;

import java.awt.*;
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

public class ControlsPanel extends JPanel {
	
	private RemoteInterface gui;
	
	private JButton nextButton, playPauseButton, prevButton;
	private JToggleButton toggleRepeatButton, toggleLoopButton, toggleShuffleButton;
	private JToggleButton togglePlaylistButton;

	private JToggleButton toggleMuteButton;
	private JSlider volumeSlider;
	private JTextField volumeTextField;
	
	private SVGIcon volumeNone, volumeLow, volumeHigh;
	
	private List<AbstractButton> vlcControlButtons = new ArrayList<>();
	
	protected ControlsPanel(RemoteInterface gui) {
		super(new BorderLayout(0, 0));
		
		this.gui = gui;
		
		this.volumeNone = SimpleIcon.VOLUME_NONE.get();
		this.volumeLow = SimpleIcon.VOLUME_LOW.get();
		this.volumeHigh = SimpleIcon.VOLUME_HIGH.get();
		
		init();
		
		Arrays.asList(togglePlaylistButton, toggleMuteButton, volumeSlider, volumeTextField).forEach(gui::addControlComponent);
		vlcControlButtons.forEach(gui::addControlComponent);
		
		initListeners();
		
		Arrays.asList(ControlsPanel.class.getDeclaredFields()).forEach(f -> {
			try {
				if (f.getType() == JButton.class || f.getType() == JToggleButton.class) {
					AbstractButton b = (AbstractButton) f.get(this);
					gui.getActions().put(f.getName().replace("Button", ""), b::doClick);
				}
			}
			catch (ReflectiveOperationException e) {
				e.printStackTrace();
			}
		});
	}
	
	private void init() {
		
		Dimension buttonSize = GuiUtils.squareDim(SimpleIcon.Defaults.BUTTON_SIZE);
		
		playPauseButton = new JButton(SimpleIcon.PLAY.get());
		playPauseButton.setActionCommand("PLAY");

		nextButton = new JButton(SimpleIcon.NEXT.get());
		nextButton.setActionCommand("NEXT");

		prevButton = new JButton(SimpleIcon.PREV.get());
		prevButton.setActionCommand("PREV");
		
		Color selected = SimpleIcon.Defaults.SELECTED_COLOR;

		toggleRepeatButton = new JToggleButton(SimpleIcon.REPEAT.get());
		toggleRepeatButton.setSelectedIcon(SimpleIcon.REPEAT.get(selected));
		toggleRepeatButton.setActionCommand("TOGGLE_REPEAT");

		toggleLoopButton = new JToggleButton(SimpleIcon.LOOP.get());
		toggleLoopButton.setSelectedIcon(SimpleIcon.LOOP.get(selected));
		toggleLoopButton.setActionCommand("TOGGLE_LOOP");

		toggleShuffleButton = new JToggleButton(SimpleIcon.SHUFFLE.get());
		toggleShuffleButton.setSelectedIcon(SimpleIcon.SHUFFLE.get(selected));
		toggleShuffleButton.setActionCommand("TOGGLE_RANDOM");
		
		vlcControlButtons = Arrays.asList(prevButton, playPauseButton, nextButton,
				toggleRepeatButton, toggleLoopButton, toggleShuffleButton);
		
		togglePlaylistButton = new JToggleButton(SimpleIcon.PLAYLIST.get());
		togglePlaylistButton.setSelectedIcon(SimpleIcon.PLAYLIST.get(selected));
		togglePlaylistButton.setToolTipText("Show playlist");
		togglePlaylistButton.setPreferredSize(buttonSize);
		
		JPanel leftHalf = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		vlcControlButtons.stream().forEachOrdered(b -> {
			b.setPreferredSize(buttonSize);
			b.setToolTipText(Command.valueOf(b.getActionCommand()).getDescription());
			leftHalf.add(b);
		});
		leftHalf.add(togglePlaylistButton);
		
		toggleMuteButton = new JToggleButton(SimpleIcon.VOLUME_HIGH.get());
		toggleMuteButton.setSelectedIcon(SimpleIcon.VOLUME_OFF.get(selected));
		toggleMuteButton.setPreferredSize(buttonSize);
		
		volumeSlider = new JSlider(0, 200, 100);
		volumeSlider.setToolTipText("Click or drag to set volume; double click to reset");
		
		volumeTextField = new JTextField("100%", 5);
		volumeTextField.setToolTipText("Enter volume from 0 to 200 percent");
		((PlainDocument) volumeTextField.getDocument()).setDocumentFilter(
				new RegexFilter("[%\\d]")
		);
		
		JPanel rightHalf = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		Stream.of(toggleMuteButton, volumeSlider, volumeTextField).forEachOrdered(rightHalf::add);
		
		this.add(leftHalf, BorderLayout.WEST);
		this.add(rightHalf, BorderLayout.EAST);
	}
	
	private void initListeners() {
		
		playPauseButton.addActionListener(this::playPausePressed);
		vlcControlButtons.forEach(b -> b.addActionListener(this::controlButtonPressed));
		Arrays.asList(nextButton, prevButton).forEach(b -> b.addActionListener(e -> {
			if (UserSettings.getBoolean("restartOnTrackChange", false))
				gui.getRemote().getPlayer().restart(1000);
		}));
		
		VolumeSliderMouseListener listener = new VolumeSliderMouseListener();
		volumeSlider.addMouseListener(listener);
		volumeSlider.addMouseWheelListener(listener);
		volumeSlider.addChangeListener(this::volumeChanged);
		
		toggleMuteButton.addActionListener(this::toggleMute);
		volumeTextField.addActionListener(this::volumeInputted);
		
		togglePlaylistButton.addActionListener(gui::togglePlaylistArea);
	}
	
	protected void updateVolume() {
		
		volumeSlider.setValue(gui.getRemote().getPlayer().getVolume());
		
		int volume = volumeSlider.getValue();
		if (!volumeTextField.hasFocus())
			volumeTextField.setText(volume + "%");
			
		toggleMuteButton.setIcon(
				volume == 0 ? volumeNone :
				volume < 100 ? volumeLow :
				volumeHigh);
		
		boolean muted = gui.getRemote().getPlayer().isMuted();
		toggleMuteButton.setToolTipText("Click to " + (muted ? "unmute" : "mute"));
		toggleMuteButton.setSelected(muted);
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

		if (toggleLoopButton.isSelected() != loop)
			toggleLoopButton.setSelected(loop);
		if (toggleRepeatButton.isSelected() != repeat)
			toggleRepeatButton.setSelected(repeat);
		if (toggleShuffleButton.isSelected() != shuffle)
			toggleShuffleButton.setSelected(shuffle);
	}
	
	private void playPausePressed(ActionEvent e) {
		// actual playing or pausing is handled by controlButtonPressed
		// before the pause or play request goes through, stop or start the stream locally first

		if (!UserSettings.getBoolean("instantPause", false)) return;

		MyVLCRemote remote = gui.getRemote();
		String cmd = e.getActionCommand();
		if (cmd.equals("PLAY") && !remote.getPlayer().isPlaying())
			remote.getPlayer().start();
		else if (cmd.equals("PAUSE") && remote.getPlayer().isPlaying())
			remote.getPlayer().stop();
	}

	private void controlButtonPressed(ActionEvent e) {
		assert gui.isConnected();

		Command cmd = Command.forName(e.getActionCommand());
		assert cmd != null;

		VLCStatus status = gui.getRemote().sendCommand(cmd);
		gui.updateInterface(status);
	}

	private void volumeChanged(ChangeEvent e) {
		gui.getRemote().getPlayer().setMuted(false);
		gui.getRemote().getPlayer().setVolume(volumeSlider.getValue());
		updateVolume();
	}

	private void volumeInputted(ActionEvent e) {
		String input = volumeTextField.getText();
		if (!StringUtils.isNumeric(input)) {
			Matcher match = Pattern.compile("^(\\d+)%$").matcher(input);
			if (!match.find()) {
				gui.handleException(new IllegalArgumentException(
						"Input value must be an integer or percentage. Entered: " + input));
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

	private void toggleMute(EventObject e) {
		gui.getRemote().getPlayer().toggleMute();
		updateVolume();
	}
	
	private class VolumeSliderMouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				volumeSlider.setValue(100);
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			double percent = e.getPoint().x / ((double) volumeSlider.getWidth());
			int newVal = (int) (volumeSlider.getMinimum() + ((volumeSlider.getMaximum() - volumeSlider.getMinimum()) * percent));
			volumeSlider.setValue(newVal);
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
			}
		}
	}

}
