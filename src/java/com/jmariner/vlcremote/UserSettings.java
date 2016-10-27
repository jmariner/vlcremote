package com.jmariner.vlcremote;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import net.infotrek.util.prefs.FilePreferencesFactory;

@Slf4j
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
	
	public static void main(String[] args) throws BackingStoreException {
	//	prefs.sync();
		Stream.of(prefs.keys()).forEach(s -> {
			log.info(s + " = " + prefs.get(s, null));
		});
		
	//	prefs.put("test", "thing");
	}
}
