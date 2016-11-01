package com.jmariner.vlcremote.gui;

import com.jmariner.vlcremote.util.GuiUtils;
import com.jmariner.vlcremote.util.UserSettings;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;

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
	private JMenuItem restartStream;
	private JMenuItem gotoPreferences;
	private JMenuItem updateDelayInput;

	protected MainMenuBar(RemoteInterface gui) {
		super();

		this.gui = gui;

		init();
		initListeners();
	}

	private void init() {

		restartStream = new JMenuItem("Restart stream");
		gotoPreferences = new JMenuItem("Show Preferences File");
		debugBorders = new JCheckBoxMenuItem("Show debug borders");
		updateDelayInput = new JMenuItem("Set update delay");
		instantPause = new JCheckBoxMenuItem("Enable instant pause");

		JMenu tools = new JMenu("Tools");
		tools.setMnemonic(VK_T);
		tools.add(restartStream);
		tools.add(gotoPreferences);

		JMenu options = new JMenu("Options");
		options.setMnemonic(VK_O);
		options.add(updateDelayInput);
		options.add(debugBorders);
		options.add(instantPause);

		this.add(tools);
		this.add(options);
		this.setPreferredSize(new Dimension(MAIN_WIDTH, MENUBAR_HEIGHT));
	}

	private void initListeners() {
		debugBorders.addActionListener(e -> GuiUtils.debugBorderComponents(gui, ((JCheckBoxMenuItem)e.getSource()).isSelected()));
		instantPause.addActionListener(e -> {
			instantPauseEnabled = ((JCheckBoxMenuItem)e.getSource()).isSelected();
			UserSettings.putBoolean("instantPause", instantPauseEnabled);
		});

		restartStream.addActionListener(e -> gui.getRemote().restartStream());
		gotoPreferences.addActionListener(e -> UserSettings.viewPreferencesFile());
		updateDelayInput.addActionListener(this::setUpdateDelay);
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

	protected void loadSettings() {
		instantPauseEnabled = UserSettings.getBoolean("instantPause", false);
		instantPause.setSelected(instantPauseEnabled);
	}

}
