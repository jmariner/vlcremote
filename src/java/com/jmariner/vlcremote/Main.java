package com.jmariner.vlcremote;

import lombok.extern.slf4j.Slf4j;

import java.util.Scanner;

import javax.swing.SwingUtilities;

import com.jmariner.vlcremote.gui.RemoteInterface;

import static com.jmariner.vlcremote.MyVLCRemote.Command.*;

@Slf4j
public class Main {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			RemoteInterface r = new RemoteInterface();
			r.setVisible(true);
		});
	}

}