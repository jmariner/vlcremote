package com.jmariner.vlcremote.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import static com.jmariner.vlcremote.util.Constants.*;

public class StatusPanel extends JPanel {

	private JLabel titleLabel;

	public StatusPanel() {
		super(new BorderLayout(0, MAIN_PADDING));

		init();
	}

	private void init() {
		JLabel nowPlayingLabel = new JLabel("Now Playing");
		nowPlayingLabel.setFont(
				FONT.deriveFont(18f).deriveFont(UNDERLINE)
		);

		titleLabel = new JLabel();
		titleLabel.addComponentListener(new TitleResizeListener());

		FlowLayout layout = new FlowLayout(FlowLayout.CENTER, 0, 0);

		JPanel topHalf = new JPanel(layout);
		topHalf.add(nowPlayingLabel);

		JPanel bottomHalf = new JPanel(layout);
		bottomHalf.add(titleLabel);

		this.add(topHalf, BorderLayout.NORTH);
		this.add(bottomHalf, BorderLayout.SOUTH);
		this.setPreferredSize(new Dimension(MAIN_WIDTH, TOP_HEIGHT));
	}

	public void setTitle(String title) {
		titleLabel.setText(title);
		titleLabel.setFont(FONT.deriveFont(MAX_TITLE_FONT_SIZE));
	}

	public String getTitle() {
		return titleLabel.getText();
	}

	private class TitleResizeListener extends ComponentAdapter {
		@Override
		public void componentResized(ComponentEvent e) {
			float curSize = titleLabel.getFont().getSize();

			if (titleLabel.getSize().getWidth() >  MAX_TITLE_WIDTH && curSize >= MIN_TITLE_FONT_SIZE)
				titleLabel.setFont(FONT.deriveFont(curSize - 2));
		}
	}
}
