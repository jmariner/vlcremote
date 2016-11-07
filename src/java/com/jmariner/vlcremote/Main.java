package com.jmariner.vlcremote;

import javax.swing.SwingUtilities;

import com.jmariner.vlcremote.gui.RemoteInterface;

public class Main {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			RemoteInterface r = new RemoteInterface();
			r.setVisible(true);
		});
	}

}