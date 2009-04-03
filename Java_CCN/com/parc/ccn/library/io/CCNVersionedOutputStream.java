package com.parc.ccn.library.io;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import com.parc.ccn.data.ContentName;
import com.parc.ccn.data.security.KeyLocator;
import com.parc.ccn.data.security.PublisherKeyID;
import com.parc.ccn.library.CCNFlowControl;
import com.parc.ccn.library.CCNLibrary;
import com.parc.ccn.library.CCNSegmenter;
import com.parc.ccn.library.profiles.VersioningProfile;

public class CCNVersionedOutputStream extends CCNOutputStream {

	public CCNVersionedOutputStream(ContentName name, KeyLocator locator,
			PublisherKeyID publisher, CCNLibrary library)
			throws XMLStreamException, IOException {
		this(name, locator, publisher, new CCNSegmenter(new CCNFlowControl(name, library)));
	}

	public CCNVersionedOutputStream(ContentName name, CCNLibrary library)
			throws XMLStreamException, IOException {
		this(name, null, null, library);
	}

	/**
	 * Assume if name is already versioned, the caller knows what name it
	 * wants to write. Otherwise generate a new version number for it.
	 * @param name
	 * @param locator
	 * @param publisher
	 * @param segmenter
	 * @throws XMLStreamException
	 * @throws IOException
	 */
	public CCNVersionedOutputStream(ContentName name, KeyLocator locator,
			PublisherKeyID publisher, CCNSegmenter segmenter)
			throws XMLStreamException, IOException {
		super((VersioningProfile.isVersioned(name) ? name : VersioningProfile.versionName(name)), 
				locator, publisher, segmenter);
	}

	public CCNVersionedOutputStream(ContentName name, KeyLocator locator,
			PublisherKeyID publisher, CCNFlowControl flowControl)
			throws XMLStreamException, IOException {
		this(name, locator, publisher, new CCNSegmenter(flowControl));
	}
}