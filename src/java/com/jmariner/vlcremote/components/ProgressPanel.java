package com.jmariner.vlcremote.components;

import com.jmariner.vlcremote.MyVLCRemote;
import com.jmariner.vlcremote.RemoteInterface;
import com.jmariner.vlcremote.util.GuiUtils;
import com.jmariner.vlcremote.util.VLCStatus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.jmariner.vlcremote.util.Constants.MAIN_PADDING;

public class ProgressPanel extends JPanel {

	private RemoteInterface gui;

	private JLabel positionLabel, lengthLabel;
	private JProgressBar progressBar;

	private static final String ZERO_TIME = "00:00:00";
	private static final Dimension TIME_LABEL_SIZE = new Dimension(66, 25);

	public ProgressPanel(RemoteInterface gui) {
		super(new BorderLayout(MAIN_PADDING, 0), true);

		this.gui = gui;

		init();
		progressBar.addMouseListener(new ProgressBarMouseListener());
		gui.getControlComponents().add(progressBar);
	}

	private void init() {
		positionLabel = new JLabel(ZERO_TIME);
		lengthLabel = new JLabel(ZERO_TIME);
		progressBar = new JProgressBar(0, (int) Math.pow(10, 6));

		positionLabel.setPreferredSize(TIME_LABEL_SIZE);
		lengthLabel.setPreferredSize(TIME_LABEL_SIZE);
		positionLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lengthLabel.setHorizontalAlignment(SwingConstants.CENTER);

		this.add(positionLabel, BorderLayout.WEST);
		this.add(progressBar, BorderLayout.CENTER);
		this.add(lengthLabel, BorderLayout.EAST);
	}

	public void update(VLCStatus status) {
		int currentTime = status.getTime();
		double positionPercent = status.getPosition();

		positionLabel.setText(GuiUtils.formatTime(currentTime));
		progressBar.setValue((int) (positionPercent * progressBar.getMaximum()));
	}

	public void updateLength(VLCStatus status) {
		int length = status.getLength();
		lengthLabel.setText(GuiUtils.formatTime(length));
	}

	private class ProgressBarMouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (progressBar.isEnabled()) {
				double percent = e.getPoint().x / ((double) progressBar.getWidth());
				int newVal = (int) (progressBar.getMinimum() + ((progressBar.getMaximum() - progressBar.getMinimum()) * percent));
				progressBar.setValue(newVal);
				VLCStatus status = gui.getRemote()
						.sendCommand(MyVLCRemote.Command.SEEK_TO, (percent * 100) + "%");
				gui.updateInterface(status);
			}
		}
	}
}
