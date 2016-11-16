package com.jmariner.vlcremote.gui.playlist;

import com.jmariner.vlcremote.SongItem;
import com.jmariner.vlcremote.util.Constants;
import com.jmariner.vlcremote.util.GuiUtils;
import com.jmariner.vlcremote.util.SimpleIcon;
import com.jmariner.vlcremote.util.UserSettings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
public class PlaylistUtil {

	@Getter(AccessLevel.NONE)
	private PlaylistPanel playlist;
	@Getter(AccessLevel.NONE)
	private PlaylistTable table;

	private Filter filter;
	private Renderer renderer;
	private Model model;

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

	protected PlaylistUtil(PlaylistPanel playlist, PlaylistTable table) {
		this.playlist = playlist;
		this.table = table;

		this.filter = new Filter();
		this.renderer = new Renderer();
	}

	protected void initPost() {
		this.model = new Model();
	}

	private class CellPanel extends JPanel {
		private JLabel label, fav;

		private ImageIcon favIcon, addFavHover, removeFavIcon;

		private int hoverRow;
		private boolean hoverFav;

		private Color foreground;

		public CellPanel() {
			super(Constants.BORDER_LAYOUT);

			int size = table.getRowHeight();
			this.foreground =  UIManager.getColor("Table.foreground");

			favIcon = SimpleIcon.FAVORITE.get(size, Color.PINK);
			addFavHover = SimpleIcon.FAVORITE.get(size, foreground);
			removeFavIcon = SimpleIcon.FAVORITE_EMPTY.get(size, foreground);

			label = new JLabel();

			fav = new JLabel();
			fav.setPreferredSize(GuiUtils.squareDim(size));

			JPanel left = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
			left.setOpaque(false);
			left.add(fav);
			left.add(label);

			log.info(this.getUI().getClass().getName());
			this.putClientProperty("backgroundTexture", new Object());
			this.add(left, BorderLayout.WEST);

			this.setOpaque(true);
		}

		public JPanel update(SongItem v, boolean sel, int r, boolean editing) {

			String s = v.toString();

			label.setText(s);

			boolean isFav = UserSettings.isFavorite(s);
			fav.setIcon(isFav ? favIcon : null);

			if (sel) {
				this.setBackground(SELECTED_BACKGROUND);
				this.setForeground(SELECTED_FOREGROUND);
				this.setBorder(SELECTED_BORDER);
			}
			else {
				this.setBackground(DEFAULT_BACKGROUND);
				this.setForeground(DEFAULT_FOREGROUND);
				this.setBorder(DEFAULT_BORDER);
			}

			if (r == playlist.hoverRow) {
				// is hovering
			}
			else {
				// undo hover settings above
			}

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

		private CellPanel panel;

		public Renderer() {
			panel = new CellPanel();
		}

		@Override
		public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
			return panel.update((SongItem) v, s, r, false);
		}

		@Override
		public Component getTableCellEditorComponent(JTable t, Object v, boolean s, int r, int c) {
			return panel.update((SongItem) v, s, r, true);
		}

		@Override
		public Object getCellEditorValue() { return null; }

	}

	private class Filter extends RowFilter<TableModel, Integer> {
		@Override
		public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {

			if (!playlist.filterEnabled)
				return true;

			String filterText = playlist.searchField.getText().trim();
			String name = entry.getStringValue(0);
			boolean show = true;

			if (playlist.showFavoritesButton.isSelected())
				show = UserSettings.isFavorite(name);

			if (!filterText.isEmpty())
				show &= name.toUpperCase().contains(filterText.toUpperCase());

			return show;

		}
	}

	private class Model extends AbstractTableModel {

		private List<SongItem> songs;

		public Model() {
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
