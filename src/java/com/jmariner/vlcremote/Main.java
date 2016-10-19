package com.jmariner.vlcremote;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

	public static void main(String[] args) throws Exception {
		
		//*
		GlobalHotkeyListener g = new GlobalHotkeyListener();

		g.registerHotkey("meta y", () -> {
			log.info("worked!");
		});//*/
	}

}