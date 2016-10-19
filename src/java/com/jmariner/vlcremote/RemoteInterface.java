package com.jmariner.vlcremote;

import com.jmariner.vlcremote.MyVLCRemote.Command;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.intellij.lang.annotations.MagicConstant;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator.Attribute;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jmariner.vlcremote.GlobalHotkeyListener.Mod;
import static com.tulskiy.keymaster.common.MediaKey.*;
import static java.awt.event.KeyEvent.*;
import static javax.swing.JOptionPane.*;

@Slf4j
@SuppressWarnings("FieldCanBeLocal")
public class RemoteInterface extends JFrame {

	private MyVLCRemote remote;

	JMenuBar menuBar;

	JCheckBoxMenuItem debugBorders;
	JMenuItem updateDelayInput;

	private JPanel mainPanel, playlistPanel;
	private JPanel topPanel, middlePanel, bottomPanel;

	private JPanel topPre1, topPre2, topPost1, topPost2;

	private JLabel titleLabel;

	private JTextField hostField, webPortField, streamPortField;
	private JPasswordField passwordField;
	private JButton connectButton;

	private JLabel positionLabel, lengthLabel;
	private JProgressBar progressBar;

	private JButton nextButton, playPauseButton, prevButton;
	private JToggleButton repeatToggleButton, loopToggleButton, shuffleToggleButton;
	private JToggleButton togglePlaylistButton;

	private JButton volumeButton;
	private JSlider volumeSlider;
	private JTextField volumeTextField;

	JList<String> playlistList;
	JButton playSelected;
	JScrollPane playlistScrollPane;

	private List<JTextField> textFields = new ArrayList<>();
	private List<AbstractButton> controlButtons = new ArrayList<>();
	private List<JComponent> controls = new ArrayList<>();

	private ScheduledFuture updateLoop;
	private int updateDelay;
	private boolean connected, muted, playlistAreaShowing;

	private final Font FONT = new Font("Courier New", Font.PLAIN, 14);

	private final float MAX_TITLE_FONT_SIZE = 28f;
	private final float MIN_TITLE_FONT_SIZE = 16f;
	private final double MAX_TITLE_WIDTH = 600.0;
	private final int MAIN_PADDING = 10;
	private final int WIDTH = 650;
	private final int MAIN_HEIGHT = 225;
	private final int PLAYLIST_HEIGHT = 300;
	private final int MENUBAR_HEIGHT = 25;
	private final int MAX_HEIGHT = MAIN_HEIGHT + PLAYLIST_HEIGHT + MENUBAR_HEIGHT;

	@SuppressWarnings("unused")
	private final Object HEIGHT = null;

	private final HashMap<Attribute, Object> UNDERLINE = new HashMap<Attribute, Object>(){{
		put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
	}};

	public RemoteInterface() {

		Stream.of("Label.font", "TextField.font", "Button.font").forEach(
				s -> UIManager.put(s, new FontUIResource(FONT))
		);

		initMenu();
		initTop();
		initMiddle();
		initBottom();

		initActionListeners();

		controls.forEach(c -> c.setEnabled(false));

		mainPanel = new JPanel(new BorderLayout(0, 20));
		mainPanel.setBorder(new EmptyBorder(MAIN_PADDING, MAIN_PADDING, MAIN_PADDING, MAIN_PADDING));
		mainPanel.setSize(WIDTH, MAIN_HEIGHT);
		mainPanel.setFocusable(true);
		mainPanel.add(topPanel, BorderLayout.NORTH);
		mainPanel.add(middlePanel, BorderLayout.CENTER);
		mainPanel.add(bottomPanel, BorderLayout.SOUTH);

		updateDelay = 1000;

		this.setLayout(new BorderLayout());
		this.add(mainPanel, BorderLayout.NORTH);
		this.setJMenuBar(menuBar);
		this.setTitle("VLC Remote");
		this.setSize(WIDTH, MAIN_HEIGHT + MENUBAR_HEIGHT);
		this.setResizable(false);
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		Runtime.getRuntime().addShutdownHook(new CleanupOnShutdown());
	}

	private void initPlaylistArea() {
		List<Map<String, String>> playlistMaps = remote.getPlaylist();
		Vector<String> playlist = new Vector<>();
		playlistMaps.stream().forEachOrdered(i -> playlist.add(i.get("name")));

		playlistList = new JList<>(playlist);
		playlistList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		playlistScrollPane = new JScrollPane(
				playlistList,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
		);

		JLabel playlistTitle = new JLabel("Playlist");
		playlistTitle.setFont(FONT.deriveFont(18f).deriveFont(UNDERLINE));
		playlistTitle.setHorizontalAlignment(SwingConstants.CENTER);
		playSelected = new JButton("Play Selected");

		playlistPanel = new JPanel(new BorderLayout(0, 10));
		playlistPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		playlistPanel.setPreferredSize(new Dimension(WIDTH, PLAYLIST_HEIGHT));

		playlistPanel.add(playlistTitle, BorderLayout.NORTH);
		playlistPanel.add(playlistScrollPane, BorderLayout.CENTER);
		playlistPanel.add(playSelected, BorderLayout.SOUTH);

		playlistAreaShowing = false;
	}

	private void togglePlaylistArea(AWTEvent e) {
		playlistAreaShowing = !playlistAreaShowing;

		if (playlistAreaShowing) {
			this.setSize(WIDTH, MAIN_HEIGHT + MENUBAR_HEIGHT + PLAYLIST_HEIGHT);
			this.add(playlistPanel, BorderLayout.SOUTH);
		}
		else {
			this.setSize(WIDTH, MAIN_HEIGHT + MENUBAR_HEIGHT);
			this.remove(playlistPanel);
		}

		togglePlaylistButton.setSelected(playlistAreaShowing);
		togglePlaylistButton.setToolTipText(playlistAreaShowing ? "Hide playlist" : "Show playlist");
	}

	private void initMenu() {
		menuBar = new JMenuBar();

		JMenu tools = new JMenu("Tools");
		tools.setMnemonic(VK_T);

		debugBorders = new JCheckBoxMenuItem("Show debug borders");
		updateDelayInput = new JMenuItem("Set update delay");

		tools.add(debugBorders);
		tools.add(updateDelayInput);

		menuBar.add(tools);
		menuBar.setPreferredSize(new Dimension(WIDTH, MENUBAR_HEIGHT));

	}

	private void initTop() {

		hostField = new JTextField("home.jmariner.com", 20);
		webPortField = new JTextField("8080", 5);
		passwordField = new JPasswordField("", 20);
		streamPortField = new JTextField("8081", 5);
		connectButton = new JButton("Connect");

		textFields = Arrays.asList(hostField, webPortField, passwordField, streamPortField);

		titleLabel = new JLabel();
		titleLabel.addComponentListener(new TitleResizeListener());
		JLabel nowPlayingLabel = new JLabel("Now Playing");
		nowPlayingLabel.setFont(
				FONT.deriveFont(18f).deriveFont(UNDERLINE)
		);

		topPre1 = new JPanel(new FlowLayout(FlowLayout.CENTER, MAIN_PADDING, 0));
		topPre1.add(new JLabel("Host:"));
		topPre1.add(hostField);
		topPre1.add(new JLabel("Password:"));
		topPre1.add(passwordField);

		topPre2 = new JPanel(new FlowLayout(FlowLayout.CENTER, MAIN_PADDING, 0));
		topPre2.add(new JLabel("Web Port:"));
		topPre2.add(webPortField);
		topPre2.add(new JLabel("Stream Port:"));
		topPre2.add(streamPortField);
		topPre2.add(connectButton);

		topPost1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		topPost1.add(nowPlayingLabel);

		topPost2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		topPost2.add(titleLabel);

		topPanel = new JPanel(new BorderLayout(0, 10));
		topPanel.setBorder(new EmptyBorder(MAIN_PADDING, 0, MAIN_PADDING, 0));
		topPanel.setPreferredSize(new Dimension(WIDTH-20, 75));

		topPanel.add(topPre1, BorderLayout.NORTH);
		topPanel.add(topPre2, BorderLayout.SOUTH);
	}

	private void initMiddle() {
		positionLabel = new JLabel("00:00:00");
		progressBar = new JProgressBar(0, (int) Math.pow(10, 6));
		lengthLabel = new JLabel("00:00:00");

		progressBar.addMouseListener(new ProgressBarMouseListener());

		Dimension d = new Dimension(66, 25);
		positionLabel.setPreferredSize(d);
		positionLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lengthLabel.setPreferredSize(d);
		lengthLabel.setHorizontalAlignment(SwingConstants.CENTER);

		middlePanel = new JPanel(new BorderLayout(10, 0), true);
		middlePanel.add(positionLabel, BorderLayout.WEST);
		middlePanel.add(progressBar, BorderLayout.CENTER);
		middlePanel.add(lengthLabel, BorderLayout.EAST);

		controls.add(progressBar);
	}

	private void initBottom() {

		Dimension buttonSize = Icon.SIZE.getDim(1.25);

		playPauseButton = new JButton(Icon.PLAY.get());
		playPauseButton.setActionCommand("PLAY");

		nextButton = new JButton(Icon.NEXT.get());
		nextButton.setActionCommand("NEXT");
		nextButton.setToolTipText(Command.NEXT.getDescription());

		prevButton = new JButton(Icon.PREV.get());
		prevButton.setActionCommand("PREV");

		repeatToggleButton = new JToggleButton(Icon.REPEAT.get());
		repeatToggleButton.setActionCommand("TOGGLE_REPEAT");

		loopToggleButton = new JToggleButton(Icon.LOOP.get());
		loopToggleButton.setActionCommand("TOGGLE_LOOP");

		shuffleToggleButton = new JToggleButton(Icon.SHUFFLE.get());
		shuffleToggleButton.setActionCommand("TOGGLE_RANDOM");

		controlButtons = Arrays.asList(prevButton, playPauseButton, nextButton, repeatToggleButton, loopToggleButton, shuffleToggleButton);

		togglePlaylistButton = new JToggleButton(Icon.PLAYLIST.get());
		togglePlaylistButton.setToolTipText("Show playlist");
		togglePlaylistButton.setPreferredSize(buttonSize);

		JPanel bottomLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		controlButtons.stream().forEachOrdered(b -> {
			b.setPreferredSize(buttonSize);
			b.setToolTipText(Command.valueOf(b.getActionCommand()).getDescription());
			bottomLeft.add(b);
		});
		bottomLeft.add(togglePlaylistButton);

		volumeButton = new JButton(Icon.VOLUME_HIGH.get());
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

		debugBorders.addActionListener(e -> debugBorderComponents(this, debugBorders.isSelected()));
		updateDelayInput.addActionListener(this::setUpdateDelay);

		textFields.forEach(i -> i.addActionListener(this::connectPressed));
		connectButton.addActionListener(this::connectPressed);
		controlButtons.forEach(b -> b.addActionListener(this::controlButtonPressed));
		controls.forEach(c -> c.addKeyListener(new ControlsKeyListener()));

		VolumeSliderMouseListener listener = new VolumeSliderMouseListener();
		volumeSlider.addMouseListener(listener);
		volumeSlider.addMouseWheelListener(listener);
		volumeSlider.addChangeListener(this::volumeChanged);
		volumeButton.addActionListener(this::toggleMute);
		volumeTextField.addActionListener(this::volumeInputted);
	}

	private void initActionListenersPost() {
		togglePlaylistButton.addActionListener(this::togglePlaylistArea);
		playSelected.addActionListener(this::switchSongToSelected);
		playlistList.addMouseListener(new PlaylistMouseListener());
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

		g.registerHotkey(VK_ADD,		Mod.NONE, playPauseButton::doClick);
		g.registerHotkey(VK_MULTIPLY, 	Mod.NONE, nextButton::doClick);
		g.registerHotkey(VK_DIVIDE, 	Mod.NONE, prevButton::doClick);
	}

	private void connectPressed(AWTEvent e) {

		initRemote();
		connected = remote.testConnection();

		if (connected) {
			topPanel.remove(topPre1);
			topPanel.remove(topPre2);

			topPanel.add(topPost1, BorderLayout.NORTH);
			topPanel.add(topPost2, BorderLayout.SOUTH);

			controls.forEach(b -> b.setEnabled(true));

			initPlaylistArea();
			initActionListenersPost();
			initHotkeys();

			remote.setSourceVolume(1);
			remote.sendCommand(Command.PLAY);
			updateInterface(remote.getStatus());
			startUpdateLoop();
			remote.playStream();
		}
	}

	private void initRemote() {
		char[] password = passwordField.getPassword();
		remote = new MyVLCRemote(
				hostField.getText(),
				Integer.parseInt(webPortField.getText()),
				String.valueOf(password),
				Integer.parseInt(streamPortField.getText())
		);
		Arrays.fill(password, '0');

		remote.setExceptionHandler(this::handleException);
	}

	private void updateInterface() {
		updateInterface(null);
	}

	private void updateInterface(Map<String, String> status) {

		int volume = volumeSlider.getValue();
		if (!volumeTextField.hasFocus())
			volumeTextField.setText(volume + "%");
		Icon volumeIcon =
				muted ? Icon.VOLUME_OFF :
				volume == 0 ? Icon.VOLUME_NONE:
				volume < 100 ? Icon.VOLUME_LOW :
				Icon.VOLUME_HIGH;

		volumeButton.setIcon(volumeIcon.get());
		volumeButton.setToolTipText("Click to " + (muted ? "unmute" : "mute"));

		if (status == null) return; // everything past here requires status

		String state = status.get("state");
		String newState = state.equals("playing") ? "PAUSE" : state.equals("paused") ? "PLAY" : null;
		assert newState != null;

		if (!playPauseButton.getActionCommand().equals(newState)) {
			playPauseButton.setActionCommand(newState);
			playPauseButton.setIcon(Icon.valueOf(newState).get());
			playPauseButton.setToolTipText(Command.valueOf(newState).getDescription());
		}

		boolean loop = status.get("loop").equals("true");
		boolean repeat = status.get("repeat").equals("true");
		boolean shuffle = status.get("random").equals("true");

		if (loopToggleButton.isSelected() != loop)
			loopToggleButton.setSelected(loop);
		if (repeatToggleButton.isSelected() != repeat)
			repeatToggleButton.setSelected(repeat);
		if (shuffleToggleButton.isSelected() != shuffle)
			shuffleToggleButton.setSelected(shuffle);

		String filename = status.get("filename");
		String artist = status.get("artist");
		String title = status.get("title");
		String text =
				title == null ? filename :
				artist == null ? title :
				artist + " - " + title;

		if (!titleLabel.getText().equals(text)) { // if we're on a new song than before
			titleLabel.setText(text);
			titleLabel.setFont(FONT.deriveFont(MAX_TITLE_FONT_SIZE));

			int length = Integer.parseInt(status.get("length"));
			lengthLabel.setText(formatTime(length));

			playlistList.setSelectedIndex(
					remote.transformPlaylistID(Integer.parseInt(status.get("currentID")))
			);
			int selected = playlistList.getSelectedIndex();
			int size = playlistList.getModel().getSize();
			int min = selected < 5 ? 0 : selected-5;
			int max = selected > size-6 ? size-1 : selected+5;
			Rectangle r = playlistList.getCellBounds(min, max);
			playlistList.scrollRectToVisible(r);
		}

		int currentTime = Integer.parseInt(status.get("time"));
		double positionPercent = Double.parseDouble(status.get("position"));

		positionLabel.setText(formatTime(currentTime));
		progressBar.setValue((int) (positionPercent * progressBar.getMaximum()));
	}

	private void setUpdateDelay(AWTEvent e) {
		String input =
				JOptionPane.showInputDialog(this, "Set update delay (ms)", "Update Delay", INFORMATION_MESSAGE);
		if (StringUtils.isNumeric(input)) {
			updateLoop.cancel(true);
			updateDelay = Integer.parseInt(input);
			startUpdateLoop();
		}
		else
			handleException(new IllegalArgumentException("Input must be a number"));
	}

	private void startUpdateLoop() {
		updateLoop = Executors.newScheduledThreadPool(1).scheduleAtFixedRate(
				() -> updateInterface(remote.getStatus()),
				0, updateDelay, TimeUnit.MILLISECONDS
		);	}

	/*
	private void startUpdateLoop() {
		UpdateLoop l = new UpdateLoop(this);
		l.execute();
	}

	private class UpdateLoop extends SwingWorker<Map<String, String>, Map<String, String>> {

		private RemoteInterface _this;
		UpdateLoop(RemoteInterface _this) {
			this._this = _this;
		}

		@Override
		protected Map<String, String> doInBackground() throws Exception {
			Map<String, String> out = new HashMap<>();
			while (connected) {
				out = remote.getStatus();
				publish(out);
				Thread.sleep(updateDelay);
			}
			return out;
		}

		@Override
		protected void process(List<Map<String, String>> chunks) {
			chunks.stream().forEachOrdered(_this::updateInterface);
		}
	}//*/

	private void controlButtonPressed(ActionEvent e) {
		assert connected;

		String cmd = e.getActionCommand();
		assert Command.keys().contains(cmd);

		Map<String, String> status = remote.sendCommand(Command.valueOf(cmd));
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

	private void switchSongToSelected(AWTEvent e) {
		remote.switchSong(playlistList.getSelectedIndex());
		updateInterface();
	}

	private void clearFocus() {
		mainPanel.requestFocus();
	}

	private void handleException(Throwable e) {
		String text = "An error has occurred.<br><br>" + StringEscapeUtils.escapeHtml4(e.getMessage()) + "<br><br>Show the error?";
		int showErrorInt = JOptionPane.showConfirmDialog(this, restrictDialogWidth(text, false), "Error", YES_NO_OPTION, ERROR_MESSAGE);
		if (showErrorInt == 0) {
			String stackTrace = String.join("\n",
					Stream.of(e.getStackTrace())
							.map(StackTraceElement::toString)
							.collect(Collectors.toList()));

			JScrollPane pane = new JScrollPane(new JTextArea(stackTrace));
			pane.setPreferredSize(new Dimension(WIDTH-100, MAX_HEIGHT-100));

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
		JOptionPane.showMessageDialog(this, restrictDialogWidth(text), title, messageType);
	}

	private String restrictDialogWidth(String text, boolean escape) {
		text = escape ? StringEscapeUtils.escapeHtml4(text) : text;
		return String.format("<html><body><p style='width: %dpx'>%s</p></body></html>", WIDTH/2, text);
	}

	private String restrictDialogWidth(String text) {
		return restrictDialogWidth(text, false);
	}

	private class TitleResizeListener extends ComponentAdapter {
		@Override
		public void componentResized(ComponentEvent e) {
			float curSize = titleLabel.getFont().getSize();

			if (titleLabel.getSize().getWidth() >  MAX_TITLE_WIDTH && curSize >= MIN_TITLE_FONT_SIZE)
				titleLabel.setFont(FONT.deriveFont(curSize - 2));
		}
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
				remote.sendCommand(Command.SEEK_TO, (percent * 100) + "%");
				updateInterface();
			}
		}
	}

	private class PlaylistMouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				switchSongToSelected(null);
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

	@SuppressWarnings("unused")
	private static List<Component> getComponents(Container c) {
		return Arrays.asList(c.getComponents());
	}

	private static String formatTime(int seconds) {
		return DateTimeFormatter.ofPattern(seconds < 3600 ? "mm:ss" : "HH:mm:ss")
				.withZone(ZoneId.of("UTC"))
				.format(Instant.ofEpochMilli((long)(seconds*1000)));
	}

	private static void debugBorderComponents(Container con, boolean show) {
		Stream.of(con.getComponents())
				.filter(c -> c instanceof JComponent)
				.map(c -> (JComponent)c)
				.forEach(c -> {
					if (show) {
						Border red = BorderFactory.createLineBorder(Color.red);
						Border old = c.getBorder();
						if (old == null)
							c.setBorder(red);
						else
							c.setBorder(new CompoundBorder(red, old));
					}
					else {
						if (c.getBorder() instanceof CompoundBorder)
							c.setBorder(((CompoundBorder) c.getBorder()).getInsideBorder());
						else
							c.setBorder(null);
					}
					if (c instanceof JScrollPane) return;
					debugBorderComponents(c, show);
				});
	}

	@SuppressWarnings("unused")
	private enum Icon {
		NEXT,
		PREV,
		PLAY,
		PAUSE,
		REPEAT,
		LOOP,
		SHUFFLE,
		VOLUME_HIGH,
		VOLUME_LOW,
		VOLUME_NONE,
		VOLUME_OFF,
		PLAYLIST,
		SIZE (true);

		private ImageIcon imageIcon;
		private int ICON_SIZE = 24;
		private boolean isSize;

		Icon() {
			this(false);
		}

		Icon(boolean isSize) {
			this.isSize = isSize;
			if (!isSize) {
				String file = String.format("icons/%s.png", name().toLowerCase());
				imageIcon = new ImageIcon(getClass().getResource(file));
				imageIcon.setImage(imageIcon.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH));
			}
		}

		private ImageIcon get() {
			if (isSize)
				return null;
			else
				return imageIcon;
		}

		private Dimension getDim(double scale) {
			if (isSize)
				return  new Dimension((int) (ICON_SIZE * scale), (int) (ICON_SIZE * scale));
			else
				return null;
		}

		private Dimension getDim() {
			return getDim(1);
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			RemoteInterface r = new RemoteInterface();
			r.setVisible(true);
		});
	}

}
