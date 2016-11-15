package com.jmariner.vlcremote.gui.playlist;

import com.jmariner.vlcremote.SongItem;
import com.jmariner.vlcremote.gui.ClearableTextField;
import com.jmariner.vlcremote.gui.RemoteInterface;
import com.jmariner.vlcremote.util.SimpleIcon;
import com.jmariner.vlcremote.util.Constants;
import com.jmariner.vlcremote.util.GuiUtils;
import com.jmariner.vlcremote.util.UserSettings;
import com.jmariner.vlcremote.util.VLCStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.*;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static com.jmariner.vlcremote.util.Constants.*;

@Slf4j
public class PlaylistPanel extends JPanel {
	
	private RemoteInterface gui;
		
	private Preferences favoritesPref;
	@Getter(AccessLevel.PROTECTED)
	private List<String> favorites;

	private SongItem currentSong;

	private PlaylistTable table;
	private ClearableTextField searchField;
	private JButton playSelectedButton, favoriteButton, viewCurrentButton, clearFiltersButton;
	private JToggleButton showFavoritesButton;

	private static final Border SELECTED_BORDER 	= UIManager.getBorder("Table.focusCellHighlightBorder");
	private static final Color SELECTED_BACKGROUND = UIManager.getColor("Table.selectionBackground");
	private static final Color SELECTED_FOREGROUND = UIManager.getColor("Table.selectionForeground");
	private static final Color DEFAULT_BACKGROUND 	= UIManager.getColor("Table.background");
	private static final Color DEFAULT_FOREGROUND 	= UIManager.getColor("Table.foreground");
	private static final Border DEFAULT_BORDER;
	static {
		int i = ((LineBorder)SELECTED_BORDER).getThickness();
		DEFAULT_BORDER = BorderFactory.createEmptyBorder(i, i, i, i);
	}
	
	public PlaylistPanel(RemoteInterface gui) {
		super(new BorderLayout(0, 10));
		this.gui = gui;
		
		favoritesPref = UserSettings.getChild("favorites");
		favorites = new ArrayList<>();
		
		init();
		initListeners();
		
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

		searchField = new ClearableTextField(18);
		searchField.setOnClear(gui::clearFocus);

		showFavoritesButton = new JToggleButton(SimpleIcon.FAVORITE_EMPTY.get());
		showFavoritesButton.setSelectedIcon(
				SimpleIcon.FAVORITE.get(SimpleIcon.Defaults.SELECTED_COLOR));
		showFavoritesButton.setToolTipText("Show favorites");
		
		playSelectedButton = new JButton(SimpleIcon.PLAY_OUTLINE.get());
		playSelectedButton.setToolTipText("Play the selected song");
		favoriteButton = new JButton("Favorite Selected");
		favoriteButton.setToolTipText("Save selected song as favorite");
		viewCurrentButton = new JButton(SimpleIcon.JUMP_TO.get());
		viewCurrentButton.setToolTipText("View the currently playing song in the list");
		clearFiltersButton = new JButton(SimpleIcon.CLEAR_FILTER.get());
		clearFiltersButton.setToolTipText("Clear the search and favorite filters");
		
		Dimension dim = GuiUtils.squareDim(SimpleIcon.Defaults.BUTTON_SIZE);
		Arrays.asList(showFavoritesButton, playSelectedButton, viewCurrentButton, clearFiltersButton)
		.forEach(b -> b.setPreferredSize(dim));

		JPanel playlistSearch = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
		playlistSearch.add(new JLabel("Search"));
		playlistSearch.add(searchField);
		playlistSearch.add(showFavoritesButton);

		JPanel playlistTop = new JPanel(new BorderLayout());
		playlistTop.add(title, BorderLayout.WEST);
		playlistTop.add(playlistSearch, BorderLayout.EAST);

		JPanel bottom = GuiUtils.horizontalGridOf(
				playSelectedButton, favoriteButton, viewCurrentButton, clearFiltersButton);

		this.setBorder(MAIN_PADDING_BORDER);
		this.setPreferredSize(new Dimension(MAIN_WIDTH, PLAYLIST_HEIGHT));
		this.add(playlistTop, BorderLayout.NORTH);
		this.add(scrollPane, BorderLayout.CENTER);
		this.add(bottom, BorderLayout.SOUTH);

		gui.setPlaylistAreaShowing(false);
	}
	
	private void initListeners() {
		showFavoritesButton.addActionListener(this::toggleShowFavorites);

		playSelectedButton.addActionListener(this::switchSongToSelected);
		favoriteButton.addActionListener(this::favoriteSelected);
		viewCurrentButton.addActionListener(this::scrollToCurrent);
		clearFiltersButton.addActionListener(this::clearFilters);
		
		PlaylistListener listener = new PlaylistListener();
		table.addMouseListener(listener);
		table.getSelectionModel().addListSelectionListener(this::selectionChanged);
		searchField.getDocument().addDocumentListener(listener);
	}
	
	public void initPost() {
		table.initPost();
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
	
	public void loadSettings() {
		int i = 1;
		String f;
		while ((f = favoritesPref.get(""+i++, null)) != null)
			favorites.add(f);
	}
	
	private void toggleShowFavorites(AWTEvent e) {
		boolean sel = showFavoritesButton.isSelected();
		showFavoritesButton.setToolTipText(sel ? "Show all" : "Show favorites");
		table.setFilterEnabled(true);
	}
	
	private void favoriteSelected(ActionEvent e) {
		String cur = table.getSelected().toString();
		if (e.getActionCommand().equals("remove"))
			favorites.remove(cur);
		else
			favorites.add(cur);
		
		table.setFilterEnabled(true);
		selectionChanged(null);
		updateFavorites();
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
	
	protected void selectionChanged(ListSelectionEvent e) {
		SongItem song = table.getSelected();
		favoriteButton.setEnabled(song != null);
		if (song != null) {
			boolean fav = favorites.contains(song.toString());
			favoriteButton.setActionCommand(fav ? "remove" : "add");
			favoriteButton.setText((fav ? "Unf":"F") + "avorite Selected");
			favoriteButton.setToolTipText(fav ? "Remove selected song from favorites" : "Save selected song to favorites");
		}
	}

	private void searchChanged() {
		table.setFilterEnabled(true);
		viewCurrentButton.setEnabled(table.itemExists(currentSong));
	}
	
	public void update(VLCStatus status) {
		currentSong = status.getCurrentSong();
		if (table.getSelected() == null || !table.getSelected().equals(currentSong)) {
			scrollToCurrent(null);
		}
	}
	
	private void switchSongToSelected(AWTEvent e) {
		
		VLCStatus s = gui.getRemote().switchSong(table.getSelected().getId());
		
	//	clearFilters();
		table.scrollToSelected();
		
		gui.updateInterface(s);
	}
	
	private void scrollToCurrent(AWTEvent e) {
		table.setSelected(currentSong);
		table.scrollToSelected();
	}
	
	private class PlaylistTable extends JTable {
		
		private TableRowSorter<TableModel> sorter;
		private RowFilter<TableModel, Integer> filter;
		
		private boolean filterEnabled;
				
		public PlaylistTable() {
			super();
			
			sorter = new TableRowSorter<>();
			filter = new RowFilter<TableModel, Integer>() {
				@Override
				public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
					
					if (!filterEnabled)
						return true;
					
					String filterText = searchField.getText().trim();
					String name = entry.getStringValue(0);
					boolean show = true;
					
					if (showFavoritesButton.isSelected())
						show = favorites.contains(name);
					
					if (!filterText.isEmpty())
						show &= name.toUpperCase().contains(filterText.toUpperCase());
					
					return show;
					
				}
			};
			
			filterEnabled = false;
			
			this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			this.setRowSelectionAllowed(true);
			this.setColumnSelectionAllowed(true);
			this.setDefaultEditor(SongItem.class, null);
			this.setDefaultRenderer(SongItem.class, new PlaylistCellRenderer());
		}
		
		protected void initPost() {
			Vector<Vector<SongItem>> data = 
					gui.getRemote().getStatus().getSongMap().values().stream()
					.map(s -> new Vector<>(Collections.singletonList(s)))
					.collect(Collectors.toCollection(Vector::new));
			
			Vector<String> headers = new Vector<>(Collections.singletonList(""));
			
			DefaultTableModel model = new DefaultTableModel(data, headers) {
				@Override
				public Class<?> getColumnClass(int c) {
					return SongItem.class;
				}
			};
			this.setModel(model);
			sorter.setModel(model);
			this.setTableHeader(null);
			this.setRowSorter(sorter);
		}
		
		protected void setFilterEnabled(boolean enabled) {
			filterEnabled = enabled;
			sorter.setRowFilter(filter);
		}

		protected boolean itemExists(SongItem item) {
			int i = 0;
			while (true) {
				try {
					if (getValueAt(i++, 0).equals(item)) return true;
				}
				catch (IndexOutOfBoundsException e) { return false; }
			}
		}
		
		protected void setSelected(SongItem item) {
			int i = -1;
			while (true) {
				try {
					if (getValueAt(++i, 0).equals(item)) {
						table.setRowSelectionInterval(i, i);
						return;
					}
				}
				catch (IndexOutOfBoundsException e) { return; }
			}
		}
		
		protected SongItem getSelected() {
			int r = getSelectedRow();
			return r == -1 ? null : (SongItem) getValueAt(r, 0);
		}
		
		protected boolean isSelectionVisible() {
			Container c = getParent();
			int r = getSelectedRow();
			
			return  (r > -1) &&
					(c instanceof JViewport) &&
					c.contains(getCellRect(r, 0, true).getLocation());
			
		}
		
		protected void scrollToSelected() {
			int selected = getSelectedRow();

			if (selected == -1 || isSelectionVisible()) return;
			
			// calculate possible spacing before and after selection
			int last = getRowCount()-1;
			int before = selected < 5 ? selected : 5;
			int after = selected > last-5 ? last-selected : 5;
			
			int h = getRowHeight();
			before *= h;
			after *= h;
			
			// create spacing before and after selection so it is somewhat center
			Rectangle rect = getCellRect(selected, 0, true);
			rect.setLocation(rect.x, rect.y - before);
			rect.setSize(rect.width, before + rect.height + after);
			
			// account for the viewport position
			JViewport view = (JViewport) getParent();
			Point pt = view.getViewPosition();
			rect.setLocation(rect.x - pt.x, rect.y - pt.y);
			
			view.scrollRectToVisible(rect);
		}
		
		private class PlaylistCellRenderer extends JPanel implements TableCellRenderer {
			
			private JLabel label, fav;
			
			private ImageIcon favIcon, addFavHoverIcon, addFavIcon;
			
			private int hoverRow;
			private boolean hoverFav;
			
			public PlaylistCellRenderer() {
				super(Constants.BORDER_LAYOUT);
				
				PlaylistTable table = PlaylistTable.this;
				
				int h = getRowHeight();
				Color fg =  UIManager.getColor("Label.foreground");
				
				favIcon = SimpleIcon.FAVORITE.get(h, Color.PINK);
				addFavHoverIcon = SimpleIcon.FAVORITE.get(h, fg);
				addFavIcon = SimpleIcon.FAVORITE_EMPTY.get(h, fg);
				
				label = new JLabel();
				label.setBorder(new EmptyBorder(0, MAIN_PADDING, 0, 0));
				
				fav = new JLabel();
				label.setBorder(new EmptyBorder(0, 0, 0, MAIN_PADDING));
				
				fav.setVisible(false);
				
				this.add(label, BorderLayout.WEST);
				this.add(fav, BorderLayout.EAST);
				
				// TODO setting this to true makes this and the table transparent so the frame background shows
				//		no idea why, it doesn't make any sense. just setting to false to table background can show
				this.setOpaque(false);
				
				table.addMouseMotionListener(new MouseMotionAdapter() {
					public void mouseMoved(MouseEvent e) {
						Point p = e.getPoint();
						hoverRow = table.rowAtPoint(p);
						int w = (int)(table.getRowHeight() * 1.5);
						table.repaint(
								table.getWidth() - w,
								0,
								w,
								table.getHeight()
							);
						hoverFav = p.x > table.getWidth()-w;
					}
				});
			}
			
			@Override
			public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
				
				String s = v.toString();
				
				label.setText(s);
				
				boolean isFav = favorites.contains(s);
				fav.setIcon(isFav ? favIcon : hoverFav ? addFavHoverIcon : addFavIcon);
				fav.setVisible(r == hoverRow || isFav);
				
				if (sel) {
					// TODO background colors don't do anything here, see previous todo
					this.setBackground(SELECTED_BACKGROUND);
					this.setForeground(SELECTED_FOREGROUND);
					this.setBorder(SELECTED_BORDER);
				}
				else {
					this.setBackground(DEFAULT_BACKGROUND);
					this.setForeground(DEFAULT_FOREGROUND);
					this.setBorder(DEFAULT_BORDER);
				}
				
				return this;
			}
			
			@Override
			public void setForeground(Color c) {
				if (label != null && fav != null) {
					label.setForeground(c);
					fav.setForeground(c);
				}
			}
			
			// DefaultTableCellRenderer states these should be overridden to no-ops
			@Override public void revalidate() {}
			@Override public void repaint(long t, int x, int y, int w, int h) {}
			@Override public void repaint(Rectangle r) {}
			@Override public void repaint() {}
			@Override public void firePropertyChange(String p, boolean o, boolean n) {}
			
			/**
			 * Direct copy of {@link DefaultTableCellRenderer#firePropertyChange(String, Object, Object)}
			 */
			@SuppressWarnings("StringEquality")
			@Override
		    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		        // Strings get interned...
		        if (propertyName=="text"
		                || propertyName == "labelFor"
		                || propertyName == "displayedMnemonic"
		                || ((propertyName == "font" || propertyName == "foreground")
		                    && oldValue != newValue
		                    && getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey) != null)) {

		            super.firePropertyChange(propertyName, oldValue, newValue);
		        }
		    }
			
			// --END PlaylistCellRenderer--
		}
		
		// --END PlaylistTable--
	}
	
	private class PlaylistListener extends MouseAdapter implements DocumentListener {
		
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				switchSongToSelected(null);
			}
		}

		@Override public void insertUpdate(DocumentEvent e) { changedUpdate(e); }
		@Override public void removeUpdate(DocumentEvent e) { changedUpdate(e); }

		@Override
		public void changedUpdate(DocumentEvent e) {
			searchChanged();
		}
	}

}
