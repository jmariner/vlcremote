package com.jmariner.vlcremote;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor
@Getter
public class SongItem {
	private int id;
	private String title;
	private String artist;
	private String album;
	private int duration;

	@Override
	public String toString() {
		if (StringUtils.isNotBlank(artist))
			return String.format("%s - %s", artist, title);
		else
			return title;
	}
}
