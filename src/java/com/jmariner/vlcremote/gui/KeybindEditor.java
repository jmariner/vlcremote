package com.jmariner.vlcremote.gui;

import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.function.Supplier;
import java.util.prefs.Preferences;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import com.jmariner.vlcremote.util.UserSettings;

public class KeybindEditor extends JPanel {
	
	private RemoteInterface gui;
	
	private Preferences keybinds;
	
	private JTable table;
	private Map<String, String> keybindIdMap;
	private Map<String, Runnable> actions;
	
	public KeybindEditor(RemoteInterface gui) {
		super(new BorderLayout(0, 0));
		
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
		
	}
	
	private void init() {
		
	//	Collector s = Collectors.toCollection(Vector::new);
		
		Vector<Vector<String>> data = new Vector<>();
		Vector<String> names = new Vector<>(keybindIdMap.keySet());
		Vector<String> binds = 
				names.stream().map(s -> keybinds.get(s, "")).collect(Collectors.toCollection(Vector::new));
		
		data.add(names);
		data.add(binds);
		
		Vector<String> cols = Stream.of("Action", "Keybind").collect(Collectors.toCollection(Vector::new));
		
		DefaultTableModel model = new DefaultTableModel(data, cols);
		table = new JTable(model);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowSelectionAllowed(true);
		table.setColumnSelectionAllowed(false);
	}
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			RemoteInterface r = new RemoteInterface();
			KeybindEditor e = new KeybindEditor(r);
		//	r.setVisible(true);
			e.setVisible(true);
		});
	}
	
}
