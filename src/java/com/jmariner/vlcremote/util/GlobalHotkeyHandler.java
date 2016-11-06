package com.jmariner.vlcremote.util;

import com.tulskiy.keymaster.common.MediaKey;
import com.tulskiy.keymaster.common.Provider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;

import static java.awt.event.InputEvent.*;

@SuppressWarnings("unused")
@Slf4j
public class GlobalHotkeyHandler {

	private Provider hotkeyProvider;

	public GlobalHotkeyHandler() {
		this(false);
	}

	private GlobalHotkeyHandler(boolean swing) {
		hotkeyProvider = Provider.getCurrentProvider(swing);
	}

	public static GlobalHotkeyHandler getSwingInstance() {
		return new GlobalHotkeyHandler(true);
	}

	//----------------------register by key code and KeyModifier modifier----------------------------

	@SuppressWarnings("MagicConstant")
	public void registerHotkey(int keyCode, KeyModifier modifiers, Runnable action) {
		register(KeyStroke.getKeyStroke(keyCode, modifiers.get()), action);
	}

	//----------------------register by key stroke string------------------------------------

	/**
	 * @see KeyStroke#getKeyStroke(String)
	 */
	public void registerHotkey(String keyStroke, Runnable action)
			throws InvalidHotkeyStringException {

		KeyStroke k = KeyStroke.getKeyStroke(keyStroke);

		//hotkey string was invalid
		if (k == null) {
			//try to capitalize the last token
			int last = keyStroke.lastIndexOf(" ");
			
			if (last == -1) throw new InvalidHotkeyStringException("Invalid hotkey syntax: \"" + keyStroke + "\"");
			
			keyStroke = keyStroke.substring(0, last) + keyStroke.substring(last).toUpperCase();
			k = KeyStroke.getKeyStroke(keyStroke);
			//still didn't work? give up
			if (k == null) throw new InvalidHotkeyStringException("Invalid hotkey syntax: \"" + keyStroke + "\"");
		}

		register(k, action);
	}

	//----------------------------register media key-----------------------------------------

	public void registerHotkey(MediaKey k, Runnable action) {
		register(k, action);
	}

	//------------------------primary method-------------------------------------------------

	private void register(Object k, Runnable action) {

		assert k instanceof MediaKey || k instanceof KeyStroke;

		if (k instanceof MediaKey)
			hotkeyProvider.register((MediaKey) k, h -> action.run());
		if (k instanceof KeyStroke)
			hotkeyProvider.register((KeyStroke) k, h -> action.run());
	}

	//-------------------------------other---------------------------------------------------

	public void clear() {
		hotkeyProvider.reset();
	}

	public void cleanup() {
		hotkeyProvider.stop();
	}

	//---------------------------------------------------------------------------------------

}
