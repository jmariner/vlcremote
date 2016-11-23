package com.jmariner.vlcremote.util;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.stream.Collectors;

public class SortedProperties extends Properties {
	
	public synchronized Enumeration<Object> keys() {
		return Collections.enumeration(
				super.keySet().stream() // stream the keys
				.map(Object::toString) 	// map to a string stream...
				.sorted()				// ...so they can be sorted
				.map(s -> (Object)s)	// map back to objects to satisfy return type
				.collect(Collectors.toList()));
	}
	
}
