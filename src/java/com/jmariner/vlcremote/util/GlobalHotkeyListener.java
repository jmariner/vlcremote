package com.jmariner.vlcremote.util;

import com.tulskiy.keymaster.common.MediaKey;
import com.tulskiy.keymaster.common.Provider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;

import static java.awt.event.InputEvent.*;

@SuppressWarnings("unused")
@Slf4j
public class GlobalHotkeyListener {

	private Provider hotkeyProvider;

	public GlobalHotkeyListener() {
		this(false);
	}

	private GlobalHotkeyListener(boolean swing) {
		hotkeyProvider = Provider.getCurrentProvider(swing);
	}

	public static GlobalHotkeyListener getSwingInstance() {
		return new GlobalHotkeyListener(true);
	}

	//----------------------register by key code and Mod modifier----------------------------

	@SuppressWarnings("MagicConstant")
	public void registerHotkey(int keyCode, Mod modifiers, Runnable action) {
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

	@SuppressWarnings("unused")
	@AllArgsConstructor
	public enum Mod {

		NONE(0),

		ALT				(ALT_DOWN_MASK),
		ALT_SHIFT		(ALT_DOWN_MASK | SHIFT_DOWN_MASK),
		ALT_META		(ALT_DOWN_MASK | META_DOWN_MASK),

		CONTROL			(CTRL_DOWN_MASK),
		CONTROL_SHIFT	(CTRL_DOWN_MASK | SHIFT_DOWN_MASK),
		CONTROL_ALT		(CTRL_DOWN_MASK | ALT_DOWN_MASK),
		CONTROL_META	(CTRL_DOWN_MASK | META_DOWN_MASK),

		SHIFT			(SHIFT_DOWN_MASK),
		SHIFT_META		(SHIFT_DOWN_MASK | META_DOWN_MASK),

		META				(META_DOWN_MASK),

		CONTROL_ALT_SHIFT	(CTRL_DOWN_MASK | ALT_DOWN_MASK | SHIFT_DOWN_MASK),
		CONTROL_ALT_META	(CTRL_DOWN_MASK | ALT_DOWN_MASK | META_DOWN_MASK),
		CONTROL_SHIFT_META	(CTRL_DOWN_MASK | SHIFT_DOWN_MASK | META_DOWN_MASK),
		ALT_SHIFT_META		(ALT_DOWN_MASK | SHIFT_DOWN_MASK | META_DOWN_MASK),

		CONTROL_ALT_SHIFT_META(CTRL_DOWN_MASK | ALT_DOWN_MASK | SHIFT_DOWN_MASK | META_DOWN_MASK);

		private int id;

		public int get() {
			return id;
		}
	}

}
