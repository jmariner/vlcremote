package com.jmariner.vlcremote.util;

import com.jmariner.vlcremote.Main;
import lombok.extern.slf4j.Slf4j;
import net.infotrek.util.prefs.FilePreferencesFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

@SuppressWarnings("unused")
@Slf4j
public class UserSettings {
	
	private static Preferences prefs = null;
	public static String fileName = "vlcremote.prefs";
	
	private static Preferences initPreferences() {
	    System.setProperty("java.util.prefs.PreferencesFactory", SortedFilePreferencesFactory.class.getName());
	    
	    try {

			String prefsFileLocation = (new File(
					Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()
			)).getParentFile().getPath() + File.separator + fileName;

			if (!(new File(prefsFileLocation)).exists()) {
				InputStream in = Main.class.getResourceAsStream("defaults.prefs");
				if (in != null)
					Files.copy(in, Paths.get(prefsFileLocation));
			}

			System.setProperty(SortedFilePreferencesFactory.SYSTEM_PROPERTY_FILE, prefsFileLocation);

		}
		catch (URISyntaxException | IOException e) {
			e.printStackTrace();
		}

		return Preferences.userNodeForPackage(Main.class);
	}

	public static File getPrefsFile() {
		return FilePreferencesFactory.getPreferencesFile();
	}

	public static Preferences getRoot() {
		if (prefs == null) prefs = initPreferences();
		return prefs;
	}

	private static String toKey(String s) {
		
		// strip problematic characters; this is fine since we won't convert back
		s = s.replaceAll("[\\s\\.]", "");
		
		int cutoff = Preferences.MAX_KEY_LENGTH;
		if (s.length() > cutoff)
			s = s.substring(0, cutoff);

		return s;
	}

	public static void addFavorite(String s) {
		getChild("favorites").putBoolean(toKey(s), true);
	}

	public static void removeFavorite(String s) {
		getChild("favorites").remove(toKey(s));
	}

	public static boolean isFavorite(String s) {
		return getChild("favorites").getBoolean(toKey(s), false);
	}
	
	/**
	 * @see Preferences#node(String)
	 */
	public static Preferences getChild(String node) {
		return getRoot().node(node);
	}
	
	public static void remove(String key) {
		getRoot().remove(key);
	}
	
	public static String get(String key, String def) {
		return getRoot().get(key, def);
	}

	public static boolean getBoolean(String key, boolean def) {
		return getRoot().getBoolean(key, def);
	}
	
	public static int getInt(String key, int def) {
		return getRoot().getInt(key, def);
	}
	
	public static double getDouble(String key, double def) {
		return getRoot().getDouble(key, def);
	}

	public static long getLong(String key, long def) {
		return getRoot().getLong(key, def);
	}

	public static float getFloat(String key, float def) {
		return getRoot().getFloat(key, def);
	}

	public static byte[] getByteArray(String key, byte[] def) {
		return getRoot().getByteArray(key, def);
	}

	public static void put(String key, String value) {
		getRoot().put(key, value);
	}

	public static void putBoolean(String key, boolean value) {
		getRoot().putBoolean(key, value);
	}

	public static void putInt(String key, int value) {
		getRoot().putInt(key, value);
	}

	public static void putDouble(String key, double value) {
		getRoot().putDouble(key, value);
	}

	public static void putLong(String key, long value) {
		getRoot().putLong(key, value);
	}

	public static void putFloat(String key, float value) {
		getRoot().putFloat(key, value);
	}

	public static void putByteArray(String key, byte[] value) {
		getRoot().putByteArray(key, value);
	}
	
	public static boolean keyExists(String key) {
		return getRoot().get(key, null) != null;
	}

	/**
	 * Blocking/synchronous call to force the preferences file to be updated before accessing it
	 */
	public static void forceUpdate() {
		try {
			getRoot().flush();
		}
		catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	public static void viewPreferencesFile() {
		try {
			Runtime.getRuntime().exec("explorer.exe /select," + getPrefsFile().getPath());
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

}
