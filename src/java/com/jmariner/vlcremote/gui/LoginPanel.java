package com.jmariner.vlcremote.gui;

import com.jmariner.vlcremote.util.GuiUtils;
import com.jmariner.vlcremote.util.UserSettings;
import lombok.AccessLevel;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.jmariner.vlcremote.util.Constants.*;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;

public class LoginPanel extends JPanel {

	private RemoteInterface gui;

	private JTextField hostField, webPortField, streamPortField;
	private JPasswordField passwordField;
	private JButton connectButton;

	private List<JTextField> textFields = new ArrayList<>();

	protected LoginPanel(RemoteInterface gui) {
		super(new BorderLayout(0, MAIN_PADDING));

		this.gui = gui;
		init();
		initListeners();
	}

	private void init() {
		hostField = new JTextField();
		webPortField = new JFormattedTextField(NumberFormat.getNumberInstance());
		streamPortField = new JFormattedTextField(NumberFormat.getNumberInstance());
		passwordField = new JPasswordField();

		hostField.setColumns(20);
		webPortField.setColumns(5);
		streamPortField.setColumns(5);
		passwordField.setColumns(20);

		connectButton = new JButton("Connect");

		textFields = Arrays.asList(hostField, webPortField, streamPortField, passwordField);

		FlowLayout layout = new FlowLayout(FlowLayout.CENTER, MAIN_PADDING, 0);

		JPanel topHalf = new JPanel(layout);
		topHalf.add(new JLabel("Host:"));
		topHalf.add(hostField);
		topHalf.add(new JLabel("Password:"));
		topHalf.add(passwordField);

		JPanel bottomHalf = new JPanel(layout);
		bottomHalf.add(new JLabel("Web Port:"));
		bottomHalf.add(webPortField);
		bottomHalf.add(new JLabel("Stream Port:"));
		bottomHalf.add(streamPortField);
		bottomHalf.add(connectButton);

		this.setBorder(new EmptyBorder(MAIN_PADDING, 0, MAIN_PADDING, 0));
		this.setPreferredSize(new Dimension(MAIN_WIDTH, TOP_HEIGHT));
		this.add(topHalf, BorderLayout.NORTH);
		this.add(bottomHalf, BorderLayout.SOUTH);

	}

	private void initListeners() {
		textFields.forEach(f -> f.addActionListener(this::connectPressed));
		connectButton.addActionListener(this::connectPressed);
	}

	protected void connectPressed(AWTEvent e) {
		gui.connect();
	}

	protected String getPassword() {
		return String.valueOf(passwordField.getPassword());
	}

	protected String getHost() {
		return hostField.getText();
	}

	protected int getHttpPort() {
		return Integer.parseInt(webPortField.getText());
	}

	protected int getStreamPort() {
		return Integer.parseInt(streamPortField.getText());
	}

	protected void saveConnectionInfo() {
		UserSettings.put("httpHost", hostField.getText());
		UserSettings.put("httpPort", webPortField.getText());
		UserSettings.put("streamPort", streamPortField.getText());

		if (!UserSettings.keyExists("httpPass") && UserSettings.getBoolean("saveHttpPass", true)) {

			boolean savePass = JOptionPane.showConfirmDialog(gui,
					GuiUtils.restrictDialogWidth("Save password in preferences?<br>WARNING: This saves in plain text."),
					"Save Password",
					YES_NO_OPTION,
					QUESTION_MESSAGE
			) == 0;

			UserSettings.putBoolean("saveHttpPass", savePass);

			if (savePass)
				UserSettings.put("httpPass", String.valueOf(passwordField.getPassword()));
		}

		if (UserSettings.keyExists("httpPass") && !UserSettings.keyExists("autoconnect")) {
			boolean autoconnect = JOptionPane.showConfirmDialog(gui,
					"Auto connect from startup next time?",
					"Auto Connect",
					YES_NO_OPTION,
					QUESTION_MESSAGE
			) == 0;

			UserSettings.putBoolean("autoconnect", autoconnect);
		}
	}

	protected void loadSettings() {
		hostField.setText(UserSettings.get("httpHost", ""));
		webPortField.setText(UserSettings.get("httpPort", ""));
		streamPortField.setText(UserSettings.get("streamPort", ""));
		passwordField.setText(UserSettings.get("httpPass", ""));
	}

}
