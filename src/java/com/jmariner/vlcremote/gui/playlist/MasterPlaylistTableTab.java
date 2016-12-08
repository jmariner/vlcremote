package com.jmariner.vlcremote.gui.playlist;

import java.awt.BorderLayout;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.JXTable;

import com.jmariner.vlcremote.SongItem;
import com.jmariner.vlcremote.gui.RemoteInterface;
import com.jmariner.vlcremote.util.GuiUtils;

import lombok.Getter;

import static com.jmariner.vlcremote.util.Constants.*;

public class MasterPlaylistTableTab extends JPanel {
	
	private RemoteInterface gui;
	
	private JXTable table;
	
	public MasterPlaylistTableTab(RemoteInterface gui) {
		this.gui = gui;
		
		init();
	}
	
	private void init() {
		
		table = new JXTable();
		
		JScrollPane scrollPane = new JScrollPane(
				table,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
		);
		
		JLabel title = new JLabel("Full Song List");
		title.setFont(FONT.deriveFont(UNDERLINE).deriveFont(24f));
		title.setHorizontalAlignment(SwingConstants.CENTER);
		
		JPanel topPanel = new JPanel(FLOW_CENTER);
		topPanel.add(title);
		
		this.setLayout(new BorderLayout(0, MAIN_PADDING));
		this.add(topPanel, BorderLayout.NORTH);
		this.add(scrollPane, BorderLayout.CENTER);
	}
	
	public void initPost() {
		SongTableModel model = new SongTableModel();
		model.update();
		table.setModel(model);
		table.packAll();
		
		for (int c=0; c < table.getColumnCount(); c++) {
			int w = Heading.valueOf(table.getColumnName(c).toUpperCase()).getMinWidth();
			if (w > -1)
				table.getColumnModel().getColumn(c).setMinWidth(w);
		}
		
	}
	
	private class SongTableModel extends AbstractTableModel {
		
		private List<SongItem> songs;
		
		protected void update() {
			// TODO load ALL songs from ALL albums and parse into the hashmap
			
			songs = gui.getRemote().getStatus() 
					.getSongMap().values().stream() // load current album only for now
					.sorted((s1, s2) -> s1.toString().compareTo(s2.toString()))
					.collect(Collectors.toList());
		}

		@Override
		public int getRowCount() {
			return songs.size();
		}

		@Override
		public int getColumnCount() {
			return Heading.values().length;
		}

		@Override
		public String getColumnName(int columnIndex) {
			Heading h = Heading.get(columnIndex);
			return h == null ? null : h.getName();
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) { return String.class; }

		@Override
		public boolean isCellEditable(int r, int c) { return false; }

		@Override
		public String getValueAt(int r, int c) {
			SongItem s = songs.get(r);
			switch (Heading.get(c)) {
				case ARTIST:
					String a = s.getArtist();
					return a.equals("") ? "N/A" : a;
				case TITLE:
					return s.getTitle();
				case ALBUM:
					return s.getAlbum();
				case DURATION:
					return GuiUtils.formatTime(s.getDuration());
				default:
					return "";
			}
		}

		@Override
		public void setValueAt(Object v, int r, int c) {}
		
	}
	
	@Getter
	private enum Heading {
		ARTIST	(-1),
		TITLE	(-1),
		ALBUM	(100),
		DURATION(65);
		
		private String name;
		private int minWidth;
		
		Heading(int minWidth) {
			this.name = StringUtils.capitalize(name().toLowerCase());
			this.minWidth = minWidth;
		}
		
		public static Heading get(int index) {
			if (index > -1 && index < values().length) {
				return values()[index];
			}
			return null;
		}
	}

}
