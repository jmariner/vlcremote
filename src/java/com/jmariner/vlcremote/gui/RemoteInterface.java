package com.jmariner.vlcremote.gui;

import com.jmariner.vlcremote.MyVLCRemote;
import com.jmariner.vlcremote.MyVLCRemote.Command;
import com.jmariner.vlcremote.gui.playlist.PlaylistPanel;
import com.jmariner.vlcremote.util.*;
import com.jmariner.vlcremote.util.VLCStatus.State;
import com.jtattoo.plaf.noire.NoireLookAndFeel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.intellij.lang.annotations.MagicConstant;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jmariner.vlcremote.util.Constants.*;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static javax.swing.JOptionPane.*;

@SuppressWarnings({"FieldCanBeLocal", "UnusedParameters"})
public class RemoteInterface extends JFrame {

	@Getter
	private MyVLCRemote remote;
	@Getter(AccessLevel.PROTECTED)
	private GlobalHotkeyHandler globalHotkeyHandler;
	@Getter(AccessLevel.PROTECTED)
	private LocalHotkeyHandler localHotkeyHandler;

	private JPanel mainPanel, primaryCard, songListCard;
	private CardLayout cardLayout;
	private JSeparator mainSeparator;

	private MainMenuBar menuBar;
	private LoginPanel loginPanel;
	private StatusPanel statusPanel;
	private ProgressPanel progressPanel;
	private ControlsPanel controlsPanel;
	private PlaylistPanel playlistPanel;
	private KeybindEditor keybindEditor;

	private List<JPanel> panels;
	private List<JComponent> controlComponents;
	private List<JTextField> textFields;

	@Getter
	private Map<String, Runnable> actions;

	private ScheduledFuture<?> updateLoop;

	@Getter @Setter
	private boolean connected, playlistAreaShowing;
	
	private static final String PRIMARY_CARD = "Main";
	private static final String SONGLIST_CARD = "Song List";
	
	protected static final List<String> CARD_NAMES =
			Arrays.asList(PRIMARY_CARD, SONGLIST_CARD);

	// this is to create a NullPointerException if i try using the superclass's HEIGHT value of 1
	@SuppressWarnings("unused") private static final Object HEIGHT = null;

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

		panels = new ArrayList<>();
		textFields = new ArrayList<>();
		controlComponents = new ArrayList<>();
		actions = new HashMap<>();

		menuBar = new MainMenuBar(this);
		loginPanel = new LoginPanel(this);
		statusPanel = new StatusPanel();
		progressPanel = new ProgressPanel(this);
		controlsPanel = new ControlsPanel(this);
		playlistPanel = new PlaylistPanel(this);

		mainPanel = new JPanel(new BorderLayout(0, 20));
		mainPanel.setBorder(new EmptyBorder(MAIN_PADDING, MAIN_PADDING, MAIN_PADDING, MAIN_PADDING));
		mainPanel.setPreferredSize(new Dimension(MAIN_WIDTH, MAIN_HEIGHT));
		mainPanel.setFocusable(true);
		mainPanel.add(loginPanel, BorderLayout.NORTH);
		mainPanel.add(progressPanel, BorderLayout.CENTER);
		mainPanel.add(controlsPanel, BorderLayout.SOUTH);

		mainSeparator = new JSeparator();
		mainSeparator.setPreferredSize(new Dimension(MAIN_WIDTH, SEPARATOR_HEIGHT));

		ClearFocusListener clearFocus = new ClearFocusListener();

		panels = Arrays.asList(
				mainPanel, playlistPanel, loginPanel, progressPanel, controlsPanel, statusPanel);
		
		controlComponents.forEach(c -> {
			c.addKeyListener(clearFocus);
			c.setEnabled(false);
		});

		panels.forEach(c -> {
			c.addMouseListener(clearFocus);
			textFields.addAll(GuiUtils.getComponents(c, JTextField.class));
		});

		textFields.forEach(t -> t.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) { focusChanged(true);}
			@Override
			public void focusLost(FocusEvent e) { focusChanged(false); }

			private void focusChanged(boolean f) {
				if (localHotkeyHandler != null)
					localHotkeyHandler.setEnabled(!f);
			}
		}));
		
		primaryCard = new JPanel(new BorderLayout());
		primaryCard.add(mainPanel);
		primaryCard.setPreferredSize(new Dimension(MAIN_WIDTH, MAIN_HEIGHT));
		
		songListCard = new JPanel();
		songListCard.setPreferredSize(new Dimension(MAIN_WIDTH, MAIN_HEIGHT + PLAYLIST_HEIGHT));
		
		cardLayout = new PageViewer();

	//	this.setLayout(new BorderLayout());
	//	this.add(mainPanel, BorderLayout.NORTH);
		this.setLayout(cardLayout);
		this.add(primaryCard, PRIMARY_CARD);
		this.add(songListCard, SONGLIST_CARD);
		this.setJMenuBar(menuBar);
		this.setTitle("VLC Remote");
	//	this.setSize(MAIN_WIDTH, MAIN_HEIGHT + MENUBAR_HEIGHT);
		this.setResizable(false);
		this.pack();
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		Runtime.getRuntime().addShutdownHook(new CleanupOnShutdown());

		loadSettings();
	}

	private void loadSettings() {
		menuBar.loadSettings();
		loginPanel.loadSettings();
		
		if (UserSettings.getBoolean("autoconnect", false)) {
			if (UserSettings.keyExists("httpPass"))
				loginPanel.connectPressed(null);
			else
				UserSettings.getRoot().remove("autoconnect");
		}
	}

	public void togglePlaylistArea(AWTEvent e) {
		playlistAreaShowing = !playlistAreaShowing;

		if (playlistAreaShowing) {
		//	this.setSize(MAIN_WIDTH, MAIN_HEIGHT + MENUBAR_HEIGHT + PLAYLIST_HEIGHT);
		//	this.add(mainSeparator, BorderLayout.CENTER);
		//	this.add(playlistPanel, BorderLayout.SOUTH);
			primaryCard.setPreferredSize(new Dimension(MAIN_WIDTH, MAIN_HEIGHT + PLAYLIST_HEIGHT + SEPARATOR_HEIGHT));
			primaryCard.add(mainSeparator, BorderLayout.CENTER);
			primaryCard.add(playlistPanel, BorderLayout.SOUTH);
		}
		else {
		//	this.setSize(MAIN_WIDTH, MAIN_HEIGHT + MENUBAR_HEIGHT);
		//	this.remove(mainSeparator);
		//	this.remove(playlistPanel);
			primaryCard.setPreferredSize(new Dimension(MAIN_WIDTH, MAIN_HEIGHT));
			primaryCard.remove(mainSeparator);
			primaryCard.remove(playlistPanel);
		}
		
		this.revalidate();
		this.pack();

		controlsPanel.update((VLCStatus)null);
	}
	
	protected void setVisibleCard(String cardName) {
		assert CARD_NAMES.contains(cardName);
		cardLayout.show(this.getContentPane(), cardName);
	}

	private void initActions() {
		int step = UserSettings.getInt("volumeStep", 5);
		
		actions.put("incVolume", () -> {
			remote.getPlayer().incrementVolume(step);
			controlsPanel.updateVolume();
		});
		actions.put("decVolume", () -> {
			remote.getPlayer().incrementVolume(-step);
			controlsPanel.updateVolume();
		});
		
		actions.put("searchPlaylist", playlistPanel::startSearch);
		actions.put("restartStream", () -> remote.getPlayer().restart(1000));
	}
	
	protected Runnable getAction(String name) {
		Runnable r = actions.get(name);
		return r == null ? () -> {} : r;
	}

	protected void connect() {

		remote = new MyVLCRemote(
				loginPanel.getHost(),
				loginPanel.getHttpPort(),
				loginPanel.getPassword(),
				loginPanel.getStreamPort(),
				this::handleException
		);
		
		connected = remote.testConnection();

		if (connected) {
			loginPanel.saveConnectionInfo();

			initPost();

			remote.setSourceVolume(1);
			remote.sendCommand(Command.PLAY);
			updateInterface(remote.getNewStatus());
			startUpdateLoop();
			remote.getPlayer().start();
		}
	}
	
	private void initPost() {
		mainPanel.remove(loginPanel);

		mainPanel.add(statusPanel, BorderLayout.NORTH);

		controlComponents.forEach(b -> b.setEnabled(true));

		playlistPanel.initPost();
		
		initActions();
		globalHotkeyHandler = new GlobalHotkeyHandler();
		localHotkeyHandler = new LocalHotkeyHandler(this);
		keybindEditor = new KeybindEditor(this);
		keybindEditor.loadSettings();
		
		menuBar.initPost();
	}

	protected void updateInterface() {
		updateInterface(null);
	}

	public void updateInterface(VLCStatus status) {

		controlsPanel.updateVolume();

		if (status == null) return; // everything past here requires status

		boolean validState =
				status.getState() == State.PAUSED ||
				status.getState() == State.PLAYING;

		if (!validState) return;

		String text = status.getCurrentSong().toString();
		if (!statusPanel.getTitle().equals(text)) { // if we're on a new song
			statusPanel.setTitle(text);
			progressPanel.updateLength(status);
			playlistPanel.update(status);
		}
		
		controlsPanel.update(status);
		progressPanel.update(status);
		menuBar.update(status);

	}

	public void addControlComponent(JComponent c) {
		controlComponents.add(c);
	}
	
	protected void editKeybindsPopup(AWTEvent e) {
		keybindEditor.setVisible(true);
	}
	
	protected void setGlobalHotkeysEnabled(boolean enabled) {
		globalHotkeyHandler.setEnabled(enabled);
	}

	private void startUpdateLoop() {
		updateLoop = Executors
				.newSingleThreadScheduledExecutor(r -> new Thread(r, "Update Loop"))
				.scheduleAtFixedRate(() -> {
					updateInterface(remote.getNewStatus());
					heartbeat();
				}, 0, UserSettings.getInt("updateDelay", 1000), TimeUnit.MILLISECONDS
		);
	}

	protected void restartUpdateLoop() {
		if (!updateLoop.isCancelled())
			updateLoop.cancel(true);
		startUpdateLoop();
	}
	
	private void heartbeat() {
		// TODO heartbeat
	}

	public void clearFocus() {
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
	
	private class PageViewer extends CardLayout {
		@Override
		public Dimension preferredLayoutSize(Container parent) {
			
			List<Component> visibles = 
					Arrays.asList(parent.getComponents())
					.stream().filter(Component::isVisible).collect(Collectors.toList());
					
			if (visibles.size() > 0) {
				Component current = visibles.get(0);
				Insets i = parent.getInsets();
				Dimension pref = current.getPreferredSize();
				pref.width += i.left + i.right;
				pref.height += i.top + i.bottom;
				return pref;
			}
			return super.preferredLayoutSize(parent);
		}
	}

	private class ClearFocusListener extends MouseAdapter implements KeyListener {
		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == VK_ESCAPE)
				clearFocus();
		}

		public void keyTyped(KeyEvent e) {}
		public void keyReleased(KeyEvent e) {}
		
		@Override
		public void mouseClicked(MouseEvent e) {
			clearFocus();
		}
	}

	private class CleanupOnShutdown extends Thread {
		@Override
		public void run() {
			if (connected) {
				remote.getPlayer().stop();
				remote.sendCommand(Command.PAUSE);
			}
			if (globalHotkeyHandler != null)
				globalHotkeyHandler.cleanup();
		}
	}

}
