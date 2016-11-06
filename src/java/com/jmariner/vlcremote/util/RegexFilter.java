package com.jmariner.vlcremote.util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class RegexFilter extends DocumentFilter {
	
	private String regex;
	
	public RegexFilter(String allowedCharactersRegex) {
		if (!allowedCharactersRegex.matches("\\[.+\\]"))
			allowedCharactersRegex = "[" + allowedCharactersRegex + "]";
		this.regex = allowedCharactersRegex;
	}
	
	@Override
	public void insertString(FilterBypass f, int offset, String text, AttributeSet attr)
			throws BadLocationException {
		if (text.matches(regex))
			super.insertString(f, offset, text, attr);
	}
	
	@Override
	public void replace(FilterBypass f, int offset, int l, String text, AttributeSet attr)
			throws BadLocationException {
		if (text.matches(regex+"+"))
			super.replace(f, offset, l, text, attr);
	}
}
