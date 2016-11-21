package com.jmariner.vlcremote.gui.playlist;

import com.jmariner.vlcremote.SongItem;
import com.jmariner.vlcremote.util.Constants;
import com.jmariner.vlcremote.util.GuiUtils;
import com.jmariner.vlcremote.util.SVGIcon;
import com.jmariner.vlcremote.util.SimpleIcon;
import com.jmariner.vlcremote.util.UserSettings;
import lombok.AccessLevel;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class PlaylistUtil {

	@Getter(AccessLevel.NONE)
	private PlaylistPanel playlist;
	@Getter(AccessLevel.NONE)
	private PlaylistTable table;

	private Filter filter;
	private Renderer renderer;
	private Model model;
	private CellPanel cellPanel;
	
	protected static final double CELL_ICON_RATIO = .05;

	private static final Border SELECTED_BORDER 	= UIManager.getBorder("Table.focusCellHighlightBorder");
	private static final Color SELECTED_BACKGROUND 	= UIManager.getColor("Table.selectionBackground");
	private static final Color SELECTED_FOREGROUND 	= UIManager.getColor("Table.selectionForeground");
	private static final Color DEFAULT_BACKGROUND 	= UIManager.getColor("Table.background");
	private static final Color DEFAULT_FOREGROUND 	= UIManager.getColor("Table.foreground");
	private static final Border DEFAULT_BORDER;
	static {
		int i = ((LineBorder)SELECTED_BORDER).getThickness();
		DEFAULT_BORDER = BorderFactory.createEmptyBorder(i, i, i, i);
	}

	protected PlaylistUtil(PlaylistPanel playlist, PlaylistTable table) {
		this.playlist = playlist;
		this.table = table;

		this.filter = new Filter();
		this.renderer = new Renderer();
		this.cellPanel = new CellPanel();
		this.model = new Model();
	}

	protected class CellPanel extends JPanel {
		
		private JPanel leftPanel;
		private JLabel label, fav;
		
		@Getter(AccessLevel.PROTECTED)
		private JPanel hoverPanel;
		private JLabel favButton, playButton;

		private SVGIcon favIcon, addFavIcon, removeFavIcon, playIcon;

		private Color foreground;
		
		private boolean sizeSet;
		
		private final int PADDING;

		public CellPanel() {
			super(Constants.BORDER_LAYOUT);
			
			this.sizeSet = false;
			
			this.PADDING = 5;

			int size = table.getRowHeight()-2;
			this.foreground =  UIManager.getColor("Table.foreground");

			favIcon = SimpleIcon.FAVORITE.get(size, Color.PINK);
			addFavIcon = SimpleIcon.FAVORITE.get(size, foreground);
			removeFavIcon = SimpleIcon.FAVORITE_EMPTY.get(size, foreground);
			playIcon = SimpleIcon.PLAY_OUTLINE.get(size, foreground);

			label = new JLabel();
			fav = new JLabel();
			
			leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, PADDING, 0));
			leftPanel.setOpaque(false);
			leftPanel.add(fav);
			leftPanel.add(label);
			
			favButton = new JLabel(addFavIcon);
			playButton = new JLabel(playIcon);
			playButton.setToolTipText("Play this song");
			
			Arrays.asList(favButton, playButton).forEach(l -> {
				l.setVisible(false);
				l.setCursor(new Cursor(Cursor.HAND_CURSOR));
			});
			
			hoverPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, PADDING, 0));
			hoverPanel.setOpaque(false);
			hoverPanel.add(favButton);
			hoverPanel.add(playButton);
			// TODO mouse click listeners on "buttons"
			
			Arrays.asList(fav, favButton, playButton)
				.forEach(l -> l.setPreferredSize(GuiUtils.squareDim(size)));

			this.putClientProperty("backgroundTexture", new Object());
			this.add(leftPanel, BorderLayout.WEST);
			this.add(hoverPanel, BorderLayout.EAST);

			this.setOpaque(true);
		}

		public JPanel update(SongItem v, boolean sel, int r, boolean editing) {
			
			if (!sizeSet) {
				int h = table.getRowHeight();
				int w = table.getWidth();
				
				// right panel width
				int rw = (int) (w * 2*CELL_ICON_RATIO);
				hoverPanel.setPreferredSize(new Dimension(rw, h));
				leftPanel.setPreferredSize(new Dimension(w-rw, h));

				// left icon width
				int lw = (int) (w * CELL_ICON_RATIO);
				label.setPreferredSize(new Dimension(w-rw-lw-3*PADDING, h));
				
				sizeSet = true;
			}

			String s = v.toString();

			label.setText(s);

			boolean isFav = UserSettings.isFavorite(s);
			fav.setIcon(isFav ? favIcon : null);
			favButton.setIcon(isFav ? removeFavIcon : addFavIcon);
			
			if (sel) {
				this.setBackground(SELECTED_BACKGROUND);
				label.setForeground(SELECTED_FOREGROUND);
				this.setBorder(SELECTED_BORDER);
			}
			else {
				this.setBackground(DEFAULT_BACKGROUND);
				label.setForeground(DEFAULT_FOREGROUND);
				this.setBorder(DEFAULT_BORDER);
			}
			
			boolean hover = sel;
		//	boolean hover = r == playlist.hoverRow;
			favButton.setVisible(hover);
			
			boolean cur = playlist.currentSong != null && playlist.currentSong.getId() == v.getId();
			playButton.setVisible(hover || cur);
			playIcon.recolor(cur ? SELECTED_FOREGROUND : foreground);
			
			return this;
		}

		// DefaultTableCellRenderer states these should be overridden to no-ops
		@Override public void revalidate() {}
		@Override public void repaint(long t, int x, int y, int w, int h) {}
		@Override public void repaint(Rectangle r) {}
		@Override public void repaint() {}
		@Override public void firePropertyChange(String p, boolean o, boolean n) {}
	}

	private class Renderer extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
			return cellPanel.update((SongItem) v, s, r, false);
		}

		@Override
		public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
			return cellPanel.update((SongItem) v, s, r, true);
		}

		@Override
		public Object getCellEditorValue() { return null; }

	}

	private class Filter extends RowFilter<TableModel, Integer> {
		@Override
		public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {

			if (!playlist.filterEnabled)
				return true;

			String filterText = playlist.searchField.getText();
			String name = entry.getStringValue(0);
			boolean show = true;

			if (playlist.showFavoritesButton.isSelected())
				show = UserSettings.isFavorite(name);

			if (!filterText.isEmpty())
				show &= name.toUpperCase().contains(filterText.toUpperCase());

			return show;

		}
	}

	protected class Model extends AbstractTableModel {

		private List<SongItem> songs;
		
		protected void update() {
			songs = new ArrayList<>(playlist.songMap.values());
		}

		@Override
		public int getRowCount() { return songs.size(); }

		@Override
		public int getColumnCount() { return 1; }

		@Override
		public String getColumnName(int columnIndex) { return null; }

		@Override
		public Class<?> getColumnClass(int columnIndex) { return SongItem.class; }

		@Override
		public boolean isCellEditable(int r, int c) { return true; }

		@Override
		public Object getValueAt(int r, int c) { return songs.get(r); }

		@Override
		public void setValueAt(Object v, int r, int c) {}
	}

}
