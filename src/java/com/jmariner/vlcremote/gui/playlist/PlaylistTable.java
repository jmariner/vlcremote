/*package com.jmariner.vlcremote.gui.playlist;

import static com.jmariner.vlcremote.util.Constants.MAIN_PADDING;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Collections;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.RowFilter.Entry;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.jmariner.vlcremote.SongItem;
import com.jmariner.vlcremote.gui.playlist.PlaylistPanel.PlaylistTable;
import com.jmariner.vlcremote.gui.playlist.PlaylistPanel.PlaylistTable.PlaylistCellRenderer;
import com.jmariner.vlcremote.util.Constants;
import com.jmariner.vlcremote.util.SimpleIcon;

class PlaylistTable extends JTable {
	
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
		
		*//**
		 * Direct copy of {@link DefaultTableCellRenderer#firePropertyChange(String, Object, Object)}
		 *//*
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
}*/