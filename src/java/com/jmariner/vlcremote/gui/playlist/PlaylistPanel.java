package com.jmariner.vlcremote.gui.playlist;

import com.jmariner.vlcremote.SongItem;
import com.jmariner.vlcremote.gui.ClearableTextField;
import com.jmariner.vlcremote.gui.RemoteInterface;
import com.jmariner.vlcremote.util.GuiUtils;
import com.jmariner.vlcremote.util.SVGIcon;
import com.jmariner.vlcremote.util.SimpleIcon;
import com.jmariner.vlcremote.util.UserSettings;
import com.jmariner.vlcremote.util.VLCStatus;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

import static com.jmariner.vlcremote.util.Constants.*;

@Slf4j
public class PlaylistPanel extends JPanel {

	private RemoteInterface gui;

	private PlaylistTable table;
	private JScrollPane scrollPane;

	private JButton favoriteButton, viewCurrentButton, 
					clearFiltersButton, sortOrderButton;
	private JComboBox<String> albumSelectionBox, sortSelectionBox;
	
	private SVGIcon addFavIcon, removeFavIcon, ascendingIcon, descendingIcon;

	private Point screenPos;
	private int hoverRow;
	
	private static final String ASCENDING_KEY = "ascending";
	private static final String DESCENDING_KEY = "descending";

	protected SongItem currentSong;
	protected ClearableTextField searchField;
	protected JToggleButton showFavoritesButton;
	protected boolean filterEnabled;

	protected Map<Integer, SongItem> songMap;

	public PlaylistPanel(RemoteInterface gui) {
		this.gui = gui;

		addFavIcon = SimpleIcon.FAVORITE.get();
		removeFavIcon = SimpleIcon.FAVORITE_EMPTY.get();
		ascendingIcon = SimpleIcon.DOWN_ARROW.get();
		descendingIcon = SimpleIcon.UP_ARROW.get();
		
		this.hoverRow = -2;

		init();
		initListeners();
		
		gui.addControlComponent(searchField);
	}
	
	private void init() {
		
		table = new PlaylistTable(this);

		scrollPane = new JScrollPane(
				table,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
		);

		JLabel title = new JLabel("Playlist:");
		title.setFont(FONT.deriveFont(24f).deriveFont(UNDERLINE));
		title.setHorizontalAlignment(SwingConstants.CENTER);
		
		albumSelectionBox = new JComboBox<>();
		albumSelectionBox.setVisible(false);
		albumSelectionBox.setFont(FONT);
		
		sortSelectionBox = new JComboBox<>(new DefaultComboBoxModel<>(
				PlaylistTable.ORDERS.stream().collect(Collectors.toCollection(Vector::new))
		));
		sortSelectionBox.setFont(FONT);
		
		JPanel topLeftPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, MAIN_PADDING, 0));
		topLeftPanel.add(title);
		topLeftPanel.add(albumSelectionBox);
		
		searchField = new ClearableTextField(18);
		searchField.setOnClear(gui::clearFocus);

		SVGIcon emptyFav = SimpleIcon.FAVORITE_EMPTY.get();
		SVGIcon filledFav = SimpleIcon.FAVORITE.get(SimpleIcon.Defaults.SELECTED_COLOR);
		
		showFavoritesButton = new JToggleButton(emptyFav);
		showFavoritesButton.setSelectedIcon(filledFav);
		showFavoritesButton.setToolTipText("Show favorites");
		
		sortOrderButton = new JButton(ascendingIcon);
		sortOrderButton.setActionCommand(ASCENDING_KEY);
		sortOrderButton.setToolTipText("Ascending sort");
		favoriteButton = new JButton(addFavIcon);
		favoriteButton.setToolTipText("Save selected song as favorite");
		viewCurrentButton = new JButton(SimpleIcon.JUMP_TO.get());
		viewCurrentButton.setToolTipText("View the currently playing song in the list");
		clearFiltersButton = new JButton(SimpleIcon.CLEAR_FILTER.get());
		clearFiltersButton.setToolTipText("Clear the search and favorite filters");
		
		Dimension dim = GuiUtils.squareDim(SimpleIcon.Defaults.BUTTON_SIZE);
		Arrays.asList(sortOrderButton, showFavoritesButton,
				favoriteButton, viewCurrentButton, clearFiltersButton)
			.forEach(b -> b.setPreferredSize(dim));

		JPanel playlistSearch = new JPanel(new FlowLayout(FlowLayout.CENTER, MAIN_PADDING, 0));
		playlistSearch.add(new JLabel("Search:"));
		playlistSearch.add(searchField);
		playlistSearch.add(showFavoritesButton);

		JPanel top = new JPanel(new BorderLayout(0, 0));
		top.add(topLeftPanel, BorderLayout.WEST);
		top.add(playlistSearch, BorderLayout.EAST);

		JPanel bottomLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, MAIN_PADDING, 0));
		bottomLeft.add(new JLabel("Sort:"));
		bottomLeft.add(sortSelectionBox);
		bottomLeft.add(sortOrderButton);
		
		JPanel bottomRight =
				GuiUtils.horizontalGridOf(favoriteButton, viewCurrentButton, clearFiltersButton);
		
		JPanel bottom = new JPanel(new GridLayout(1, 2));
		bottom.add(bottomLeft);
		bottom.add(bottomRight);

		this.setBorder(MAIN_PADDING_BORDER);
		this.setPreferredSize(new Dimension(MAIN_WIDTH, PLAYLIST_HEIGHT));
		this.setLayout(new BorderLayout(0, MAIN_PADDING));
		this.add(top, BorderLayout.NORTH);
		this.add(scrollPane, BorderLayout.CENTER);
		this.add(bottom, BorderLayout.SOUTH);
	}
	
	private void initListeners() {
		showFavoritesButton.addActionListener(this::toggleShowFavorites);

		favoriteButton.addActionListener(e ->
			favoriteSong(e.getActionCommand(), table.getSelected().toString())
		);
		viewCurrentButton.addActionListener(this::scrollToCurrent);
		clearFiltersButton.addActionListener(this::clearFilters);
		albumSelectionBox.addActionListener(this::switchAlbum);
		sortSelectionBox.addActionListener(this::switchSortType);
		sortOrderButton.addActionListener(this::switchSortOrder);
		searchField.setChangeAction(this::filterChanged);

		PlaylistListener listener = new PlaylistListener();
		table.addMouseListener(listener);
		table.addMouseMotionListener(listener);
		table.getSelectionModel().addListSelectionListener(this::selectionChanged);
		
		gui.addMouseMotionListener(new MouseAdapter() {
			public void mouseExited(MouseEvent e) { 
				PlaylistPanel.this.mouseExited();
			}
		});
		
		ToolTipManager.sharedInstance().unregisterComponent(table);

	//	scrollPane.getViewport().addChangeListener(listener);
		
		this.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				scrollToCurrent(null);
			}
		});

	}
	
	public void initPost() {
		VLCStatus status = gui.getRemote().getStatus();
		this.songMap = status.getSongMap();
		
		if (status.libraryExists()) {
			albumSelectionBox.setModel(new DefaultComboBoxModel<>(
					status.getLibraryFolders().keySet()
					.stream().collect(Collectors.toCollection(Vector::new))
			));
			albumSelectionBox.setVisible(true);
		}
		
		table.initPost();
	}
	
	public void update(VLCStatus status) {
		currentSong = status.getCurrentSong();
		if (table.getSelected() == null ||!table.getSelected().equals(currentSong))
			scrollToCurrent(null);
		viewCurrentButton.setEnabled(table.getRowOf(currentSong) > -1);
		albumSelectionBox.setSelectedItem(status.getCurrentAlbum());
	}
	
	private void switchSortOrder(ActionEvent e) {
		boolean wasAscending = e.getActionCommand().equals(ASCENDING_KEY);
		sortOrderButton.setActionCommand(wasAscending ? DESCENDING_KEY : ASCENDING_KEY);
		sortOrderButton.setIcon(wasAscending ? descendingIcon : ascendingIcon);
		sortOrderButton.setToolTipText(wasAscending ? "Descending sort" : "Ascending sort");

		switchSortType(null);
	}
	
	private void switchSortType(AWTEvent e) {
		table.sortBy((String) sortSelectionBox.getSelectedItem(),
				sortOrderButton.getActionCommand().equals(ASCENDING_KEY));
	}
	
	private void switchAlbum(AWTEvent e) {
		String album = (String) albumSelectionBox.getSelectedItem();
		if (!album.equals(gui.getRemote().getStatus().getCurrentAlbum())) {
			VLCStatus status = gui.getRemote().switchAlbum(album);
			this.songMap = status.getSongMap();
			update(status);
			table.initPost();
		}
	}
	
	private void clearFilters(AWTEvent e) {
		searchField.clear();
		if (showFavoritesButton.isSelected())
			showFavoritesButton.doClick();
		table.setFilterEnabled(false);
	}
	
	public void startSearch() {
		if (!gui.isPlaylistAreaShowing())
			gui.togglePlaylistArea(null);
		searchField.requestFocusInWindow();
	}

	private void toggleShowFavorites(AWTEvent e) {
		boolean sel = showFavoritesButton.isSelected();
		showFavoritesButton.setToolTipText(sel ? "Show all" : "Show favorites");
		filterChanged();
	}
	
	protected void favoriteSong(String action, String title) {
		if (action.equals("remove"))
			UserSettings.removeFavorite(title);
		else
			UserSettings.addFavorite(title);

		filterChanged();
		selectionChanged(null);
	}

	private void selectionChanged(ListSelectionEvent e) {
		SongItem song = table.getSelected();
		boolean exists = song != null;
		favoriteButton.setEnabled(exists);
		if (exists) {
			boolean fav = UserSettings.isFavorite(song.toString());
			favoriteButton.setActionCommand(fav ? "remove" : "add");
			favoriteButton.setIcon(fav ? removeFavIcon : addFavIcon);
			favoriteButton.setToolTipText(fav ? "Remove selected song from favorites" : "Save selected song to favorites");
		}
	}

	private void filterChanged() {
		table.setFilterEnabled(true);
		viewCurrentButton.setEnabled(table.getRowOf(currentSong) > -1);
	}
	
	protected void switchSong(int id) {
		VLCStatus s = gui.getRemote().switchSong(id);
		if (UserSettings.getBoolean("restartOnTrackChange", false))
			gui.getRemote().restartStream(1000);
		table.scrollToSelected();
		gui.updateInterface(s);
	}
	
	private void scrollToCurrent(AWTEvent e) {
		table.setSelected(currentSong);
		table.scrollToSelected();
	}
	
	private void mouseExited() {
		table.disableInteractiveRow();
		screenPos = null;
	}

	private class PlaylistListener extends MouseAdapter implements ChangeListener {
		

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				switchSong(table.getSelected().getId());
			}
			e.consume();
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			if (e != null)
				screenPos = e.getLocationOnScreen();

			Point p = new Point(screenPos);
			SwingUtilities.convertPointFromScreen(p, table);

			hoverRow = table.rowAtPoint(p);
			if (hoverRow > -1) {
				table.setInteractiveRow(hoverRow);
			}
		}

		@Override
		public void mouseExited(MouseEvent e) {

			Point p = e.getPoint();
			JViewport view = table.getViewport();
			Point viewPos = view.getViewPosition();
			//check if it actually left the table and didn't just enter a child component
			if 	(p.x < viewPos.x || p.x > view.getWidth() ||
				 p.y < viewPos.y || p.y > viewPos.y + view.getHeight()) {
				
				PlaylistPanel.this.mouseExited();
			}
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if (screenPos != null) 
				mouseMoved(null); // TODO this doesn't work
		}
	}

}
