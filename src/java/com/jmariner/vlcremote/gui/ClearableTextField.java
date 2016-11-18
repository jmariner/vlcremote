package com.jmariner.vlcremote.gui;

import com.jmariner.vlcremote.util.SVGIcon;
import com.jmariner.vlcremote.util.SimpleIcon;
import lombok.Setter;
import org.apache.xmlgraphics.java2d.color.ColorUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.PlainDocument;

import java.awt.*;
import java.awt.event.*;
import java.util.EventObject;

public class ClearableTextField extends JTextField {

	private SVGIcon clearIcon;

	private Insets defaultInsets;
	private Border normalBorder;

	@Setter
	private Component focusReceiver;

	@Setter
	private Runnable onClear;

	private int mouseX, iconHorizontalThreshold;
	private boolean isHovering, hasText;

	@Setter
	private Runnable changeAction;

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

		this.clearIcon = SimpleIcon.CLEAR.get();
		this.focusReceiver = null;
		this.onClear = null;

		this.isHovering = false;

		this.normalBorder = DUMMY.getBorder();
		this.defaultInsets = normalBorder.getBorderInsets(DUMMY);

		this.addPropertyChangeListener("foreground", this::updateIcon);
		this.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) { onResize(); }
		});

		this.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) { isHovering = true; }
		});

		MainListener listener = new MainListener();
		this.addMouseMotionListener(listener);
		this.addMouseListener(listener);
		((PlainDocument) this.getDocument()).addDocumentListener(listener);
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
		Color color = mouseX > iconHorizontalThreshold ? 
				ColorUtil.lightenColor(getForeground(), -.25f) :
				getForeground();
				
		this.clearIcon.resizeAndRecolor(size, color);
		repaintIcon();
	}

	private Point getIconPos() {
		return new Point(getWidth() - clearIcon.getIconWidth() - defaultInsets.right, defaultInsets.top);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (showClearButton()) {
			Point p = getIconPos();
			clearIcon.paintIcon(this, g, p.x, p.y);
		}
	}

	private void repaintIcon() {
		repaint(iconHorizontalThreshold, 0, getWidth()-iconHorizontalThreshold, getHeight());
	}

	private void searchChanged() {
		hasText = !getText().equals("");
		updateCursor();
		repaintIcon();
		if (changeAction != null)
			changeAction.run();
	}
	
	private void updateCursor() {
		setCursor(showClearButton() && mouseX > iconHorizontalThreshold 
				? BUTTON_CURSOR : NORMAL_CURSOR);
	}

	public void clear() {
		setText("");
		if (focusReceiver != null)
			focusReceiver.requestFocusInWindow();
		if (onClear != null)
			onClear.run();
	}
	
	private boolean showClearButton() {
		return isHovering && hasText && clearIcon != null;
	}

	private class MainListener extends MouseAdapter implements DocumentListener {
		@Override
		public void mouseMoved(MouseEvent e) {
			mouseX = e.getPoint().x;
			updateCursor();
			updateIcon(null);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			updateIcon(null);
			isHovering = false;
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			isHovering = true;
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
			searchChanged();
		}
	}

}
