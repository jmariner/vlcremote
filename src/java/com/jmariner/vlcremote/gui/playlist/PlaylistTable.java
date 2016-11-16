package com.jmariner.vlcremote.gui.playlist;

import com.jmariner.vlcremote.SongItem;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

public class PlaylistTable extends JTable {

	private PlaylistPanel playlist;
	
	private TableRowSorter<TableModel> sorter;
	private RowFilter<TableModel, Integer> filter;

	private PlaylistUtil util;
			
	public PlaylistTable(PlaylistPanel playlist) {
		super();

		this.playlist = playlist;
		this.util = new PlaylistUtil(playlist, this);
		
		sorter = new TableRowSorter<>();
		filter = util.getFilter();
		
		playlist.filterEnabled = false;
		
		this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.setRowSelectionAllowed(true);
		this.setColumnSelectionAllowed(true);
		this.setDefaultEditor(SongItem.class, null);
		this.setDefaultRenderer(SongItem.class, util.getRenderer());
	}
	
	protected void initPost() {

		util.initPost();

		TableModel model = util.getModel();
		this.setModel(model);
		sorter.setModel(model);

		this.setTableHeader(null);
		this.setRowSorter(sorter);
	}
	
	protected void setFilterEnabled(boolean enabled) {
		playlist.filterEnabled = enabled;
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
					setRowSelectionInterval(i, i);
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

	/**
	 * Utility method for {@link JTable#editCellAt(int, int)}
	 */
	public boolean editCellAt(int r) {
		return super.editCellAt(r, 0);
	}

/*	@Override
	public void removeEditor() {
		if (getEditingRow() != playlist.hoverRow)
			super.removeEditor();
	}*/
}