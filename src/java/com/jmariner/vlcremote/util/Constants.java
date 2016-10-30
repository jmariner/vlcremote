package com.jmariner.vlcremote.util;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;

public class Constants {
	public static final Font FONT = Roboto.REGULAR.deriveFont(14f);
	public static final float MAX_TITLE_FONT_SIZE = 28f;
	public static final float MIN_TITLE_FONT_SIZE = 16f;
	public static final double MAX_TITLE_WIDTH = 600.0;
	public static final int MAIN_PADDING = 10;
	public static final int MAIN_WIDTH = 650;
	public static final int MAIN_HEIGHT = 225;
	public static final int PLAYLIST_HEIGHT = 300;
	public static final int MENUBAR_HEIGHT = 25;
	public static final int MAX_HEIGHT = MAIN_HEIGHT + PLAYLIST_HEIGHT + MENUBAR_HEIGHT;
	public static final HashMap<AttributedCharacterIterator.Attribute, Object> UNDERLINE;

	static {
		UNDERLINE = new HashMap<>();
		UNDERLINE.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
	}
}
