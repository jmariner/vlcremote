package com.jmariner.vlcremote.gui;

import com.jmariner.vlcremote.util.InvalidHotkeyStringException;
import com.jmariner.vlcremote.util.UserSettings;
import com.tulskiy.keymaster.AWTTest;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static com.jmariner.vlcremote.util.Constants.*;

public class KeybindEditor extends JDialog {
	
	private RemoteInterface gui;
	
	private Preferences globalKeybinds;
	private Preferences localKeybinds;
	
	private JPanel mainPanel;
	private JButton editSelectedButton, saveButton;
	private JTable table;

	private Dialog keyPressDialog;
	
	private LinkedHashMap<String, String> keybindIdMap;
	
	private static final String QUERY_STRING = "Press a key...";
	private static final String DISABLED_STRING = "[Disabled]";
	
	private static final int LOCAL_COL = 2;
	private static final int GLOBAL_COL = 1;

	private static final List<String> ID_LIST = Arrays.asList(
			"Play/Pause:playPause",
			"Next:next",
			"Previous:prev",
			"Toggle Shuffle:toggleShuffle",
			"Toggle Repeat:toggleRepeat",
			"Toggle Loop:toggleLoop",
			"Toggle Mute:toggleMute",
			"Increase Volume:incVolume",
			"Decrease Volume:decVolume",
			"Search Playlist:searchPlaylist",
			"Restart Stream:restartStream"
	);
	
	private static final List<String> LOCAL_ONLY = Arrays.asList("searchPlaylist");
	private static final List<String> GLOBAL_ONLY = Arrays.asList("");
	
	protected KeybindEditor(RemoteInterface gui) {
		super(gui, "Keybind Editor", true);
		
		this.gui = gui;
		
		keybindIdMap = new LinkedHashMap<>();
		globalKeybinds = UserSettings.getChild("globalKeybinds");
		localKeybinds  = UserSettings.getChild("localKeybinds");
		
		ID_LIST.stream().forEachOrdered(s -> {
			String[] split = s.split(":");
			assert gui.getActions().containsKey(split[1]);

			keybindIdMap.put(split[0], split[1]);
		});
		
		init();
		initListeners();
		
		this.add(mainPanel);
		this.pack();

		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent e) {
				gui.getGlobalHotkeyHandler().clear();
				gui.getLocalHotkeyHandler().clear();
			}
			@Override
			public void windowClosed(WindowEvent e) {
				loadSettings();
			}
		});
	}
	
	private void init() {
				
		table = new KeybindTable();
		
		editSelectedButton = new JButton("Edit Selected");
		saveButton = new JButton("Save & Exit");
		JLabel title = new JLabel("Keybinds");
		title.setFont(FONT.deriveFont(18f).deriveFont(UNDERLINE));
		
		JPanel top = new JPanel(FLOW_CENTER);
		top.add(title);
		
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		JPanel bottomRight = new JPanel(FLOW_CENTER);
		JPanel bottomLeft = new JPanel(FLOW_CENTER);
		bottomLeft.add(editSelectedButton);
		bottomRight.add(saveButton);
		
		JPanel bottom = new JPanel(new GridLayout(1, 2));
		bottom.add(bottomLeft);
		bottom.add(bottomRight);
		
		mainPanel = new JPanel(new BorderLayout(0, MAIN_PADDING));
		mainPanel.setBorder(new EmptyBorder(MAIN_PADDING, MAIN_PADDING, MAIN_PADDING, MAIN_PADDING));
		mainPanel.setPreferredSize(new Dimension((int) (0.75*MAIN_WIDTH), (int) (MAIN_HEIGHT*1.5)));
		mainPanel.add(top, BorderLayout.NORTH);
		mainPanel.add(scrollPane, BorderLayout.CENTER);
		mainPanel.add(bottom, BorderLayout.SOUTH);
	}
	
	private void initListeners() {
		saveButton.addActionListener(e -> this.dispose());
		editSelectedButton.addActionListener(this::editSelectedKeybind);
		table.addMouseListener(new DoubleClickListener());
	}

	protected void loadSettings() {
		keybindIdMap.values().forEach(actionID -> {
			assert gui.getActions().containsKey(actionID);
			String globalKeystroke = globalKeybinds.get(actionID, null);
			String localKeystroke = localKeybinds.get(actionID, null);
			if (globalKeystroke != null) tryToRegister(globalKeystroke, actionID, true);
			if (localKeystroke != null) tryToRegister(localKeystroke, actionID, false);
		});
	}
	
	private void tryToRegister(String keystroke, String actionID, boolean global) {
		try {
			if (global)
				gui.getGlobalHotkeyHandler().registerHotkey(keystroke, gui.getAction(actionID));
			else
				gui.getLocalHotkeyHandler().registerHotkey(keystroke, actionID);
		} catch (InvalidHotkeyStringException e) {
			e.printStackTrace();
		}
	}
	
	private void editSelectedKeybind(AWTEvent e) {
		JPanel listener = new JPanel(FLOW_CENTER);
		listener.add(new JLabel(QUERY_STRING));
		listener.addKeyListener(new KeybindListener());
		listener.addHierarchyListener(e1 -> {
			Window w = SwingUtilities.getWindowAncestor(listener);
			if (w instanceof Dialog) {
				keyPressDialog = (Dialog) w;
				keyPressDialog.addWindowListener(new WindowAdapter() {
					@Override
					public void windowOpened(WindowEvent e) {
						Arrays.asList(keyPressDialog.getComponents()).forEach(c -> c.setFocusable(false));
						listener.setFocusable(true);
						listener.requestFocusInWindow();
					}
				});
			}
		});

		int choice = JOptionPane.showOptionDialog(this, listener, QUERY_STRING,
				JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
				new Object[]{"Unset", "Cancel"}, null
		);

		if (choice == 0) unsetSelected();
	}
	
	private void unsetSelected() {
		table.setValueAt("", table.getSelectedRow(), table.getSelectedColumn());
		if (table.isColumnSelected(GLOBAL_COL))
			globalKeybinds.remove(getSelectedId());
		if (table.isColumnSelected(LOCAL_COL))
			localKeybinds.remove(getSelectedId());
	}

	@SuppressWarnings({"RedundantCast", "cast"})
	private String getSelectedId() {
		int r = table.getSelectedRow();
		if (r == -1)
			return "";
		else
			return keybindIdMap.get((String) table.getValueAt(r, 0));
	}
	
	private class DoubleClickListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				int c = table.getSelectedColumn();
				if (c == LOCAL_COL || c == GLOBAL_COL) {
					if (!(LOCAL_ONLY.contains(getSelectedId()) && c == GLOBAL_COL) &&
						!(GLOBAL_ONLY.contains(getSelectedId()) && c == LOCAL_COL))
							editSelectedKeybind(e);
				}
			}
		}
	}
	
	private class KeybindListener extends KeyAdapter {
		@Override
		public void keyPressed(KeyEvent e) {
			if (!AWTTest.MODIFIERS.contains(e.getKeyCode())) {

				String keyString = KeyStroke.getKeyStrokeForEvent(e).toString();

				keyString = keyString.replace("pressed ", "");

				table.setValueAt(keyString, table.getSelectedRow(), table.getSelectedColumn());

				if (table.isColumnSelected(1))
					globalKeybinds.put(getSelectedId(), keyString);
				else if (table.isColumnSelected(2))
					localKeybinds.put(getSelectedId(), keyString);

				e.consume();

				keyPressDialog.dispose();
			}
		}
	}
	
	private class KeybindTable extends JTable {

		public KeybindTable() {
			super();
			
			Vector<Vector<String>> data =
					keybindIdMap.entrySet().stream()
					.map(e -> 
						new Vector<>(Arrays.asList(
								e.getKey(),
								globalKeybinds.get(e.getValue(), ""),
								localKeybinds.get(e.getValue(), "")
						))
					)
					.collect(Collectors.toCollection(Vector::new));

			Vector<String> cols = new Vector<>(Arrays.asList("Action", "Global", "Local"));
			
			this.setModel(new DefaultTableModel(data, cols));
			this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			this.setRowSelectionAllowed(true);
			this.setColumnSelectionAllowed(true);
			this.setDefaultEditor(Object.class, null);
		}
		
		@Override
		public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
			JComponent c = (JComponent) super.prepareRenderer(renderer, row, column);
			
			((DefaultTableCellRenderer)renderer)
					.setHorizontalAlignment(SwingConstants.CENTER);

			// without "Redundant Cast" it's "Suspicious call to LinkedHashMap.get" so I can't win here
			@SuppressWarnings({ "RedundantCast", "cast" })
			String id = keybindIdMap.get((String) table.getValueAt(row, 0));

			if ((LOCAL_ONLY.contains(id) && column == GLOBAL_COL) ||
					(GLOBAL_ONLY.contains(id) && column == LOCAL_COL)) {
						setValueAt(DISABLED_STRING, row, column);
			}

			return c;
		}
	}
}
