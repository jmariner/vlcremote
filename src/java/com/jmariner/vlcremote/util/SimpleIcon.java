package com.jmariner.vlcremote.util;

import java.awt.*;
import org.w3c.dom.Document;

public enum SimpleIcon {
	NEXT,
	PREV,
	PLAY,
	PAUSE,
	REPEAT,
	LOOP,
	SHUFFLE,
	VOLUME_HIGH,
	VOLUME_LOW,
	VOLUME_NONE,
	VOLUME_OFF,
	PLAYLIST,
	FAVORITE,
	FAVORITE_EMPTY,
	CLEAR,
	PLAY_OUTLINE,
	JUMP_TO,
	CLEAR_FILTER;

	private Document iconDoc;

	public static class Defaults {
		public static final double BUTTON_ICON_RATIO = 1.25;
		public static final int SIZE = 24;
		public static final int BUTTON_SIZE = (int) (SIZE * BUTTON_ICON_RATIO);
		public static final Color COLOR = Color.BLACK;
		public static final Color SELECTED_COLOR = Color.WHITE;
	}

	SimpleIcon() {
		this.iconDoc = SVGIcon.getDocument(name().toLowerCase());
	}

	public SVGIcon get() {
		return new SVGIcon(iconDoc, Defaults.SIZE, Defaults.COLOR);
	}

	public SVGIcon get(double sizeScale) {
		return get(sizeScale, Defaults.COLOR);
	}

	public SVGIcon get(int newSize) {
		return get(newSize, Defaults.COLOR);
	}

	public SVGIcon get(Color color) {
		return get(Defaults.SIZE, color);
	}

	public SVGIcon get(double sizeScale, Color color) {
		int size = (int) (sizeScale * Defaults.SIZE);
		return get(size, color);
	}

	public SVGIcon get(int newSize, Color color) {
		return new SVGIcon(iconDoc, newSize, color);
	}
}