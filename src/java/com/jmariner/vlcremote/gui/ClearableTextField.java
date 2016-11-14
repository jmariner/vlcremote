package com.jmariner.vlcremote.gui;

import com.jmariner.vlcremote.util.SVGIcon;
import com.jmariner.vlcremote.util.SimpleIcon;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.xmlgraphics.java2d.color.ColorUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.EventObject;

@Slf4j
public class ClearableTextField extends JTextField {

	private SVGIcon clearIcon;

	private Insets defaultInsets;
	private Border normalBorder;

	@Setter
	private Component focusReceiver;

	@Setter
	private Runnable onClear;

	private int iconHorizontalThreshold;
	private boolean showClearButton;

	private static final JTextField DUMMY = new JTextField();
	private static final Cursor BUTTON_CURSOR = new Cursor(Cursor.HAND_CURSOR);
	private static final Cursor NORMAL_CURSOR = DUMMY.getCursor();

	public ClearableTextField() {
		this(null, 0);
	}

	public ClearableTextField(int columns) {
		this(null, columns);
	}

	public ClearableTextField(String text) {
		this(text, 0);
	}

	public ClearableTextField(String text, int columns) {
		super(text, columns);

		this.clearIcon = null;
		this.focusReceiver = null;
		this.onClear = null;

		this.showClearButton = false;

		this.normalBorder = DUMMY.getBorder();
		this.defaultInsets = normalBorder.getBorderInsets(DUMMY);

		this.addPropertyChangeListener("foreground", this::updateIcon);
		this.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) { onResize(); }
		});

		this.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) { showClearButton = true; }
		});

		MainListener mouseListener = new MainListener();
		this.addMouseMotionListener(mouseListener);
		this.addMouseListener(mouseListener);
	}

	private void onResize() {
		updateIcon(null);
		iconHorizontalThreshold = getWidth() - (clearIcon.getIconWidth() + defaultInsets.left);

		int right = getWidth() - getIconPos().x + defaultInsets.right;
		setBorder(new EmptyBorder(
				defaultInsets.top, defaultInsets.left, defaultInsets.bottom, right));
	}

	private void updateIcon(EventObject e) {
		int size = getHeight() - defaultInsets.bottom - defaultInsets.top;
		this.clearIcon = SimpleIcon.CLEAR.get(size, getForeground());
		repaintIcon();
	}

	private Point getIconPos() {
		return new Point(getWidth() - clearIcon.getIconWidth() - defaultInsets.right, defaultInsets.top);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (clearIcon != null && showClearButton && !getText().equals("")) {
			Point p = getIconPos();
			clearIcon.paintIcon(this, g, p.x, p.y);
		}
	}

	private void repaintIcon() {
		repaint(iconHorizontalThreshold, 0, getWidth()-iconHorizontalThreshold, getHeight());
	}

	public void clear() {
		setText("");
		if (focusReceiver != null)
			focusReceiver.requestFocusInWindow();
		if (onClear != null)
			onClear.run();
	}

	private class MainListener extends MouseAdapter implements DocumentListener {
		@Override
		public void mouseMoved(MouseEvent e) {
			Point p = e.getPoint();
			if (p.x > iconHorizontalThreshold) {
				setCursor(BUTTON_CURSOR);

				Color darker = ColorUtil.lightenColor(getForeground(), -.25f);
				clearIcon = clearIcon.recolor(darker);
				repaintIcon();
			}
			else {
				setCursor(NORMAL_CURSOR);

				updateIcon(null);
			}
		}

		@Override
		public void mouseExited(MouseEvent e) {
			updateIcon(null);
			showClearButton = false;
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			showClearButton = true;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getPoint().x > iconHorizontalThreshold) {
				clear();
			}
		}

		@Override public void insertUpdate(DocumentEvent e) { changedUpdate(e); }

		@Override public void removeUpdate(DocumentEvent e) { changedUpdate(e); }

		@Override
		public void changedUpdate(DocumentEvent e) {
			repaintIcon();
		}
	}

}
