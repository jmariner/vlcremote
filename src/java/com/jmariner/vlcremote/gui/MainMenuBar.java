package com.jmariner.vlcremote.gui;

import com.jmariner.vlcremote.MyVLCRemote.Command;
import com.jmariner.vlcremote.util.GuiUtils;
import com.jmariner.vlcremote.util.UserSettings;
import com.jmariner.vlcremote.util.VLCStatus;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jmariner.vlcremote.util.Constants.MAIN_WIDTH;
import static com.jmariner.vlcremote.util.Constants.MENUBAR_HEIGHT;
import static java.awt.event.KeyEvent.VK_O;
import static java.awt.event.KeyEvent.VK_T;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;

public class MainMenuBar extends JMenuBar {

	private RemoteInterface gui;

	private boolean instantPauseEnabled;

	private JCheckBoxMenuItem instantPause;
	private JCheckBoxMenuItem debugBorders;
	private JMenuItem restartStream, gotoPreferences,
						updateDelayInput, setEqPreset,
						editKeybinds;
	
	private List<String> eqPresets;
	private Map<String,  JRadioButtonMenuItem> eqPresetButtons;
	private ButtonGroup eqPresetGroup;

	protected MainMenuBar(RemoteInterface gui) {
		super();

		this.gui = gui;
		
		this.eqPresets = null;

		this.eqPresetButtons = new HashMap<>();

		init();
		initListeners();
	}

	private void init() {

		restartStream = new JMenuItem("Restart stream");
		gotoPreferences = new JMenuItem("Show Preferences File");
		debugBorders = new JCheckBoxMenuItem("Show debug borders");
		updateDelayInput = new JMenuItem("Set update delay");
		instantPause = new JCheckBoxMenuItem("Enable instant pause");
		setEqPreset = new JMenu("Equalizer");
		editKeybinds = new JMenuItem("Edit Keybinds...");

		JMenu tools = new JMenu("Tools");
		tools.setMnemonic(VK_T);
		tools.add(restartStream);
		tools.add(gotoPreferences);
		tools.add(editKeybinds);

		JMenu options = new JMenu("Options");
		options.setMnemonic(VK_O);
		options.add(updateDelayInput);
		options.add(debugBorders);
		options.add(instantPause);
		options.add(setEqPreset);
		
		setEqPreset.setEnabled(false);
		restartStream.setEnabled(false);
		editKeybinds.setEnabled(false);

		this.add(tools);
		this.add(options);
		this.setPreferredSize(new Dimension(MAIN_WIDTH, MENUBAR_HEIGHT));
	}

	private void initEqPresets() {
		setEqPreset.removeAll();
		eqPresetGroup = new ButtonGroup();
		eqPresets.stream().forEachOrdered(s -> {
			JRadioButtonMenuItem b = new JRadioButtonMenuItem(s);

			int presetId = eqPresets.indexOf(s);
			b.addActionListener(e -> {
				gui.getRemote().sendCommand(Command.SET_EQ_ENABLED, "1");
				gui.getRemote().sendCommand(Command.SET_EQ_PRESET, ""+presetId);
			});

			setEqPreset.add(b);
			eqPresetGroup.add(b);

			s = s.toLowerCase().replaceAll("\\s", "").replace("and", "");
			eqPresetButtons.put(s, b);
		});
	}
	
	protected void initPost() {
		restartStream.setEnabled(true);
		editKeybinds.setEnabled(true);
	}

	private void initListeners() {
		debugBorders.addActionListener(e -> GuiUtils.debugBorderComponents(gui, ((JCheckBoxMenuItem)e.getSource()).isSelected()));
		instantPause.addActionListener(e -> {
			instantPauseEnabled = ((JCheckBoxMenuItem)e.getSource()).isSelected();
			UserSettings.putBoolean("instantPause", instantPauseEnabled);
		});

		restartStream.addActionListener(e -> gui.getAction("restartStream").run());
		gotoPreferences.addActionListener(e -> UserSettings.viewPreferencesFile());
		updateDelayInput.addActionListener(this::setUpdateDelay);
		editKeybinds.addActionListener(gui::editKeybindsPopup);
	}
	
	protected void update(VLCStatus status) {
		if (eqPresets == null) {
			eqPresets = status.getEqPresets();
			initEqPresets();
		}
		setEqPreset.setEnabled(eqPresets.size() > 0);

		JRadioButtonMenuItem curEqButton = eqPresetButtons.get(status.getEqPreset());
		if (!curEqButton.isSelected()) {
		//	eqPresetGroup.clearSelection();
			eqPresetGroup.setSelected(curEqButton.getModel(), true);
		}

	}
	
	private void setUpdateDelay(AWTEvent e) {
		String input =
				JOptionPane.showInputDialog(this,
						GuiUtils.restrictDialogWidth("Set update delay (ms)<br>Default: 1000; Current: " + UserSettings.get("updateDelay", "1000")),
						"Update Delay", INFORMATION_MESSAGE);
		if (StringUtils.isNumeric(input)) {
			int d =  Integer.parseInt(input);
			UserSettings.putInt("updateDelay", d);
			UserSettings.forceUpdate();
			gui.restartUpdateLoop();
		}
		else
			gui.handleException(new IllegalArgumentException("Input must be a number"));
	}
	
	private void setEqPreset(AWTEvent e) {
		if (eqPresets != null) {
			String input = (String) JOptionPane.showInputDialog(gui, 
					"Select a preset", "Equalizer Preset",
					JOptionPane.QUESTION_MESSAGE, null,
					eqPresets.toArray(new String[eqPresets.size()]),
					eqPresets.get(0)
			);
			int presetId = eqPresets.indexOf(input);
			gui.getRemote().sendCommand(Command.SET_EQ_ENABLED, "1");
			gui.getRemote().sendCommand(Command.SET_EQ_PRESET, ""+presetId);
		}
	}

	protected void loadSettings() {
		instantPauseEnabled = UserSettings.getBoolean("instantPause", false);
		instantPause.setSelected(instantPauseEnabled);
	}

}
