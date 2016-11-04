package com.jmariner.vlcremote.gui;

import com.jmariner.vlcremote.MyVLCRemote;
import com.jmariner.vlcremote.MyVLCRemote.Command;
import com.jmariner.vlcremote.util.GlobalHotkeyListener;
import com.jmariner.vlcremote.util.GuiUtils;
import com.jmariner.vlcremote.util.UserSettings;
import com.jmariner.vlcremote.util.VLCStatus;
import com.jtattoo.plaf.noire.NoireLookAndFeel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.intellij.lang.annotations.MagicConstant;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.jmariner.vlcremote.util.Constants.*;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static javax.swing.JOptionPane.*;

@Slf4j
@SuppressWarnings({"FieldCanBeLocal", "UnusedParameters"})
public class RemoteInterface extends JFrame {

	@Getter(AccessLevel.PROTECTED)
	private MyVLCRemote remote;
	@Getter(AccessLevel.PROTECTED)
	private GlobalHotkeyListener hotkeyListener;

	private MainMenuBar menuBar;

	private JPanel mainPanel;
	
	private LoginPanel loginPanel;
	private StatusPanel statusPanel;
	
	private ProgressPanel progressPanel;
	
	private ControlsPanel controlsPanel;
	
	private JSeparator mainSeparator;
	
	private PlaylistPanel playlistPanel;
	
	private KeybindEditor keybindEditor;
	
	private List<JComponent> controlComponents = new ArrayList<>();
	
	@Getter
	private Map<String, Runnable> actions;

	private ScheduledFuture<?> updateLoop;

	@Getter(AccessLevel.PROTECTED) @Setter(AccessLevel.PROTECTED)
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
		
		actions = new HashMap<>();
		hotkeyListener = new GlobalHotkeyListener();

		menuBar = new MainMenuBar(this);
		loginPanel = new LoginPanel(this);
		statusPanel = new StatusPanel();
		progressPanel = new ProgressPanel(this);
		controlsPanel = new ControlsPanel(this);
		playlistPanel = new PlaylistPanel(this);

		controlComponents.forEach(c -> {
			c.addKeyListener(new ClearFocusListener());
			c.setEnabled(false);
		});

		initActions();
		keybindEditor = new KeybindEditor(this);

		mainPanel = new JPanel(new BorderLayout(0, 20));
		mainPanel.setBorder(new EmptyBorder(MAIN_PADDING, MAIN_PADDING, MAIN_PADDING, MAIN_PADDING));
		mainPanel.setSize(MAIN_WIDTH, MAIN_HEIGHT);
		mainPanel.setFocusable(true);
		mainPanel.add(loginPanel, BorderLayout.NORTH);
		mainPanel.add(progressPanel, BorderLayout.CENTER);
		mainPanel.add(controlsPanel, BorderLayout.SOUTH);

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

	private void loadSettings() {
		menuBar.loadSettings();
		loginPanel.loadSettings();
		keybindEditor.loadSettings();
	}

	private void initActions() {
		double step = UserSettings.getDouble("volumeStep", 0.05);
		actions.put("incVolume", () -> remote.incrementVolume(step));
		actions.put("decVolume", () -> remote.incrementVolume(-step));
		actions.put("searchPlaylist", playlistPanel::startSearch);
	}
	
	protected Runnable getAction(String name) {
		Runnable r = actions.get(name);
		return r == null ? () -> {} : r;
	}

	protected void connect() {

		initRemote();
		connected = remote.testConnection();

		if (connected) {
			loginPanel.saveConnectionInfo();

			mainPanel.remove(loginPanel);

			mainPanel.add(statusPanel, BorderLayout.NORTH);

			controlComponents.forEach(b -> b.setEnabled(true));

			playlistPanel.init();

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

	protected void updateInterface() {
		updateInterface(null);
	}

	protected void updateInterface(VLCStatus status) {

		controlsPanel.updateVolume();

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

		controlsPanel.update(status);

		String filename = status.getFilename();
		String artist = status.getArtist();
		String title = status.getTitle();
		String text =
				title == null ? filename :
				artist == null ? title :
				artist + " - " + title;

		if (!statusPanel.getTitle().equals(text)) { // if we're on a new song
			statusPanel.setTitle(text);
			progressPanel.updateLength(status);
			playlistPanel.update(status);
		}

		progressPanel.update(status);
		
		menuBar.update(status);

	}

	protected void addControlComponent(JComponent c) {
		controlComponents.add(c);
	}
	
	protected void togglePlaylistArea(AWTEvent e) {
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

		controlsPanel.update((VLCStatus)null);
	}
	
	protected void editKeybindsPopup(AWTEvent e) {
		keybindEditor.setVisible(true);
	}

	private void startUpdateLoop() {
		updateLoop = Executors.newScheduledThreadPool(1).scheduleAtFixedRate(
				() -> updateInterface(remote.getStatus()),
				0, UserSettings.getInt("updateDelay", 1000), TimeUnit.MILLISECONDS
		);
	}

	protected void restartUpdateLoop() {
		if (!updateLoop.isCancelled())
			updateLoop.cancel(true);
		startUpdateLoop();
	}

	protected void clearFocus() {
		mainPanel.requestFocus();
	}

	protected void handleException(Throwable e) {
		String text = "An error has occurred.<br><br>" + StringEscapeUtils.escapeHtml4(e.getMessage()) + "<br><br>Show the error?";
		int showErrorInt = JOptionPane.showConfirmDialog(this, GuiUtils.restrictDialogWidth(text, false), "Error", YES_NO_OPTION, ERROR_MESSAGE);
		if (showErrorInt == 0) {

			String stackTrace = ExceptionUtils.getStackTrace(e);

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

	private class ClearFocusListener implements KeyListener {

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
			hotkeyListener.cleanup();
		}
	}

}