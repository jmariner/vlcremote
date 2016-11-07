package com.jmariner.vlcremote.gui;

import com.jmariner.vlcremote.SongItem;
import com.jmariner.vlcremote.util.SimpleIcon;
import com.jmariner.vlcremote.util.UserSettings;
import com.jmariner.vlcremote.util.VLCStatus;

import lombok.AccessLevel;
import lombok.Getter;

import org.jdesktop.swingx.JXList;
import org.jdesktop.swingx.sort.ListSortController;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static com.jmariner.vlcremote.util.Constants.*;

public class PlaylistPanel extends JPanel {

	private RemoteInterface gui;
	
	private ImageIcon showFavorites, hideFavorites;
	
	private Preferences favoritesPref;
	@Getter(AccessLevel.PROTECTED)
	private List<String> favorites;

	private JXList list;
	private JTextField searchField;
	private JButton playSelectedButton, favoriteButton, jumpToSelectedButton;
	private JToggleButton showFavoritesButton;

	private ListSortController<ListModel<SongItem>> sorter;
	private RowFilter<ListModel<SongItem>, Integer> filter;

	protected PlaylistPanel(RemoteInterface gui) {
		super(new BorderLayout(0, 10));
		this.gui = gui;
		
		double scale = .75;
		showFavorites = SimpleIcon.FAVORITE_EMPTY.get(scale);
		hideFavorites = SimpleIcon.FAVORITE.get(scale);
		
		favoritesPref = UserSettings.getChild("favorites");
		favorites = new ArrayList<>();
		
		filter = new RowFilter<ListModel<SongItem>, Integer>() {
			@Override
			public boolean include(Entry<? extends ListModel<SongItem>, ? extends Integer> entry) {
				
				String filterText = searchField.getText().trim();
				String name = entry.getStringValue(0);
				boolean show = true;
				
				if (showFavoritesButton.isSelected())
					show &= favorites.contains(name);
				
				if (!filterText.isEmpty())
					show &= name.toUpperCase().contains(filterText.toUpperCase());
				
				return show;
				
			}
		};
		
		init();
		
		gui.addControlComponent(searchField);
		gui.addControlComponent(playSelectedButton);
	}

	protected void init() {

		list = new PlaylistList();
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(new FavoriteRenderer());

		JScrollPane scrollPane = new JScrollPane(
				list,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
		);

		JLabel title = new JLabel("Playlist");
		title.setFont(FONT.deriveFont(18f).deriveFont(UNDERLINE));
		title.setHorizontalAlignment(SwingConstants.CENTER);
		
		playSelectedButton = new JButton("Play Selected");
		favoriteButton = new JButton("Favorite Selected");
		showFavoritesButton = new JToggleButton(showFavorites);
		jumpToSelectedButton = new JButton("View Selected");

		searchField = new JTextField(20);
		JPanel playlistSearch = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
		playlistSearch.add(new JLabel("Search"));
		playlistSearch.add(searchField);
		playlistSearch.add(showFavoritesButton);

		JPanel playlistTop = new JPanel(new BorderLayout());
		playlistTop.add(title, BorderLayout.WEST);
		playlistTop.add(playlistSearch, BorderLayout.EAST);

		JPanel bottomLeft = new JPanel(FLOW_CENTER);
		JPanel bottomRight = new JPanel(FLOW_CENTER);
		JPanel bottomMiddle = new JPanel(FLOW_CENTER);
		JPanel bottom = new JPanel(new GridLayout(1, 3));
		
		bottomLeft.add(favoriteButton);
		bottomRight.add(playSelectedButton);
		bottomMiddle.add(jumpToSelectedButton);
		bottom.add(bottomLeft);
		bottom.add(bottomMiddle);
		bottom.add(bottomRight);

		this.setBorder(new EmptyBorder(10, 10, 10, 10));
		this.setPreferredSize(new Dimension(MAIN_WIDTH, PLAYLIST_HEIGHT));
		this.add(playlistTop, BorderLayout.NORTH);
		this.add(scrollPane, BorderLayout.CENTER);
		this.add(bottom, BorderLayout.SOUTH);

		gui.setPlaylistAreaShowing(false);

		initListeners();
	}

	protected void initPost() {
		Map<Integer, SongItem> songMap = gui.getRemote().getSongMap();
		DefaultListModel<SongItem> playlist = new DefaultListModel<>();
		songMap.values().stream().forEachOrdered(playlist::addElement);
		
		list.setModel(playlist);
		sorter = new ListSortController<>(playlist);
		list.setRowSorter(sorter);
	}
	
	private void initListeners() {
		playSelectedButton.addActionListener(this::switchSongToSelected);
		favoriteButton.addActionListener(this::favoriteSelected);
		showFavoritesButton.addActionListener(this::toggleFavorites);
		jumpToSelectedButton.addActionListener(e -> this.viewSelected(true));
		
		list.addMouseListener(new PlaylistMouseListener());
		
		searchField.getDocument().addDocumentListener(new PlaylistSearchListener());
	}
	
	private void updateFavorites() {
		try {
			favoritesPref.clear();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
		
		for(int i=0, l=favorites.size(); i<l; i++)
			favoritesPref.put(""+(i+1), favorites.get(i));
	}
	
	private SongItem getSelected() {
		return ((SongItem) list.getSelectedValue());
	}
	
	protected void loadSettings() {
		int i = 1;
		String f;
		while ((f = favoritesPref.get(""+i++, null)) != null)
			favorites.add(f);
	}

	private void switchSongToSelected(AWTEvent e) {
		
		int index = ((SongItem)list.getSelectedValue()).getId();
		VLCStatus s = gui.getRemote().switchSong(index);
		
		viewSelected(true);
				
		gui.updateInterface(s);
	}
	
	private void favoriteSelected(ActionEvent e) {
		if (e.getActionCommand().equals("remove"))
			favorites.remove(""+getSelected());
		else
			favorites.add(""+getSelected());
		
		list.repaint();
		sorter.setRowFilter(filter);
		updateFavoriteButton();
		updateFavorites();
	}
	
	private void toggleFavorites(AWTEvent e) {
		if (showFavoritesButton.isSelected()) {
			showFavoritesButton.setIcon(hideFavorites);
		}
		else {
			showFavoritesButton.setIcon(showFavorites);
		}
		sorter.setRowFilter(filter);
	}
	
	private void viewSelected(boolean clear) {
		
		if (clear) clearFilters();
				
		int selected = list.getSelectedIndex();
		int size = list.getModel().getSize();
		int min = selected < 5 ? 0 : selected-5;
		int max = selected > size-6 ? size-1 : selected+5;
		Rectangle r = list.getCellBounds(min, max);
		list.scrollRectToVisible(r);
	}
	
	protected void updateFavoriteButton() {
		boolean fav = favorites.contains(""+getSelected());
		favoriteButton.setActionCommand(fav ? "remove" : "add");
		favoriteButton.setText((fav ? "Unf":"F") + "avorite Selected");
	}

	protected void update(VLCStatus status) {
		int sel = gui.getRemote().transformPlaylistID(status.getCurrentID());
		if (sel != list.getSelectedIndex())
			list.setSelectedIndex(sel); // TODO setSelectedValue doesn't work here for some reason
		
		viewSelected(false);
	}

	protected void startSearch() {
		if (!gui.isPlaylistAreaShowing())
			gui.togglePlaylistArea(null);
		searchField.requestFocusInWindow();
	}
	
	private void clearFilters() {
		searchField.setText("");
		if (showFavoritesButton.isSelected())
			showFavoritesButton.doClick();
	}
	
	private class FavoriteRenderer extends DefaultListCellRenderer {
		
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			
			JLabel c = (JLabel) super.getListCellRendererComponent(
					list, value, index, isSelected, cellHasFocus);
			
			SongItem item = (SongItem) value;
			
			if (favorites.contains(""+item))
				c.setText(item + "  *");
			else
				c.setText(""+item);
			
			return c;
		}
		
	}

	private class PlaylistList extends JXList {
		public PlaylistList() {
			super();
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
			sorter.setRowFilter(filter);
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
