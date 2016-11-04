package com.jmariner.vlcremote.gui;

import com.jmariner.vlcremote.SongItem;
import com.jmariner.vlcremote.util.VLCStatus;
import org.jdesktop.swingx.JXList;
import org.jdesktop.swingx.sort.ListSortController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

import static com.jmariner.vlcremote.util.Constants.*;

public class PlaylistPanel extends JPanel {

	private RemoteInterface gui;

	private JXList list;
	private JTextField searchField;
	private JButton playSelected;

	private ListSortController<ListModel<SongItem>> sorter;

	protected PlaylistPanel(RemoteInterface gui) {
		super(new BorderLayout(0, 10));
		this.gui = gui;
	}

	public void init() {
		Map<Integer, SongItem> songMap = gui.getRemote().getSongMap();

		DefaultListModel<SongItem> playlist = new DefaultListModel<>();
		songMap.values().stream().forEachOrdered(playlist::addElement);

		list = new PlaylistList(playlist);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		sorter = new ListSortController<>(playlist);
		list.setRowSorter(sorter);

		JScrollPane scrollPane = new JScrollPane(
				list,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
		);

		JLabel title = new JLabel("Playlist");
		title.setFont(FONT.deriveFont(18f).deriveFont(UNDERLINE));
		title.setHorizontalAlignment(SwingConstants.CENTER);

		//*
		JButton clearSearchButton = new JButton("✖");
		clearSearchButton.setFont(new Font("Dingbats", 0, FONT.getSize()));
		clearSearchButton.setToolTipText("Clear the search");
		clearSearchButton.setForeground(Color.GRAY);
		clearSearchButton.setBackground(this.getBackground());
		clearSearchButton.setBorder(BorderFactory.createEmptyBorder());//*/

		searchField = new JTextField(20);
		JPanel playlistSearch = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
		playlistSearch.add(new JLabel("Search"));
		playlistSearch.add(searchField);
		//playlistSearch.add(clearSearchButton);

		JPanel playlistTop = new JPanel(new BorderLayout());
		playlistTop.add(title, BorderLayout.WEST);
		playlistTop.add(playlistSearch, BorderLayout.EAST);

		playSelected = new JButton("Play Selected");

		this.setBorder(new EmptyBorder(10, 10, 10, 10));
		this.setPreferredSize(new Dimension(MAIN_WIDTH, PLAYLIST_HEIGHT));
		this.add(playlistTop, BorderLayout.NORTH);
		this.add(scrollPane, BorderLayout.CENTER);
		this.add(playSelected, BorderLayout.SOUTH);

		gui.setPlaylistAreaShowing(false);

		initListeners();
	}

	private void initListeners() {
		playSelected.addActionListener(this::switchSongToSelected);
		list.addMouseListener(new PlaylistMouseListener());
		searchField.getDocument().addDocumentListener(new PlaylistSearchListener());
	}

	private void switchSongToSelected(AWTEvent e) {
		int index = ((SongItem)list.getSelectedValue()).getId();
		searchField.setText("");
		gui.getRemote().switchSong(index);
		gui.updateInterface(gui.getRemote().getStatus());
	}

	protected void update(VLCStatus status) {
		list.setSelectedIndex( // TODO setSelectedValue doesn't work here for some reason
				gui.getRemote().transformPlaylistID(status.getCurrentID())
		);
		int selected = list.getSelectedIndex();
		int size = list.getModel().getSize();
		int min = selected < 5 ? 0 : selected-5;
		int max = selected > size-6 ? size-1 : selected+5;
		Rectangle r = list.getCellBounds(min, max);
		list.scrollRectToVisible(r);
	}

	public void startSearch() {
		if (!gui.isPlaylistAreaShowing())
			gui.togglePlaylistArea(null);
		searchField.requestFocusInWindow();
	}

	private class PlaylistList extends JXList {
		public PlaylistList(ListModel<?> m) {
			super(m);
		}

		@Override
		protected void doFind() {}
	}

	private class PlaylistSearchListener implements DocumentListener {

		@Override
		public void insertUpdate(DocumentEvent e) { changedUpdate(e); }

		@Override
		public void removeUpdate(DocumentEvent e) { changedUpdate(e); }

		@Override
		public void changedUpdate(DocumentEvent e) {
			sorter.setRowFilter(new RowFilter<ListModel<SongItem>, Integer>() {
				@Override
				public boolean include(Entry<? extends ListModel<SongItem>, ? extends Integer> entry) {
					String filterText = searchField.getText().trim();
					return entry.getStringValue(0).toUpperCase()
							.contains(filterText.toUpperCase())
							|| filterText.isEmpty();
				}
			});
		}
	}

	private class PlaylistMouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				switchSongToSelected(null);
			}
		}
	}
}