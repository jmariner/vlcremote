package com.jmariner.vlcremote.gui.playlist;

import com.jmariner.vlcremote.SongItem;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.Collections;

@Slf4j
public class PlaylistTable extends JTable {

	private PlaylistPanel playlist;
	
	private TableRowSorter<TableModel> sorter;
	private RowFilter<TableModel, Integer> filter;

	private PlaylistUtil util;
	
	private JPanel interactiveRowComponent;
	@Getter
	private int interactiveRow;
			
	public PlaylistTable(PlaylistPanel playlist) {
		super();
		
		this.setRowHeight(22);

		this.playlist = playlist;
		this.util = new PlaylistUtil(playlist, this);
		
		sorter = new TableRowSorter<>();
		filter = util.getFilter();
		
		playlist.filterEnabled = false;
		
		this.interactiveRow = -1;
		
		this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.setRowSelectionAllowed(true);
		this.setColumnSelectionAllowed(true);
		this.setDefaultEditor(SongItem.class, null);
	//	this.setDefaultEditor(SongItem.class, util.getRenderer());
		this.setDefaultRenderer(SongItem.class, util.getRenderer());
	}
	
	protected void initPost() {

		PlaylistUtil.Model model = util.getModel();
		model.update();
		this.setModel(model);
		sorter.setModel(model);
		sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));

		interactiveRowComponent = new JPanel();
		interactiveRowComponent.setVisible(false);
		interactiveRowComponent.setBounds(getCellRect(0, 0, false));
		this.add(interactiveRowComponent);

		this.setTableHeader(null);
		this.setRowSorter(sorter);
	}
	
	protected void setFilterEnabled(boolean enabled) {
		playlist.filterEnabled = enabled;
		sorter.setRowFilter(filter);
	}

	/**
	 * Gets the table row of the <code>SongItem</code>. This is affected by filters.
	 * @param item the item to find
	 * @return row where <code>item</code> is located or <code>-1</code> if it isn't found
	 */
	protected int getRowOf(SongItem item) {
		int r = -1;
		while (true) {
			try {
				if (getValueAt(++r, 0).equals(item)) return r;
			}
			catch (IndexOutOfBoundsException e) { return -1; }
		}
	}
	
	protected void setSelected(SongItem item) {
		int r = -1;
		while (true) {
			try {
				if (getValueAt(++r, 0).equals(item)) {
					setRowSelectionInterval(r, r);
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
	
	protected void scrollToSelected() {
		int selected = getSelectedRow();

		if (selected == -1) return;
		
		Rectangle rect = getCellRect(selected, 0, true);
		JViewport view = (JViewport) getParent();
		Point pt = view.getViewPosition();

		int around = (view.getHeight()-rect.height)/2;
		
		// create spacing before and after selection so it is somewhat center
		rect.setLocation(rect.x, rect.y - around);
		rect.setSize(rect.width, around + rect.height + around);
		
		// account for the viewport position
		rect.setLocation(rect.x - pt.x, rect.y - pt.y);
		
		view.scrollRectToVisible(rect);
	}
	
	protected JViewport getViewport() {
		return (JViewport) getParent();
	}
	
	protected void repaintViewArea() {
		JViewport view = getViewport();
		this.repaint(0, view.getViewPosition().y, view.getWidth(), view.getHeight());
	}
	
	protected void repaintHoverArea() {
		Rectangle hover = util.getCellPanel().getHoverPanel().getBounds();
		JViewport view = getViewport();
		this.repaint(hover.x, view.getViewPosition().y, hover.width, view.getHeight());
	}
	
	protected void setInteractiveRow(int r) {
		if (r > -1 && r < getRowCount()) {

		//	disableInteractiveRow();
			
			interactiveRowComponent = util.getCellPanel().update(this, r, true);

			this.repaint(getCellRect(interactiveRow, 0, false));
			this.remove(interactiveRowComponent);
			interactiveRowComponent.setBounds(getCellRect(r, 0, false));
			this.add(interactiveRowComponent);
			interactiveRowComponent.setVisible(true);
			this.revalidate();
			interactiveRowComponent.revalidate();
			interactiveRowComponent.repaint();

			this.interactiveRow = r;
			
		}
	}
	
	protected void disableInteractiveRow() {
		if (interactiveRowEnabled()) {
			this.remove(interactiveRowComponent);

			this.repaint(getCellRect(interactiveRow, 0, false));

			this.interactiveRowComponent = null;
			this.interactiveRow = -1;
		}
	}
	
	protected boolean interactiveRowEnabled() {
		return interactiveRowComponent.isVisible();
	}
}