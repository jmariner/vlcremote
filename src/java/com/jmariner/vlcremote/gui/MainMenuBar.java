package com.jmariner.vlcremote.gui;

import com.jmariner.vlcremote.MyVLCRemote.Command;
import com.jmariner.vlcremote.util.GuiUtils;
import com.jmariner.vlcremote.util.UserSettings;
import com.jmariner.vlcremote.util.VLCStatus;

import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.jmariner.vlcremote.util.Constants.MAIN_WIDTH;
import static com.jmariner.vlcremote.util.Constants.MENUBAR_HEIGHT;
import static java.awt.event.KeyEvent.VK_O;
import static java.awt.event.KeyEvent.VK_T;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;

public class MainMenuBar extends JMenuBar {

	private RemoteInterface gui;

	private JCheckBoxMenuItem instantPause, debugBorders,
						restartOnChange, disableGlobalHotkeys;
	private JMenuItem restartStream, gotoPreferences,
						updateDelayInput, setEqPreset,
						editKeybinds, resetPassSave;
	
	private Map<String, JToggleButton> cardButtons;
	
	private List<String> eqPresets;
	private Map<String,  JRadioButtonMenuItem> eqPresetButtons;
	private ButtonGroup eqPresetGroup, cardButtonGroup;
	
	private static final Color MENU_SELECT_BG = UIManager.getColor("Menu.selectionBackground");
	private static final Color MENU_DEFAULT_BG = UIManager.getColor("Menu.background");

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
		gotoPreferences = new JMenuItem("Show preferences file");
		debugBorders = new JCheckBoxMenuItem("Show debug borders");
		updateDelayInput = new JMenuItem("Set update delay");
		restartOnChange = new JCheckBoxMenuItem("Restart stream on track change");
		instantPause = new JCheckBoxMenuItem("Enable instant pause");
		disableGlobalHotkeys = new JCheckBoxMenuItem("Disabled global hotkeys");
		setEqPreset = new JMenu("Equalizer");
		editKeybinds = new JMenuItem("Edit keybinds...");
		resetPassSave = new JMenuItem("Reset saved password status");

		JMenu tools = new JMenu("Tools");
		tools.setMnemonic(VK_T);
		tools.add(restartStream);
		tools.add(gotoPreferences);
		tools.add(editKeybinds);

		JMenu options = new JMenu("Options");
		options.setMnemonic(VK_O);
		options.add(updateDelayInput);
		options.add(resetPassSave);
		options.add(instantPause);
		options.add(restartOnChange);
		options.add(disableGlobalHotkeys);
		options.add(setEqPreset);
		options.add(debugBorders);
		
		setEqPreset.setEnabled(false);
		restartStream.setEnabled(false);
		editKeybinds.setEnabled(false);
		disableGlobalHotkeys.setEnabled(false);
		
		cardButtons = RemoteInterface.CARD_NAMES.stream()
				.collect(Collectors.toMap(Function.identity(), MenuCardButton::new));
		
		cardButtonGroup = new ButtonGroup();
		cardButtons.values().forEach(b -> {
			cardButtonGroup.add(b);
		});
		
		cardButtons.forEach((n, b) -> b.addActionListener(e -> {
			gui.setVisibleCard(n);
		}));
		
		this.add(tools);
		this.add(options);
		this.add(Box.createHorizontalGlue());
		new ArrayDeque<>(cardButtons.values()).descendingIterator().forEachRemaining(this::add);
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
		disableGlobalHotkeys.setSelected(false);
	}
	
	protected void loadSettings() {
		instantPause.setSelected(UserSettings.getBoolean("instantPause", false));
		restartOnChange.setSelected(UserSettings.getBoolean("restartOnTrackChange", false));
	}

	private void initListeners() {
		debugBorders.addActionListener(e -> GuiUtils.debugBorderComponents(gui, debugBorders.isSelected()));
		
		restartOnChange.addActionListener(e -> 
			UserSettings.putBoolean("restartOnTrackChange", restartOnChange.isSelected()));
		
		instantPause.addActionListener(e -> 
			UserSettings.putBoolean("instantPause", instantPause.isSelected()));
		
		disableGlobalHotkeys.addActionListener(e -> 
			gui.setGlobalHotkeysEnabled(!disableGlobalHotkeys.isSelected()));

		restartStream.addActionListener(e -> gui.getAction("restartStream").run());
		gotoPreferences.addActionListener(e -> UserSettings.viewPreferencesFile());
		updateDelayInput.addActionListener(this::setUpdateDelay);
		editKeybinds.addActionListener(gui::editKeybindsPopup);
		resetPassSave.addActionListener(this::resetPass);
	}
	
	protected void update(VLCStatus status) {
		
		if (eqPresets == null) {
			eqPresets = status.getEqPresets();
			initEqPresets();
		}
		setEqPreset.setEnabled(eqPresets.size() > 0);

		JRadioButtonMenuItem curEqButton = eqPresetButtons.get(status.getEqPreset());
		if (!curEqButton.isSelected()) {
			eqPresetGroup.setSelected(curEqButton.getModel(), true);
		}

	}
	
	protected void updateCardTabs(String currentCard) {
		cardButtonGroup.setSelected(cardButtons.get(currentCard).getModel(), true);
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
	
	private void resetPass(AWTEvent e) {
		UserSettings.remove("httpPass");
		UserSettings.remove("saveHttpPass");
		UserSettings.remove("autoconnect");
		JOptionPane.showMessageDialog(gui,
				"Saved password status has been reset.",
				"Password reset",
				INFORMATION_MESSAGE);
	}
	
	private class MenuCardButton extends JToggleButton {
		
		private Border selectBorder, defaultBorder;
		
		public MenuCardButton(String text) {
			super(text);
			setFocusPainted(false);
			setOpaque(true);
			setForeground(UIManager.getColor("Button.selected"));
			setBackground(MENU_DEFAULT_BG);
			
			Insets i = getBorder().getBorderInsets(this);
			Insets s = new Insets(i.top-1, i.left-1, i.bottom-1, i.right-1);
			
			selectBorder = new CompoundBorder(
					new LineBorder(MENU_SELECT_BG, 1),
					new EmptyBorder(s));
			
			defaultBorder = new EmptyBorder(i);
		}
		
		@Override
		public void paint(Graphics g) {
			super.paint(g);
			this.setBorder(isSelected() ? selectBorder : defaultBorder);
		}
	}

}
