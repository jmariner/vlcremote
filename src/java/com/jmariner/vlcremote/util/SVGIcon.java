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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;

public class SVGIcon extends ImageIcon {

	private static final SAXSVGDocumentFactory FACTORY
			= new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());

	private Document doc;

	private int size;
	private Color color;

	protected SVGIcon(String iconName, int size, Color color) throws URISyntaxException, IOException {
		this(getDocument(iconName), size, color);
	}

	private SVGIcon(Document doc, int size, Color color) {

		this.doc = doc;
		this.size = size;
		this.color = color;

		prepare();
	}

	protected void prepare() {

		String colorString = String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
		doc.getDocumentElement().setAttribute("fill", colorString);
		SVGIconTranscoder transcoder = new SVGIconTranscoder(size);

		try {
			transcoder.transcode(new TranscoderInput(doc), null);
			setImage(transcoder.getImage());

		} catch (TranscoderException e) {
			e.printStackTrace();
		}
	}

	public SVGIcon resize(int size) {
		return new SVGIcon(doc, size, color);
	}

	public SVGIcon recolor(Color color) {
		return new SVGIcon(doc, size, color);
	}

	public SVGIcon resizeAndRecolor(int size, Color color) {
		return new SVGIcon(doc, size, color);
	}

	private static Document getDocument(String iconName) {
		try {
			String uri = Main.class.getResource(String.format("icons/%s.svg", iconName)).toURI().toString();
			return FACTORY.createDocument(uri);
		} catch (URISyntaxException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private class SVGIconTranscoder extends ImageTranscoder {

		@Getter
		private BufferedImage image;

		public SVGIconTranscoder(int width) {
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
