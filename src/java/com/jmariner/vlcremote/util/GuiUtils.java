package com.jmariner.vlcremote.util;

import org.apache.commons.lang3.StringEscapeUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.jmariner.vlcremote.util.Constants.FLOW_CENTER;
import static com.jmariner.vlcremote.util.Constants.MAIN_WIDTH;

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
				.format(Instant.ofEpochMilli(seconds*1000));
	}

	public static Dimension squareDim(int size) {
		return new Dimension(size, size);
	}
	
	public static JPanel horizontalGridOf(JComponent... comps) {
		JPanel panel = new JPanel(new GridLayout(1, comps.length));
		Arrays.asList(comps).stream().forEachOrdered(c -> {
			JPanel p = new JPanel(FLOW_CENTER);
			p.add(c);
			panel.add(p);
		});
		return panel;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Component> List<T> getComponents(Container c, Class<T> cls) {
		List<T> out = new ArrayList<>();
		Arrays.asList(c.getComponents()).forEach(com -> {
			if (cls.isInstance(com))
				out.add((T) com);
			if (com instanceof Container)
				out.addAll(getComponents((Container) com, cls));
		});
		return out;
	}
	
	public static JPanel flowLayout(JComponent c, int align) {
		JPanel out = new JPanel(new FlowLayout(align, 0, 0));
		out.add(c);
		return out;
	}

}
