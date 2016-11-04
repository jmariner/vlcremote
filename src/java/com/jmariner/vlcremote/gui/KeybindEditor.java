package com.jmariner.vlcremote.gui;

import com.jmariner.vlcremote.util.InvalidHotkeyStringException;
import com.jmariner.vlcremote.util.UserSettings;
import com.tulskiy.keymaster.AWTTest;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
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

@Slf4j
public class KeybindEditor extends JDialog {
	
	private RemoteInterface gui;
	
	private Preferences keybinds;
	
	private JPanel mainPanel;
	private JButton editSelectedButton, saveButton;
	private JTable table;

	private Dialog keyPressDialog;
	
	private LinkedHashMap<String, String> keybindIdMap;

	private List<String> idList = Arrays.asList(
			"Play/Pause:playPause",
			"Next:next",
			"Previous:prev",
			"Toggle Shuffle:toggleShuffle",
			"Toggle Repeat:toggleRepeat",
			"Toggle Loop:toggleLoop",
			"Toggle Mute:toggleMute",
			"Increase Volume:incVolume",
			"Decrease Volume:decVolume",
			"Search Playlist:searchPlaylist"
	);
	
	public KeybindEditor(RemoteInterface gui) {
		super(gui, "Keybind Editor", true);
		
		this.gui = gui;
		
		keybindIdMap = new LinkedHashMap<>();
		keybinds = UserSettings.getChild("keybinds");
		
		idList.stream().forEachOrdered(s -> {
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
		bottomRight.add(editSelectedButton);
		JPanel bottomLeft = new JPanel(FLOW_CENTER);
		bottomLeft.add(saveButton);
		
		JPanel bottom = new JPanel(new GridLayout(1, 2));
		bottom.add(bottomLeft);
		bottom.add(bottomRight);
		
		mainPanel = new JPanel(new BorderLayout(0, MAIN_PADDING));
		mainPanel.setBorder(new EmptyBorder(MAIN_PADDING, MAIN_PADDING, MAIN_PADDING, MAIN_PADDING));
		mainPanel.setPreferredSize(new Dimension((int) (0.75*MAIN_WIDTH), MAIN_HEIGHT));
		mainPanel.add(top, BorderLayout.NORTH);
		mainPanel.add(scrollPane, BorderLayout.CENTER);
		mainPanel.add(bottom, BorderLayout.SOUTH);
	}
	
	private void initListeners() {
		saveButton.addActionListener(e -> this.dispose());
		editSelectedButton.addActionListener(this::editSelectedKeybind);
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2)
					editSelectedKeybind(null);
			}
		});
	}

	protected void loadSettings() {

		gui.getHotkeyListener().clear();

		keybindIdMap.values().forEach(s -> {
			assert gui.getActions().containsKey(s);
			String keystroke = keybinds.get(s, null);
			if (keystroke != null) {
				try {
					Runnable a = gui.getAction(s);
					gui.getHotkeyListener().registerHotkey(keystroke, a);
				} catch (InvalidHotkeyStringException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	private void editSelectedKeybind(AWTEvent e) {
		JPanel listener = new JPanel();
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

		int choice = JOptionPane.showOptionDialog(this, listener, "Press a key...",
				JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
				new Object[]{"Unset", "Cancel"}, null
		);

		if (choice == 0) {
			keybinds.remove(getSelectedId());
		}
	}

	private class KeybindListener extends KeyAdapter {
		@Override
		public void keyPressed(KeyEvent e) {
			if (!AWTTest.MODIFIERS.contains(e.getKeyCode())) {

				String keyString = KeyStroke.getKeyStrokeForEvent(e).toString();

				keyString = keyString.replace("pressed ", "");

				table.setValueAt(keyString, table.getSelectedRow(), 1);

				keybinds.put(getSelectedId(), keyString);

				e.consume();

				keyPressDialog.dispose();
			}
		}
	}

	private String getSelectedId() {
		//noinspection RedundantCast
		return keybindIdMap.get((String) table.getValueAt(table.getSelectedRow(), 0));
	}
	
	private class KeybindTable extends JTable {
		
		private LineBorder highlightBorder;
		private MatteBorder leftHighlight;
		private MatteBorder rightHighlight;

		public KeybindTable() {
			super();
			
			Vector<Vector<String>> data =
					keybindIdMap.entrySet().stream()
					.map(e -> 
						new Vector<>(Arrays.asList(
								e.getKey(),
								keybinds.get(e.getValue(), "")
						))
					)
					.collect(Collectors.toCollection(Vector::new));

			Vector<String> cols = new Vector<>(Arrays.asList("Action", "Keybind"));
			
			this.setModel(new DefaultTableModel(data, cols));
			this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			this.setRowSelectionAllowed(true);
			this.setColumnSelectionAllowed(false);
			this.setDefaultEditor(Object.class, null);

			highlightBorder = (LineBorder) UIManager.getBorder("Table.focusCellHighlightBorder");
			leftHighlight = new MatteBorder(1, 1, 1, 0, highlightBorder.getLineColor());
			rightHighlight = new MatteBorder(1, 0, 1, 1, highlightBorder.getLineColor());
		}
		
		@Override
		public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
			JComponent c = (JComponent) super.prepareRenderer(renderer, row, column);
			
			if (isRowSelected(row)) {
				if (column == 0)
					c.setBorder(leftHighlight);
				if (column == 1)
					c.setBorder(rightHighlight);
			}
			
			((DefaultTableCellRenderer)renderer)
					.setHorizontalAlignment(SwingConstants.CENTER);

			return c;
		}
	}
}
