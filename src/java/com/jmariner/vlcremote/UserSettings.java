package com.jmariner.vlcremote;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

import net.infotrek.util.prefs.FilePreferencesFactory;

public class UserSettings {
	
	private static Preferences prefs = initPreferences();
	
	private static Preferences initPreferences() {
	    System.setProperty("java.util.prefs.PreferencesFactory", FilePreferencesFactory.class.getName());	
	    
	    try {
	    	String s = Paths.get(UserSettings.class.getResource(".").toURI()).toFile().getAbsolutePath() + File.separator + "userSettings.prefs";
			System.setProperty(FilePreferencesFactory.SYSTEM_PROPERTY_FILE, s);
		} catch (URISyntaxException e) {}
	    
	    return Preferences.userNodeForPackage(UserSettings.class);
	}
	
	public static Preferences get() {
		return prefs;
	}
}
