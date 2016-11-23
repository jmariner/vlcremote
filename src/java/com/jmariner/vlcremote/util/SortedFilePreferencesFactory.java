package com.jmariner.vlcremote.util;

import java.io.File;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

import net.infotrek.util.prefs.FilePreferencesFactory;

/**
 * Extension of FilePreferencesFactory by David C.<br>
 * See <a href="http://www.davidc.net/programming/java/java-preferences-using-file-backing-store">http://www.davidc.net/programming/java/java-preferences-using-file-backing-store</a>
 * for details.<br>
 * Only changes including replacing instances of Properties with custom SortedProperties,
 * allowing keys to be written to file in a sorted order.
 */
public class SortedFilePreferencesFactory implements PreferencesFactory {
	private static final Logger log = Logger.getLogger(FilePreferencesFactory.class.getName());

	Preferences rootPreferences;
	public static final String SYSTEM_PROPERTY_FILE =
			"net.infotrek.util.prefs.FilePreferencesFactory.file";

	public Preferences systemRoot()
	{
		return userRoot();
	}

	public Preferences userRoot()
	{
		if (rootPreferences == null) {
			log.finer("Instantiating root preferences");

			rootPreferences = new SortedFilePreferences(null, "");
		}
		return rootPreferences;
	}

	private static File preferencesFile;

	public static File getPreferencesFile()
	{
		if (preferencesFile == null) {
			String prefsFile = System.getProperty(SYSTEM_PROPERTY_FILE);
			if (prefsFile == null || prefsFile.length() == 0) {
				prefsFile = System.getProperty("user.home") + File.separator + ".fileprefs";
			}
			preferencesFile = new File(prefsFile).getAbsoluteFile();
			log.finer("Preferences file is " + preferencesFile);
		}
		return preferencesFile;
	}
}
