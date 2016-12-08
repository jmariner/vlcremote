package com.jmariner.vlcremote.util;

import com.google.common.collect.ImmutableMap;
import com.jmariner.vlcremote.Main;
import lombok.Getter;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.SVGConstants;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;

import javax.swing.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class SVGIcon extends ImageIcon {

	private static final SAXSVGDocumentFactory FACTORY
			= new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());
	
	private static SVGIconTranscoder transcoder =
			new SVGIcon().new SVGIconTranscoder();
	
	private static Map<String, Image> cache = new HashMap<>();

	private String name;
	
	private Document doc;

	@Getter
	private int size;
	@Getter
	private Color color;

	protected SVGIcon(String iconName, int size, Color color) {
		this(iconName, size, color, null);
	}

	private SVGIcon(String iconName, int size, Color color, Component parentComponent) {
		this.name = iconName;
		this.doc = getDocument(iconName);
		this.size = size;
		this.color = color;
		
		if (parentComponent != null)
			setParentComponent(parentComponent);

		update();
	}
	
	private SVGIcon() {}

	private void update() {
		
		if (doc == null) return;

		Image fromCache = cache.get(this.toString());
		if (fromCache != null) {
			setImage(fromCache);
			return;
		}

		String colorString = String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
		doc.getDocumentElement().setAttribute("fill", colorString);
		transcoder.update(size);

		try {
			transcoder.transcode(new TranscoderInput(doc), null);
			setImage(transcoder.getImage());
			cache.put(this.toString(), this.getImage());

		} catch (TranscoderException e) {
			e.printStackTrace();
		}
	}

	public void resize(int size) {
		resizeAndRecolor(size, this.color);
	}

	public void recolor(Color color) {
		resizeAndRecolor(this.size, color);
	}

	public void resizeAndRecolor(int size, Color color) {
		this.size = size;
		this.color = color;
		update();
	}
	
	public void setParentComponent(Component c) {
		c.addPropertyChangeListener("foreground", e -> {
			recolor((Color) e.getNewValue());
		});
		
		if (c.getWidth() > 0) resizeForComponent(c);
		
		c.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				resizeForComponent(c);
			}
		});
	}
	
	private void resizeForComponent(Component c) {
		resize((int) (c.getWidth() / SimpleIcon.Defaults.BUTTON_ICON_RATIO));
	}

	protected static Document getDocument(String iconName) {
		try {
			String uri = Main.class.getResource(String.format("icons/%s.svg", iconName)).toURI().toString();
			return FACTORY.createDocument(uri);
		} catch (URISyntaxException | IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String toString() {
		return String.format("SVGIcon[name=%s,size=%d,color=%s]",
				this.name, this.size, this.color.toString());
	}
	
	public boolean equals(SVGIcon other) {
		return this.name.equals(other.name) &&
				this.size == other.size &&
				this.color.equals(other.color);
	}

	private class SVGIconTranscoder extends ImageTranscoder {

		@Getter
		private BufferedImage image;
		
		public void update(int width) {
			setTranscodingHints(ImmutableMap.of(
					KEY_DOM_IMPLEMENTATION, SVGDOMImplementation.getDOMImplementation(),
					KEY_DOCUMENT_ELEMENT_NAMESPACE_URI, SVGConstants.SVG_NAMESPACE_URI,
					KEY_DOCUMENT_ELEMENT, SVGConstants.SVG_SVG_TAG,
					KEY_WIDTH, (float) width
			));
		}

		@Override
		public BufferedImage createImage(int width, int height) {
			image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			return image;
		}

		@Override
		public void writeImage(BufferedImage i, TranscoderOutput o) throws TranscoderException {}
	}
}
