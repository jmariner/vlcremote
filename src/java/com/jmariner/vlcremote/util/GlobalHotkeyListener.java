package com.jmariner.vlcremote.util;

import com.tulskiy.keymaster.common.MediaKey;
import com.tulskiy.keymaster.common.Provider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;

import static java.awt.event.InputEvent.*;

@Slf4j
public class GlobalHotkeyListener {

	private Provider hotkeyProvider;
	private Provider swingProvider;

	public GlobalHotkeyListener() {

		hotkeyProvider = Provider.getCurrentProvider(false);
		swingProvider = Provider.getCurrentProvider(true);

	}

	//----------------------register by key code and Mod modifier-----------------------------

	@SuppressWarnings("MagicConstant")
	public void registerHotkey(int keyCode, Mod modifiers, Runnable action) {
		register(KeyStroke.getKeyStroke(keyCode, modifiers.get()), false, action);
	}

	@SuppressWarnings("MagicConstant")
	public void registerSwingHotkey(int keyCode, Mod modifiers, Runnable action) {
		register(KeyStroke.getKeyStroke(keyCode, modifiers.get()), true, action);
	}

	//----------------------register by key stroke string-------------------------------------

	public void registerHotkey(String keyStroke, Runnable action) throws Exception {
		registerFromString(keyStroke, false, action);
	}

	public void registerSwingHotkey(String keyStroke, Runnable action) throws Exception {
		registerFromString(keyStroke, true, action);
	}

	// TODO create an InvalidHotkeyException for this and its accessors to throw
	private void registerFromString(String keyStroke, boolean isSwing, Runnable action) throws Exception {
		KeyStroke k = KeyStroke.getKeyStroke(keyStroke);

		if (k == null) {
			int last = keyStroke.lastIndexOf(" ");
			keyStroke = keyStroke.substring(0, last) + keyStroke.substring(last).toUpperCase();
			k = KeyStroke.getKeyStroke(keyStroke);
			if (k == null) throw new Exception("Invaild keystoke syntax: \"" + keyStroke + "\"");
		}

		register(k, isSwing, action);
	}

	//----------------------------register media keys-----------------------------------------

	public void registerHotkey(MediaKey k, Runnable action) {
		register(k, false, action);
	}

	public void registerSwingHotkey(MediaKey k, Runnable action) {
		register(k, true, action);
	}

	//------------------------primary methods-------------------------------------------------

	private void register(KeyStroke k, boolean swing, Runnable action) {
		if (swing) {
			hotkeyProvider.register(k, h -> action.run());
		}
		else {
			swingProvider.register(k, h -> action.run());
		}
	}

	private void register(MediaKey k, boolean swing, Runnable action) {
		if (swing) {
			hotkeyProvider.register(k, h -> action.run());
		}
		else {
			swingProvider.register(k, h -> action.run());
		}
	}

	//----------------------------------------------------------------------------------------

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
