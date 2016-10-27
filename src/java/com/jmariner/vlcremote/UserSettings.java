package com.jmariner.vlcremote;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.prefs.Preferences;

import lombok.extern.slf4j.Slf4j;
import net.infotrek.util.prefs.FilePreferences;
import net.infotrek.util.prefs.FilePreferencesFactory;

@Slf4j
public class UserSettings {
	
	private static Preferences prefs = initPreferences();
	
	private static Preferences initPreferences() {
	    System.setProperty("java.util.prefs.PreferencesFactory", FilePreferencesFactory.class.getName());	
	    
	    try {
	    	String s = new File(UserSettings.class.getResource(".").toURI()).getAbsolutePath() + File.separator + "userSettings.prefs";
	    	s = UserSettings.class.getResource(".").getFile() + File.separator + "userSettings.prefs";
	    	s = Paths.get(UserSettings.class.getResource(".").toURI()).toFile().getAbsolutePath() + File.separator + "userSettings.prefs";
			System.setProperty(FilePreferencesFactory.SYSTEM_PROPERTY_FILE, s);
		} catch (URISyntaxException e) {}
	    
	    return Preferences.userNodeForPackage(UserSettings.class);
	}
	
	public static void main(String[] args) {
		
		log.info(FilePreferencesFactory.getPreferencesFile().toString());
		
		log.info(prefs.get("key1", "default1"));
		log.info(prefs.get("key2", "default2"));
		log.info(prefs.get("key3", "default3"));
		System.out.println();
		
		Scanner s = new Scanner(System.in);
		log.info("1");
		String i1 = s.nextLine();
		log.info("2");
		String i2 = s.nextLine();
		log.info("3");
		String i3 = s.nextLine();
		
		System.out.println();
		
		if (!i1.equals("")) prefs.put("key1", i1);
		if (!i2.equals("")) prefs.put("key2", i2);
		if (!i3.equals("")) prefs.put("key3", i3);
		
		log.info(prefs.get("key1", "default1"));
		log.info(prefs.get("key2", "default2"));
		log.info(prefs.get("key3", "default3"));
		
		log.info("clear?");
		if (s.nextLine().equalsIgnoreCase("y")) {
			prefs.remove("key1");
			prefs.remove("key2");
			prefs.remove("key3");
		}
		
		s.close();//*/
	}
}
