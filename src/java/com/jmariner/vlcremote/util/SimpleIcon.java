package com.jmariner.vlcremote.util;

import com.jmariner.vlcremote.RemoteInterface;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.Dimension;

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
	SIZE (true);

	private ImageIcon imageIcon;
	private int ICON_SIZE = 24;
	private boolean isSize;

	SimpleIcon() {
		this(false);
	}

	SimpleIcon(boolean isSize) {
		this.isSize = isSize;
		if (!isSize) {
			String file = String.format("icons/%s.png", name().toLowerCase());
			imageIcon = new ImageIcon(RemoteInterface.class.getResource(file));
			imageIcon.setImage(imageIcon.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH));
		}
	}

	public ImageIcon get() {
		if (isSize)
			return null;
		else
			return imageIcon;
	}

	public Dimension getDim(double scale) {
		if (isSize)
			return  new Dimension((int) (ICON_SIZE * scale), (int) (ICON_SIZE * scale));
		else
			return null;
	}

	public Dimension getDim() {
		return getDim(1);
	}
}