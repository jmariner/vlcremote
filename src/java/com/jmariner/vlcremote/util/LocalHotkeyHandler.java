package com.jmariner.vlcremote.util;

import com.jmariner.vlcremote.gui.RemoteInterface;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class LocalHotkeyHandler {
	
	private JPanel contentPane;

	private List<String> actionIDs;
	
	public LocalHotkeyHandler(RemoteInterface gui) {
		contentPane = (JPanel) gui.getContentPane();
		actionIDs = new ArrayList<>();

		gui.getActions().forEach((id, a) -> contentPane.getActionMap().put(id, runnableToAction(a)));
	}
	
	private Action runnableToAction(Runnable a) {
		return new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				a.run();
			}
		};
	}

	//----------------------register by key code and KeyModifier modifier----------------------------

	@SuppressWarnings("MagicConstant")
	public void registerHotkey(int keyCode, KeyModifier modifiers, String actionID) {
		register(KeyStroke.getKeyStroke(keyCode, modifiers.get()), actionID);
	}

	//----------------------register by key stroke string------------------------------------

	/**
	 * @see KeyStroke#getKeyStroke(String)
	 */
	public void registerHotkey(String keyStroke, String actionID)
			throws InvalidHotkeyStringException {

		KeyStroke k = KeyStroke.getKeyStroke(keyStroke);

		//hotkey string was invalid
		if (k == null) {
			//try to capitalize the last token
			int last = keyStroke.lastIndexOf(" ");
			
			if (last == -1) throw new InvalidHotkeyStringException(
					"Invalid hotkey syntax: \"" + keyStroke + "\"");
			
			keyStroke = keyStroke.substring(0, last) + keyStroke.substring(last).toUpperCase();
			k = KeyStroke.getKeyStroke(keyStroke);
			//still didn't work? give up
			if (k == null) throw new InvalidHotkeyStringException(
					"Invalid hotkey syntax: \"" + keyStroke + "\"");
		}

		register(k, actionID);
	}
	
	//------------------------primary method-------------------------------------------------

	private void register(KeyStroke k, String actionID) {
		actionIDs.add(actionID);
		contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(k, actionID);
	}
	
	//--------------------------other--------------------------------------------------------
	
	public void clear() {
		InputMap i = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		i.clear();
	}

	public void setEnabled(boolean enabled) {
		actionIDs.forEach(s -> contentPane.getActionMap().get(s).setEnabled(enabled));
	}
}
