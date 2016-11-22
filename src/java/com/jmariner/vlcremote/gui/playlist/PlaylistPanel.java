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
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

	private JButton playSelectedButton, favoriteButton, viewCurrentButton, clearFiltersButton;
	private JComboBox<String> albumSelectionBox;
	
	private SVGIcon addFavIcon, removeFavIcon;

	protected SongItem currentSong;
	protected ClearableTextField searchField;
	protected JToggleButton showFavoritesButton;
	protected boolean filterEnabled;
	protected int hoverRow;

	protected Map<Integer, SongItem> songMap;

	public PlaylistPanel(RemoteInterface gui) {
		this.gui = gui;

		addFavIcon = SimpleIcon.FAVORITE.get();
		removeFavIcon = SimpleIcon.FAVORITE_EMPTY.get();
		
		this.hoverRow = -2;

		init();
		initListeners();
		
		gui.addControlComponent(searchField);
		gui.addControlComponent(playSelectedButton);
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
		
		albumSelectionBox = new JComboBox<String>();
		albumSelectionBox.setVisible(false);
		albumSelectionBox.setFont(FONT);
		
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
		
		playSelectedButton = new JButton(SimpleIcon.PLAY_OUTLINE.get());
		playSelectedButton.setToolTipText("Play the selected song");
		favoriteButton = new JButton(addFavIcon);
		favoriteButton.setToolTipText("Save selected song as favorite");
		viewCurrentButton = new JButton(SimpleIcon.JUMP_TO.get());
		viewCurrentButton.setToolTipText("View the currently playing song in the list");
		clearFiltersButton = new JButton(SimpleIcon.CLEAR_FILTER.get());
		clearFiltersButton.setToolTipText("Clear the search and favorite filters");
		
		Dimension dim = GuiUtils.squareDim(SimpleIcon.Defaults.BUTTON_SIZE);
		Arrays.asList(showFavoritesButton, favoriteButton, playSelectedButton, viewCurrentButton, clearFiltersButton)
			.forEach(b -> b.setPreferredSize(dim));

		JPanel playlistSearch = new JPanel(new FlowLayout(FlowLayout.CENTER, MAIN_PADDING, 0));
		playlistSearch.add(new JLabel("Search:"));
		playlistSearch.add(searchField);
		playlistSearch.add(showFavoritesButton);

		JPanel playlistTop = new JPanel(new BorderLayout(0, 0));
		playlistTop.add(topLeftPanel, BorderLayout.WEST);
		playlistTop.add(playlistSearch, BorderLayout.EAST);

		JPanel bottom = GuiUtils.horizontalGridOf(
				playSelectedButton, favoriteButton, viewCurrentButton, clearFiltersButton);

		this.setBorder(MAIN_PADDING_BORDER);
		this.setPreferredSize(new Dimension(MAIN_WIDTH, PLAYLIST_HEIGHT));
		this.setLayout(new BorderLayout(0, MAIN_PADDING));
		this.add(playlistTop, BorderLayout.NORTH);
		this.add(scrollPane, BorderLayout.CENTER);
		this.add(bottom, BorderLayout.SOUTH);
	}
	
	private void initListeners() {
		showFavoritesButton.addActionListener(this::toggleShowFavorites);

		playSelectedButton.addActionListener(this::switchSongToSelected);
		favoriteButton.addActionListener(this::favoriteSelected);
		viewCurrentButton.addActionListener(this::scrollToCurrent);
		clearFiltersButton.addActionListener(this::clearFilters);
		albumSelectionBox.addActionListener(this::switchAlbum);
		table.getSelectionModel().addListSelectionListener(this::selectionChanged);

		PlaylistListener listener = new PlaylistListener();
		table.addMouseListener(listener);
		table.addMouseMotionListener(listener);
		searchField.setChangeAction(this::filterChanged);
		
		scrollPane.getViewport().addChangeListener(listener);
		
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
		table.repaintHoverArea();
		viewCurrentButton.setEnabled(table.getRowOf(currentSong) > -1);
		albumSelectionBox.setSelectedItem(status.getCurrentAlbum());
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
	
	private void favoriteSelected(ActionEvent e) {
		String cur = table.getSelected().toString();
		if (e.getActionCommand().equals("remove"))
			UserSettings.removeFavorite(cur);
		else
			UserSettings.addFavorite(cur);

		filterChanged();
		selectionChanged(null);
	}

	protected void selectionChanged(ListSelectionEvent e) {
		SongItem song = table.getSelected();
		boolean exists = song != null;
		favoriteButton.setEnabled(exists);
		playSelectedButton.setEnabled(exists);
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
	
	private void switchSongToSelected(AWTEvent e) {
		VLCStatus s = gui.getRemote().switchSong(table.getSelected().getId());
		table.scrollToSelected();
		gui.updateInterface(s);
	}
	
	private void scrollToCurrent(AWTEvent e) {
		table.setSelected(currentSong);
		table.scrollToSelected();
	}

	private class PlaylistListener extends MouseAdapter implements ChangeListener {
		
		private Point pos;

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				switchSongToSelected(null);
			}
			e.consume();
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			if (e != null)
				pos = e.getLocationOnScreen();
			
			Point p = new Point(pos);
			SwingUtilities.convertPointFromScreen(p, table);
			
			int prev = hoverRow;
			hoverRow = table.rowAtPoint(p);
			
			if (hoverRow != prev) {
				
				table.setInteractiveRow(hoverRow);
				
				log.info("moved from row " + prev + " to row " + hoverRow);
			}
		}
		
		@Override
		public void mouseExited(MouseEvent e) {
			Point p = new Point(pos);
			JViewport view = table.getViewport();
			SwingUtilities.convertPointFromScreen(p, view);
			if (!view.getBounds().contains(p)) {
				
				table.disableInteractiveRow();
				
				hoverRow = -2; // -2 since JTable's row getters return -1 for none
				pos = null;
				
				log.info("exited!");
			}
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if (pos != null)
				mouseMoved(null);
		}
	}

}
