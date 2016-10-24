package com.jmariner.vlcremote;

import lombok.extern.slf4j.Slf4j;

import java.util.Scanner;

import static com.jmariner.vlcremote.MyVLCRemote.Command.*;

@Slf4j
public class Main {

	public static void main(String[] args) throws Exception {

		MyVLCRemote remote = new MyVLCRemote("home.jmariner.com", 8080, new Scanner(System.in).nextLine(), 8081);
		remote.sendCommand(PLAY);
		remote.playStream();

	}

}