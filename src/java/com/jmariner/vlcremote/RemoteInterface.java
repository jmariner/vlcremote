package com.jmariner.vlcremote;

import com.jmariner.vlcremote.MyVLCRemote.Command;
import com.jmariner.vlcremote.components.LoginPanel;
import com.jmariner.vlcremote.components.MainMenuBar;
import com.jmariner.vlcremote.components.PlaylistPanel;
import com.jmariner.vlcremote.components.StatusPanel;
import com.jmariner.vlcremote.util.*;
import com.jtattoo.plaf.noire.NoireLookAndFeel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.intellij.lang.annotations.MagicConstant;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jmariner.vlcremote.util.Constants.*;
import static com.jmariner.vlcremote.util.GlobalHotkeyListener.Mod.NONE;
import static com.tulskiy.keymaster.common.MediaKey.*;
import static java.awt.event.KeyEvent.*;
import static javax.swing.JOptionPane.*;

@Slf4j
@SuppressWarnings("FieldCanBeLocal")
public class RemoteInterface extends JFrame {

	@Getter(AccessLevel.PUBLIC)
	private MyVLCRemote remote;

	private MainMenuBar menuBar;

	private JPanel mainPanel;
	private JPanel middlePanel, bottomPanel;
	private LoginPanel loginPanel;
	private StatusPanel statusPanel;
	private JSeparator mainSeparator;

	private PlaylistPanel playlistPanel;

	private JLabel positionLabel, lengthLabel;
	private JProgressBar progressBar;

	private JButton nextButton, playPauseButton, prevButton;
	private JToggleButton repeatToggleButton, loopToggleButton, shuffleToggleButton;
	private JToggleButton togglePlaylistButton;

	private JButton volumeButton;
	private JSlider volumeSlider;
	private JTextField volumeTextField;

	private List<AbstractButton> controlButtons = new ArrayList<>();
	private List<JComponent> controls = new ArrayList<>();

	private ScheduledFuture<?> updateLoop;

	@Getter(AccessLevel.PUBLIC) @Setter(AccessLevel.PUBLIC)
	private boolean connected, muted, playlistAreaShowing;

	// this is to create a NullPointerException if i try using the superclass's HEIGHT value of 1
	@SuppressWarnings("unused")
	private static final Object HEIGHT = null;

	public RemoteInterface() {

		Stream.of("Label.font", "TextField.font", "Button.font").forEach(
				s -> UIManager.put(s, new FontUIResource(FONT))
		);

		try {
			Properties p = new Properties();
			p.put("logoString", "");
			NoireLookAndFeel.setCurrentTheme(p);
			UIManager.setLookAndFeel(new NoireLookAndFeel());
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		menuBar = new MainMenuBar(this);
		loginPanel = new LoginPanel(this);
		statusPanel = new StatusPanel();
		initMiddle();
		initBottom();

		initActionListeners();

		controls.forEach(c -> c.setEnabled(false));

		mainPanel = new JPanel(new BorderLayout(0, 20));
		mainPanel.setBorder(new EmptyBorder(MAIN_PADDING, MAIN_PADDING, MAIN_PADDING, MAIN_PADDING));
		mainPanel.setSize(MAIN_WIDTH, MAIN_HEIGHT);
		mainPanel.setFocusable(true);
		mainPanel.add(loginPanel, BorderLayout.NORTH);
		mainPanel.add(middlePanel, BorderLayout.CENTER);
		mainPanel.add(bottomPanel, BorderLayout.SOUTH);

		mainSeparator = new JSeparator();

		this.setLayout(new BorderLayout());
		this.add(mainPanel, BorderLayout.NORTH);
		this.setJMenuBar(menuBar);
		this.setTitle("VLC Remote");
		this.setSize(MAIN_WIDTH, MAIN_HEIGHT + MENUBAR_HEIGHT);
		this.setResizable(false);
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		Runtime.getRuntime().addShutdownHook(new CleanupOnShutdown());

		loadSettings();
		if (UserSettings.getBoolean("autoconnect", false)) {
			if (UserSettings.keyExists("httpPass"))
				loginPanel.connectPressed(null);
			else
				UserSettings.getRoot().remove("autoconnect");
		}
	}

	private void togglePlaylistArea(AWTEvent e) {
		playlistAreaShowing = !playlistAreaShowing;

		if (playlistAreaShowing) {
			this.setSize(MAIN_WIDTH, MAIN_HEIGHT + MENUBAR_HEIGHT + PLAYLIST_HEIGHT);
			this.add(mainSeparator, BorderLayout.CENTER);
			this.add(playlistPanel, BorderLayout.SOUTH);
		}
		else {
			this.setSize(MAIN_WIDTH, MAIN_HEIGHT + MENUBAR_HEIGHT);
			this.remove(mainSeparator);
			this.remove(playlistPanel);
		}

		togglePlaylistButton.setSelected(playlistAreaShowing);
		togglePlaylistButton.setToolTipText(playlistAreaShowing ? "Hide playlist" : "Show playlist");
	}

	private void loadSettings() {
		menuBar.loadSettings();
		loginPanel.loadSettings();
	}

	private void initMiddle() {
		positionLabel = new JLabel("00:00:00");
		progressBar = new JProgressBar(0, (int) Math.pow(10, 6));
		lengthLabel = new JLabel("00:00:00");

		Dimension d = new Dimension(66, 25);
		positionLabel.setPreferredSize(d);
		positionLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lengthLabel.setPreferredSize(d);
		lengthLabel.setHorizontalAlignment(SwingConstants.CENTER);

		middlePanel = new JPanel(new BorderLayout(MAIN_PADDING, 0), true);
		middlePanel.add(positionLabel, BorderLayout.WEST);
		middlePanel.add(progressBar, BorderLayout.CENTER);
		middlePanel.add(lengthLabel, BorderLayout.EAST);

		controls.add(progressBar);
	}

	private void initBottom() {

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

		controlButtons = Arrays.asList(prevButton, playPauseButton, nextButton, repeatToggleButton, loopToggleButton, shuffleToggleButton);

		togglePlaylistButton = new JToggleButton(SimpleIcon.PLAYLIST.get());
		togglePlaylistButton.setToolTipText("Show playlist");
		togglePlaylistButton.setPreferredSize(buttonSize);

		JPanel bottomLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		controlButtons.stream().forEachOrdered(b -> {
			b.setPreferredSize(buttonSize);
			b.setToolTipText(Command.valueOf(b.getActionCommand()).getDescription());
			bottomLeft.add(b);
		});
		bottomLeft.add(togglePlaylistButton);

		volumeButton = new JButton(SimpleIcon.VOLUME_HIGH.get());
		volumeButton.setPreferredSize(buttonSize);
		volumeSlider = new JSlider(0, 200, 100);
		volumeSlider.setToolTipText("Click or drag to set volume; double click to reset");
		volumeTextField = new JTextField("100%", 5);
		volumeTextField.setToolTipText("Enter volume from 0 to 200 percent");

		controls.add(togglePlaylistButton);
		controls.addAll(controlButtons);
		controls.addAll(Arrays.asList(volumeButton, volumeSlider, volumeTextField));

		JPanel bottomRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		bottomRight.add(volumeButton);
		bottomRight.add(volumeSlider);
		bottomRight.add(volumeTextField);

		bottomPanel = new JPanel(new BorderLayout(0, 0));
		bottomPanel.add(bottomLeft, BorderLayout.WEST);
		bottomPanel.add(bottomRight, BorderLayout.EAST);
	}

	private void initActionListeners() {

		progressBar.addMouseListener(new ProgressBarMouseListener());

		playPauseButton.addActionListener(this::playPausePressed);
		controlButtons.forEach(b -> b.addActionListener(this::controlButtonPressed));
		controls.forEach(c -> c.addKeyListener(new ControlsKeyListener()));

		VolumeSliderMouseListener listener = new VolumeSliderMouseListener();
		volumeSlider.addMouseListener(listener);
		volumeSlider.addMouseWheelListener(listener);
		volumeSlider.addChangeListener(this::volumeChanged);
		volumeButton.addActionListener(this::toggleMute);
		volumeTextField.addActionListener(this::volumeInputted);

		togglePlaylistButton.addActionListener(this::togglePlaylistArea);
	}

	/* 	TODO create a menu to edit hotkeys. ask user to input a hotkey and use KeyStroke.getKeyStroke(SomeEvent e)
		to get it. ignore control/alt/shift/meta keys as normal keys, only allow their use as mask keys

		keypad keys are temporary and will be removed to allow user to customize them
		gotta figure out saving hotkey info?
	*/
	private void initHotkeys() {
		GlobalHotkeyListener g = new GlobalHotkeyListener();
		g.registerHotkey(MEDIA_PLAY_PAUSE, playPauseButton::doClick);
		g.registerHotkey(MEDIA_NEXT_TRACK, nextButton::doClick);
		g.registerHotkey(MEDIA_PREV_TRACK, prevButton::doClick);

		g.registerHotkey(VK_ADD,		NONE, playPauseButton::doClick);
		g.registerHotkey(VK_MULTIPLY, 	NONE, nextButton::doClick);
		g.registerHotkey(VK_DIVIDE, 	NONE, prevButton::doClick);
	}

	public void connect() {

		initRemote();
		connected = remote.testConnection();

		if (connected) {
			mainPanel.remove(loginPanel);

			mainPanel.add(statusPanel, BorderLayout.NORTH);

			controls.forEach(b -> b.setEnabled(true));

			playlistPanel = new PlaylistPanel(this);
			initHotkeys();

			remote.setSourceVolume(1);
			remote.sendCommand(Command.PLAY);
			updateInterface(remote.getStatus());
			startUpdateLoop();
			remote.playStream();
		}
	}

	private void initRemote() {
		remote = new MyVLCRemote(
				loginPanel.getHost(),
				loginPanel.getHttpPort(),
				loginPanel.getPassword(),
				loginPanel.getStreamPort(),
				this::handleException
		);
	}

	private void updateInterface() {
		updateInterface(null);
	}

	public void updateInterface(VLCStatus status) {

		int volume = volumeSlider.getValue();
		if (!volumeTextField.hasFocus())
			volumeTextField.setText(volume + "%");
		SimpleIcon volumeIcon =
				muted ? SimpleIcon.VOLUME_OFF :
				volume == 0 ? SimpleIcon.VOLUME_NONE:
				volume < 100 ? SimpleIcon.VOLUME_LOW :
				SimpleIcon.VOLUME_HIGH;

		volumeButton.setIcon(volumeIcon.get());
		volumeButton.setToolTipText("Click to " + (muted ? "unmute" : "mute"));

		if (status == null) return; // everything past here requires status

		String newState = null;
		switch (status.getState()) {
			case PLAYING:
				newState = "PAUSE";
				break;
			case PAUSED:
				newState = "PLAY";
				break;
			case STOPPED:
			case UNKNOWN:
				newState = null;
				break;
		}

		// newState is null if VLC is neither playing nor paused
		if (newState == null) return;

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

		String filename = status.getFilename();
		String artist = status.getArtist();
		String title = status.getTitle();
		String text =
				title == null ? filename :
				artist == null ? title :
				artist + " - " + title;

		if (!statusPanel.getTitle().equals(text)) { // if we're on a new song than before
			statusPanel.setTitle(text);

			int length = status.getLength();
			lengthLabel.setText(GuiUtils.formatTime(length));

			playlistPanel.update(status);
		}

		int currentTime = status.getTime();
		double positionPercent = status.getPosition();

		positionLabel.setText(GuiUtils.formatTime(currentTime));
		progressBar.setValue((int) (positionPercent * progressBar.getMaximum()));
		
	}

	private void startUpdateLoop() {
		updateLoop = Executors.newScheduledThreadPool(1).scheduleAtFixedRate(
				() -> updateInterface(remote.getStatus()),
				0, UserSettings.getInt("updateDelay", 1000), TimeUnit.MILLISECONDS
		);
	}

	public void restartUpdateLoop() {
		if (!updateLoop.isCancelled())
			updateLoop.cancel(true);
		startUpdateLoop();
	}

	private void playPausePressed(ActionEvent e) {
		// actual playing or pausing is handled by controlButtonPressed
		// before the pause or play request goes through, stop or start the stream locally first

		if (!UserSettings.getBoolean("instantPause", false)) return;

		String cmd = e.getActionCommand();
		if (cmd.equals("PLAY") && !remote.isPlayingStream())
			remote.playStream();
		else if (cmd.equals("PAUSE") && remote.isPlayingStream())
			remote.stopStream();
	}

	private void controlButtonPressed(ActionEvent e) {
		assert connected;

		String cmd = e.getActionCommand();
		assert Command.keys().contains(cmd);

		VLCStatus status = remote.sendCommand(Command.valueOf(cmd));
		updateInterface(status);
	}

	private void volumeChanged(ChangeEvent e) {
		double percentVolume = volumeSlider.getValue() / 100.0;
		if (muted) muted = false;
		remote.setPlaybackVolume(percentVolume);
	}

	private void volumeInputted(ActionEvent e) {
		String input = volumeTextField.getText();
		if (!StringUtils.isNumeric(input)) {
			Matcher match = Pattern.compile("^(\\d+)%$").matcher(input);
			if (!match.find()) {
				handleException(new IllegalArgumentException("Input value must be an integer or percentage. Entered: " + input));
				return;
			}
			input = match.group(1);
		}

		assert StringUtils.isNumeric(input);
		int volume = Integer.parseInt(input);
		if (volume > 200) volume = 200;
		if (volume < 0) volume = 0;

		clearFocus();
		volumeSlider.setValue(volume);
		updateInterface();
	}

	private void resetVolume() {
		volumeSlider.setValue(100);
		updateInterface();
	}

	private void toggleMute(EventObject e) {
		muted = !muted;
		if (muted)
			remote.setPlaybackVolume(0);
		else
			volumeChanged(null); // updates volume from slider

		updateInterface();
	}

	private void clearFocus() {
		mainPanel.requestFocus();
	}

	public void handleException(Throwable e) {
		String text = "An error has occurred.<br><br>" + StringEscapeUtils.escapeHtml4(e.getMessage()) + "<br><br>Show the error?";
		int showErrorInt = JOptionPane.showConfirmDialog(this, GuiUtils.restrictDialogWidth(text, false), "Error", YES_NO_OPTION, ERROR_MESSAGE);
		if (showErrorInt == 0) {
			String stackTrace = String.join("\n",
					Stream.of(e.getStackTrace())
							.map(StackTraceElement::toString)
							.collect(Collectors.toList()));

			JScrollPane pane = new JScrollPane(new JTextArea(stackTrace));
			pane.setPreferredSize(new Dimension(MAIN_WIDTH-100, MAX_HEIGHT-100));

			pane.addHierarchyListener(a -> {
				Window w = SwingUtilities.getWindowAncestor(pane);
				if (w instanceof Dialog) {
					Dialog d = (Dialog) w;
					d.setResizable(true);
				}
			});

			JOptionPane.showMessageDialog(this, pane, "Error Stack Trace", ERROR_MESSAGE);
		}
	}

	@SuppressWarnings("unused")
	private void alert(String text) {
		alert("Message", text, PLAIN_MESSAGE);
	}

	private void alert(String title, String text, @MagicConstant(intValues={INFORMATION_MESSAGE,WARNING_MESSAGE, ERROR_MESSAGE,QUESTION_MESSAGE,PLAIN_MESSAGE}) int messageType) {
		JOptionPane.showMessageDialog(this, GuiUtils.restrictDialogWidth(text), title, messageType);
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
			updateInterface();
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
				updateInterface();
			}
		}
	}

	private class ProgressBarMouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (progressBar.isEnabled()) {
				double percent = e.getPoint().x / ((double) progressBar.getWidth());
				int newVal = (int) (progressBar.getMinimum() + ((progressBar.getMaximum() - progressBar.getMinimum()) * percent));
				progressBar.setValue(newVal);
				VLCStatus status = remote.sendCommand(Command.SEEK_TO, (percent * 100) + "%");
				updateInterface(status);
			}
		}
	}

	private class ControlsKeyListener implements KeyListener {

		public void keyTyped(KeyEvent e) {}
		public void keyReleased(KeyEvent e) {}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == VK_ESCAPE)
				clearFocus();
		}
	}

	private class CleanupOnShutdown extends Thread {
		@Override
		public void run() {
			if (connected) {
				remote.stopStream();
				remote.sendCommand(Command.PAUSE);
			}
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			RemoteInterface r = new RemoteInterface();
			r.setVisible(true);
		});
	}

}
