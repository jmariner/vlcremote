package com.jmariner.vlcremote.gui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import static com.jmariner.vlcremote.util.Constants.*;
import com.jmariner.vlcremote.util.UserSettings;

public class KeybindEditor extends JDialog {
	
	private RemoteInterface gui;
	
	private Preferences keybinds;
	
	private JPanel mainPanel;
	private JLabel title;
	private JButton editSelectedButton, saveButton;
	private JTable table;
	
	private Map<String, String> keybindIdMap;
	private Map<String, Runnable> actions;
	
	public KeybindEditor(RemoteInterface gui) {
		super(gui, "Keybind Editor", true);
		
		this.gui = gui;
		
		keybindIdMap = new HashMap<>();
		actions = new HashMap<>();
		keybinds = UserSettings.getChild("keybinds");
		
		Arrays.asList(
				"Play/Pause:playPause", "Next:next", "Previous:prev",
				"Toggle Shuffle:toggleShuffle", "Toggle Repeat:toggleRepeat", "Toggle Loop:toggleLoop",
				"Toggle Mute:toggleMute"
		).forEach(s -> {
			String[] split = s.split(":");
			keybindIdMap.put(split[0], split[1]);
			actions.put(split[1], gui.getButton(split[1])::doClick);
		});
		
		init();
		initListeners();
		
		this.add(mainPanel);
		this.pack();
		
		//fix last cell highlight being cut off at the bottom
		this.setSize(getWidth(), getHeight()+1);
	}
	
	private void init() {
				
		table = new KeybindTable();
		
		table.setRowHeight(table.getRowHeight()+10);
		
		editSelectedButton = new JButton("Edit Selected");
		saveButton = new JButton("Save & Exit");
		title = new JLabel("Keybinds");
		title.setFont(FONT.deriveFont(18f).deriveFont(UNDERLINE));
		
		JPanel top = new JPanel(FLOW_CENTER);
		top.add(title);
		
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		JPanel bottomLeft = new JPanel(FLOW_CENTER);
		bottomLeft.add(editSelectedButton);
		JPanel bottomRight = new JPanel(FLOW_CENTER);
		bottomRight.add(saveButton);
		
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
		saveButton.addActionListener(this::saveAndClose);
		editSelectedButton.addActionListener(this::editSelectedKeybind);
	}
	
	private void saveAndClose(AWTEvent e) {
		
	}
	
	private void editSelectedKeybind(AWTEvent e) {
		
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
			
			this.setModel(new DefaultTableModel(
				data,
				new Vector<>(Arrays.asList("Action", "Keybind"))
			));
			
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
			
			if (column == 1) {
				((DefaultTableCellRenderer)renderer).setHorizontalAlignment(SwingConstants.RIGHT);
			}
			
			return c;
		}
	}
}
