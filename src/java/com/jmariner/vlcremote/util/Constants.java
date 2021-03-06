package com.jmariner.vlcremote.util;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;

import javax.swing.border.EmptyBorder;

public class Constants {
	public static final Font FONT = Roboto.REGULAR.deriveFont(14f);

	public static final float MAX_TITLE_FONT_SIZE = 28f;
	public static final float MIN_TITLE_FONT_SIZE = 16f;

	public static final double MAX_TITLE_WIDTH = 600.0;

	public static final int MAIN_PADDING = 10;
	public static final int MAIN_WIDTH = 650;
	public static final int MAIN_HEIGHT = 190;

	public static final int PLAYLIST_HEIGHT = 300;
	public static final int MENUBAR_HEIGHT = 25;
	public static final int TOP_HEIGHT = 75;
	public static final int SEPARATOR_HEIGHT = MAIN_PADDING/2;
	public static final int FULL_HEIGHT = MAIN_HEIGHT + PLAYLIST_HEIGHT + SEPARATOR_HEIGHT;
	
	public static final FlowLayout FLOW_CENTER = new FlowLayout(FlowLayout.CENTER, 0, 0);
	public static final BorderLayout BORDER_LAYOUT = new BorderLayout(0, 0);
	public static final EmptyBorder MAIN_PADDING_BORDER = 
			new EmptyBorder(MAIN_PADDING, MAIN_PADDING, MAIN_PADDING, MAIN_PADDING);

	public static final HashMap<AttributedCharacterIterator.Attribute, Object> UNDERLINE = new HashMap<>();

	static {
		UNDERLINE.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
	}
}
