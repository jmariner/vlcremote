package com.jmariner.vlcremote.gui.playlist;

import com.google.common.collect.ImmutableMap;
import com.jmariner.vlcremote.SongItem;

import lombok.Getter;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

public class PlaylistTable extends JTable {

	private PlaylistPanel playlist;
	
	private TableRowSorter<TableModel> sorter;
	private RowFilter<TableModel, Integer> filter;

	private PlaylistUtil util;
	
	private JPanel interactiveRowComponent;
	@Getter
	private int interactiveRow;
			
			
	private static final List<RowSorter.SortKey> ASCENDING_SORT =
			Collections.singletonList(new RowSorter.SortKey(0, SortOrder.ASCENDING));
	private static final List<RowSorter.SortKey> DESCENDING_SORT =
			Collections.singletonList(new RowSorter.SortKey(0, SortOrder.DESCENDING));
	
	private static final LinkedHashMap<String, Comparator<SongItem>> COMPARATORS =
			new LinkedHashMap<>();
	
	protected static final List<String> ORDERS;
	
	static {
		COMPARATORS.put("name", (s1, s2) -> s1.toString().compareTo(s2.toString()));
		COMPARATORS.put("title", (s1, s2) -> s1.getTitle().compareTo(s2.getTitle()));
		COMPARATORS.put("duration", (s1, s2) -> new Integer(s1.getId()).compareTo(new Integer(s2.getId())));
		ORDERS = new ArrayList<>(COMPARATORS.keySet());
	}
			
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
		this.setDefaultRenderer(SongItem.class, util.getRenderer());
	}
	
	protected void initPost() {

		PlaylistUtil.Model model = util.getModel();
		model.update();
		this.setModel(model);
		sorter.setModel(model);
		sortBy("name", true);
		
		this.setTableHeader(null);
		this.setRowSorter(sorter);
	}
	
	protected void setFilterEnabled(boolean enabled) {
		playlist.filterEnabled = enabled;
		sorter.setRowFilter(filter);
	}
	
	protected void sortBy(String type, boolean ascending) {
		assert COMPARATORS.containsKey(type);
	//	sorter.setComparator(0, COMPARATORS.get(type));
		sorter.setSortKeys(ascending ? ASCENDING_SORT : DESCENDING_SORT);
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
				
			disableInteractiveRow();
			
			interactiveRowComponent = util.getCellPanel().update(this, r, true);
			
			interactiveRowComponent.setBounds(getCellRect(r, 0, false));
			this.add(interactiveRowComponent);
			interactiveRowComponent.revalidate();
			interactiveRowComponent.repaint();

			this.interactiveRow = r;
						
		}
	}
	
	protected void disableInteractiveRow() {
		if (interactiveRowEnabled()) {
			this.repaint(getCellRect(interactiveRow, 0, false));
			this.remove(interactiveRowComponent);
			this.interactiveRowComponent = null;
			this.interactiveRow = -1;
		}
	}
	
	protected boolean interactiveRowEnabled() {
		return interactiveRowComponent != null;
	}
}