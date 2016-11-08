package com.jmariner.vlcremote.gui;

import static com.jmariner.vlcremote.util.Constants.*;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.prefs.BackingStoreException;
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
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.jmariner.vlcremote.SongItem;
import com.jmariner.vlcremote.util.Constants;
import com.jmariner.vlcremote.util.GuiUtils;
import com.jmariner.vlcremote.util.SimpleIcon;
import com.jmariner.vlcremote.util.UserSettings;
import com.jmariner.vlcremote.util.VLCStatus;

import lombok.AccessLevel;
import lombok.Getter;

public class PlaylistPanel2 extends JPanel {
	
	private RemoteInterface gui;
	
	private ImageIcon showFavorites, hideFavorites;
	
	private Preferences favoritesPref;
	@Getter(AccessLevel.PROTECTED)
	private List<String> favorites;

	private PlaylistTable table;
	private JTextField searchField;
	private JButton playSelectedButton, favoriteButton, jumpToCurrentButton;
	private JToggleButton showFavoritesButton;
	
	public PlaylistPanel2(RemoteInterface gui) {
		super(new BorderLayout(0, 10));
		this.gui = gui;
		
		double scale = .75;
		showFavorites = SimpleIcon.FAVORITE_EMPTY.get(scale);
		hideFavorites = SimpleIcon.FAVORITE.get(scale);
		
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
		
		playSelectedButton = new JButton("Play Selected");
		favoriteButton = new JButton("Favorite Selected");
		showFavoritesButton = new JToggleButton(showFavorites);
		jumpToCurrentButton = new JButton("View Current");

		searchField = new JTextField(20);
		JPanel playlistSearch = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
		playlistSearch.add(new JLabel("Search"));
		playlistSearch.add(searchField);
		playlistSearch.add(showFavoritesButton);

		JPanel playlistTop = new JPanel(new BorderLayout());
		playlistTop.add(title, BorderLayout.WEST);
		playlistTop.add(playlistSearch, BorderLayout.EAST);

		JPanel bottom = GuiUtils.horizontalGridOf(playSelectedButton, favoriteButton, jumpToCurrentButton);

		this.setBorder(MAIN_PADDING_BORDER);
		this.setPreferredSize(new Dimension(MAIN_WIDTH, PLAYLIST_HEIGHT));
		this.add(playlistTop, BorderLayout.NORTH);
		this.add(scrollPane, BorderLayout.CENTER);
		this.add(bottom, BorderLayout.SOUTH);

		gui.setPlaylistAreaShowing(false);
	}
	
	private void initListeners() {
		playSelectedButton.addActionListener(this::switchSongToSelected);
		favoriteButton.addActionListener(this::favoriteSelected);
		showFavoritesButton.addActionListener(this::toggleShowFavorites);
		jumpToCurrentButton.addActionListener(this::scrollToCurrent);
		
		table.addMouseListener(new PlaylistMouseListener());
		
		searchField.getDocument().addDocumentListener(new PlaylistSearchListener());
	}
	
	protected void initPost() {
		table.initPost();
	}
	
	private void clearFilters() {
		searchField.setText("");
		if (showFavoritesButton.isSelected())
			showFavoritesButton.doClick();
		table.setFilterEnabled(false);
	}
	
	protected void startSearch() {
		if (!gui.isPlaylistAreaShowing())
			gui.togglePlaylistArea(null);
		searchField.requestFocusInWindow();
	}
	
	protected void loadSettings() {
		int i = 1;
		String f;
		while ((f = favoritesPref.get(""+i++, null)) != null)
			favorites.add(f);
	}
	
	private void toggleShowFavorites(AWTEvent e) {
		if (showFavoritesButton.isSelected()) {
			showFavoritesButton.setIcon(hideFavorites);
		}
		else {
			showFavoritesButton.setIcon(showFavorites);
		}
		table.setFilterEnabled(true);
	}

	
	private void favoriteSelected(ActionEvent e) {
		String cur = table.getSelected().toString();
		if (e.getActionCommand().equals("remove"))
			favorites.remove(cur);
		else
			favorites.add(cur);
		
		table.setFilterEnabled(true);
		updateFavoriteButton();
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
	
	protected void updateFavoriteButton() {
		boolean fav = favorites.contains(table.getSelected().toString());
		favoriteButton.setActionCommand(fav ? "remove" : "add");
		favoriteButton.setText((fav ? "Unf":"F") + "avorite Selected");
	}
	
	protected void update(VLCStatus status) {
		SongItem cur = gui.getRemote().getCurrentSong(status);
		if (table.getSelected() == null || !table.getSelected().equals(cur)) {
			table.setSelected(cur);
			table.scrollToSelected();
		}
	}
	
	private void switchSongToSelected(AWTEvent e) {
		
		VLCStatus s = gui.getRemote().switchSong(table.getSelected().getId());
		
		clearFilters();
		table.scrollToSelected();
		
		gui.updateInterface(s);
	}
	
	private void scrollToCurrent(AWTEvent e) {
		// TODO this
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
						show &= favorites.contains(name);
					
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
					gui.getRemote().getSongMap().values().stream()
					.map(s -> new Vector<>(Arrays.asList(s)))
					.collect(Collectors.toCollection(Vector::new));
			
			Vector<String> headers = new Vector<>(Arrays.asList(""));
			
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
		
		protected void setSelected(SongItem item) {
			for (int i=0, l=getRowCount(); i<l; i++) {
				if (getValueAt(i, 0).equals(item)) {
					table.setRowSelectionInterval(i, i);
					return;
				}
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
					((JViewport) c)
						.contains(getCellRect(r, 0, true).getLocation());
			
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
			
			// this block is only called on init of the renderer, must use different method to create the panel
			public PlaylistCellRenderer() {
				super(Constants.BORDER_LAYOUT);
				
				label = new JLabel();
				
				double downScale = getRowHeight() / ((double) SimpleIcon.ICON_SIZE);
				fav = new JLabel(SimpleIcon.FAVORITE.get(downScale));
				
				fav.setVisible(false);
				
				this.add(label, BorderLayout.WEST);
				this.add(fav, BorderLayout.EAST);
				
				this.setOpaque(true);
				
			}
			
			@Override
			public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int col) {
								
				String s = ((SongItem) v).toString();
				
				label.setText(s);
				fav.setVisible(favorites.contains(s));
				
				if (sel) {
					setBackground(t.getSelectionBackground());
					setForeground(t.getSelectionForeground());
				}
				else {
					setBackground(t.getBackground());
					setForeground(t.getForeground());
				}
				
				if (foc) {
					if (sel) setBorder(UIManager.getBorder("Table.focusSelectedCellHighlightBorder"));
					else setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
				}
				else
					setBorder(UIManager.getBorder("Table.cellNoFocusBorder"));
				
				return this;
			}
			
			// DefaultTableCellRenderer states these should be overridden to no-ops
			public void invalidate() {}
			public void validate() {}
			public void revalidate() {}
			public void repaint(long tm, int x, int y, int w, int h) {}
			public void repaint(Rectangle r) {}
			public void repaint() {}
			
			/**
			 * Direct copy of {@link DefaultTableCellRenderer#firePropertyChange(String, boolean, boolean)}
			 */
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
			
		}
	}
	
	private class PlaylistSearchListener implements DocumentListener {

		public void insertUpdate(DocumentEvent e) { changedUpdate(e); }
		public void removeUpdate(DocumentEvent e) { changedUpdate(e); }

		@Override
		public void changedUpdate(DocumentEvent e) { table.setFilterEnabled(true); }
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
