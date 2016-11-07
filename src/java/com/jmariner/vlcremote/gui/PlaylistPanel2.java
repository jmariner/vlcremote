package com.jmariner.vlcremote.gui;

import static com.jmariner.vlcremote.util.Constants.FLOW_CENTER;
import static com.jmariner.vlcremote.util.Constants.FONT;
import static com.jmariner.vlcremote.util.Constants.MAIN_WIDTH;
import static com.jmariner.vlcremote.util.Constants.PLAYLIST_HEIGHT;
import static com.jmariner.vlcremote.util.Constants.UNDERLINE;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.RowFilter.Entry;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

import org.jdesktop.swingx.JXList;
import org.jdesktop.swingx.sort.ListSortController;

import com.jmariner.vlcremote.SongItem;
import com.jmariner.vlcremote.util.SimpleIcon;
import com.jmariner.vlcremote.util.UserSettings;

import lombok.AccessLevel;
import lombok.Getter;

public class PlaylistPanel2 extends JPanel {
	
	private RemoteInterface gui;
	
	private ImageIcon showFavorites, hideFavorites;
	
	private Preferences favoritesPref;
	@Getter(AccessLevel.PROTECTED)
	private List<String> favorites;

	private JTable table;
	private JTextField searchField;
	private JButton playSelectedButton, favoriteButton, jumpToSelectedButton;
	private JToggleButton showFavoritesButton;

	private ListSortController<ListModel<SongItem>> sorter;
	private RowFilter<ListModel<SongItem>, Integer> filter;
	
	public PlaylistPanel2(RemoteInterface gui) {
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
	
	private void init() {
		table = new PlaylistTable();

		JScrollPane scrollPane = new JScrollPane(
				table,
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
	}
	
	private class PlaylistTable extends JTable {
		
		public PlaylistTable() {
			super();
						
			this.setTableHeader(null);
			this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		
		protected void initPost() {
			Vector<Vector<SongItem>> data = 
					gui.getRemote().getSongMap().values().stream()
					.map(s -> new Vector<>(Arrays.asList(s)))
					.collect(Collectors.toCollection(Vector::new));
			
			Vector<String> headers = new Vector<>(Arrays.asList(""));
			
			this.setModel(new DefaultTableModel(data, headers));
		}
	}
	
	private class PlaylistSearchListener implements DocumentListener {

		public void insertUpdate(DocumentEvent e) { changedUpdate(e); }
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
