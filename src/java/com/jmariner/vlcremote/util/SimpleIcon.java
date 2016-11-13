package com.jmariner.vlcremote.util;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;

@SuppressWarnings("unused")
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
	CLEAR;

	private SVGIcon icon;

	public static class Defaults {
		public static final int SIZE = 24;
		public static final Color COLOR = Color.BLACK;
	}

	SimpleIcon() {
		try {
			icon = new SVGIcon(
					name().toLowerCase(),
					Defaults.SIZE,
					Defaults.COLOR
			);
		} catch (URISyntaxException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public SVGIcon get() {
		return icon;
	}

	public SVGIcon get(double sizeScale) {
		int size = (int) (sizeScale * Defaults.SIZE);
		return icon.resize(size);
	}

	public SVGIcon get(int newSize) {
		return icon.resize(newSize);
	}

	public SVGIcon get(Color color) {
		return icon.recolor(color);
	}

	public SVGIcon get(double sizeScale, Color color) {
		int size = (int) (sizeScale * Defaults.SIZE);
		return icon.resizeAndRecolor(size, color);
	}

	public SVGIcon get(int newSize, Color color) {
		return icon.resizeAndRecolor(newSize, color);
	}
}