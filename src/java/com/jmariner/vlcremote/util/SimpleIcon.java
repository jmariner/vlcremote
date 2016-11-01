package com.jmariner.vlcremote.util;

import com.jmariner.vlcremote.Main;
import com.jmariner.vlcremote.gui.RemoteInterface;

import javax.swing.*;
import java.awt.*;

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
	PLAYLIST;

	private ImageIcon imageIcon;
	public static final int ICON_SIZE = 24;
	private boolean isSize;


	SimpleIcon() {
		String file = String.format("icons/%s.png", name().toLowerCase());
		imageIcon = new ImageIcon(Main.class.getResource(file));
		imageIcon.setImage(imageIcon.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH));
	}

	public ImageIcon get() {
		return imageIcon;
	}
}