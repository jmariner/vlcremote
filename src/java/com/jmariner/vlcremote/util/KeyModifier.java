package com.jmariner.vlcremote.util;

import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.InputEvent.META_DOWN_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;

import lombok.AllArgsConstructor;

@SuppressWarnings("unused")
@AllArgsConstructor
public enum KeyModifier {

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