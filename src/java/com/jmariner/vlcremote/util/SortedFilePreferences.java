package com.jmariner.vlcremote.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

import net.infotrek.util.prefs.FilePreferences;
import net.infotrek.util.prefs.FilePreferencesFactory;

/**
 * Extension of FilePreferences by David C.<br>
 * See <a href="http://www.davidc.net/programming/java/java-preferences-using-file-backing-store">http://www.davidc.net/programming/java/java-preferences-using-file-backing-store</a>
 * for details.<br>
 * Only changes including replacing instances of Properties with custom SortedProperties,
 * allowing keys to be written to file in a sorted order.
 */
public class SortedFilePreferences extends AbstractPreferences {

	private static final Logger log = Logger.getLogger(FilePreferences.class.getName());

	private Map<String, String> root;
	private Map<String, SortedFilePreferences> children;
	private boolean isRemoved = false;

	public SortedFilePreferences(AbstractPreferences parent, String name)
	{
		super(parent, name);

		log.finest("Instantiating node " + name);

		root = new TreeMap<>();
		children = new TreeMap<>();

		try {
			sync();
		}
		catch (BackingStoreException e) {
			log.log(Level.SEVERE, "Unable to sync on creation of node " + name, e);
		}
	}

	protected void putSpi(String key, String value)
	{
		root.put(key, value);
		try {
			flush();
		}
		catch (BackingStoreException e) {
			log.log(Level.SEVERE, "Unable to flush after putting " + key, e);
		}
	}

	protected String getSpi(String key)
	{
		return root.get(key);
	}

	protected void removeSpi(String key)
	{
		root.remove(key);
		try {
			flush();
		}
		catch (BackingStoreException e) {
			log.log(Level.SEVERE, "Unable to flush after removing " + key, e);
		}
	}

	protected void removeNodeSpi() throws BackingStoreException
	{
		isRemoved = true;
		flush();
	}

	protected String[] keysSpi() throws BackingStoreException
	{
		return root.keySet().toArray(new String[root.keySet().size()]);
	}

	protected String[] childrenNamesSpi() throws BackingStoreException
	{
		return children.keySet().toArray(new String[children.keySet().size()]);
	}

	protected SortedFilePreferences childSpi(String name)
	{
		SortedFilePreferences child = children.get(name);
		if (child == null || child.isRemoved()) {
			child = new SortedFilePreferences(this, name);
			children.put(name, child);
		}
		return child;
	}


	protected void syncSpi() throws BackingStoreException
	{
		if (isRemoved()) return;

		final File file = FilePreferencesFactory.getPreferencesFile();

		if (!file.exists()) return;

		synchronized (file) {
			SortedProperties p = new SortedProperties();
			try {
				p.load(new FileInputStream(file));

				StringBuilder sb = new StringBuilder();
				getPath(sb);
				String path = sb.toString();

				final Enumeration<?> pnen = p.propertyNames();
				while (pnen.hasMoreElements()) {
					String propKey = (String) pnen.nextElement();
					if (propKey.startsWith(path)) {
						String subKey = propKey.substring(path.length());
						// Only load immediate descendants
						if (subKey.indexOf('.') == -1) {
							root.put(subKey, p.getProperty(propKey));
						}
					}
				}
			}
			catch (IOException e) {
				throw new BackingStoreException(e);
			}
		}
	}

	private void getPath(StringBuilder sb)
	{
		final SortedFilePreferences parent = (SortedFilePreferences) parent();
		if (parent == null) return;

		parent.getPath(sb);
		sb.append(name()).append('.');
	}

	protected void flushSpi() throws BackingStoreException
	{
		final File file = FilePreferencesFactory.getPreferencesFile();

		synchronized (file) {
			SortedProperties p = new SortedProperties();
			try {

				StringBuilder sb = new StringBuilder();
				getPath(sb);
				String path = sb.toString();

				if (file.exists()) {
					p.load(new FileInputStream(file));

					List<String> toRemove = new ArrayList<String>();

					// Make a list of all direct children of this node to be removed
					final Enumeration<?> pnen = p.propertyNames();
					while (pnen.hasMoreElements()) {
						String propKey = (String) pnen.nextElement();
						if (propKey.startsWith(path)) {
							String subKey = propKey.substring(path.length());
							// Only do immediate descendants
							if (subKey.indexOf('.') == -1) {
								toRemove.add(propKey);
							}
						}
					}

					// Remove them now that the enumeration is done with
					for (String propKey : toRemove) {
						p.remove(propKey);
					}
				}

				// If this node hasn't been removed, add back in any values
				if (!isRemoved) {
					for (String s : root.keySet()) {
						p.setProperty(path + s, root.get(s));
					}
				}

				p.store(new FileOutputStream(file), "FilePreferences");
			}
			catch (IOException e) {
				throw new BackingStoreException(e);
			}
		}
	}
}
