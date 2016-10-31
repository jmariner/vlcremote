package com.jmariner.vlcremote.util;

import org.apache.commons.lang3.StringEscapeUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static com.jmariner.vlcremote.util.Constants.*;

public class GuiUtils {

	public static void debugBorderComponents(Container con, boolean show) {
		Stream.of(con.getComponents())
				.filter(c -> c instanceof JComponent)
				.map(c -> (JComponent)c)
				.forEach(c -> {
					if (show) {
						Border red = BorderFactory.createLineBorder(Color.red);
						Border old = c.getBorder();
						if (old == null)
							c.setBorder(red);
						else
							c.setBorder(new CompoundBorder(red, old));
					}
					else {
						if (c.getBorder() instanceof CompoundBorder)
							c.setBorder(((CompoundBorder) c.getBorder()).getInsideBorder());
						else
							c.setBorder(null);
					}
					if (c instanceof JScrollPane) return;
					debugBorderComponents(c, show);
				});
	}

	public static String restrictDialogWidth(String text, boolean escape) {
		text = escape ? StringEscapeUtils.escapeHtml4(text) : text;
		return String.format("<html><body><p style='width: %dpx'>%s</p></body></html>", MAIN_WIDTH /2, text);
	}

	public static String restrictDialogWidth(String text) {
		return restrictDialogWidth(text, false);
	}

	public static String formatTime(int seconds) {
		return DateTimeFormatter.ofPattern(seconds < 3600 ? "mm:ss" : "HH:mm:ss")
				.withZone(ZoneId.of("UTC"))
				.format(Instant.ofEpochMilli((long)(seconds*1000)));
	}

}