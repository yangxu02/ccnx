package com.parc.ccn.data.util;

import java.io.IOException;
import java.sql.Timestamp;

import javax.xml.stream.XMLStreamException;

import com.parc.ccn.Library;
import com.parc.ccn.data.ContentName;
import com.parc.ccn.data.ContentObject;
import com.parc.ccn.data.security.SignedInfo.ContentType;
import com.parc.ccn.library.CCNFlowControl;
import com.parc.ccn.library.CCNLibrary;
import com.parc.ccn.library.io.CCNInputStream;
import com.parc.ccn.library.io.CCNVersionedInputStream;
import com.parc.ccn.library.io.CCNVersionedOutputStream;
import com.parc.ccn.library.profiles.VersionMissingException;
import com.parc.ccn.library.profiles.VersioningProfile;

public abstract class CCNNetworkObject<E> extends NetworkObject<E> {

	protected ContentName _currentName;
	protected CCNLibrary _library;
	protected CCNFlowControl _flowControl;
	protected boolean _isGone = false;

	public CCNNetworkObject(Class<E> type) {
		super(type);
	}

	public CCNNetworkObject(Class<E> type, E data) {
		super(type, data);
	}

	public void update() throws XMLStreamException, IOException {
		if (null == _currentName) {
			throw new IllegalStateException("Cannot retrieve an object without giving a name!");
		}
		// Look for latest version.
		update(VersioningProfile.versionRoot(_currentName));
	}

	/**
	 * Load data into object. If name is versioned, load that version. If
	 * name is not versioned, look for latest version. CCNInputStream doesn't
	 * have that property at the moment.
	 * @param name
	 * @throws IOException 
	 * @throws XMLStreamException 
	 * @throws ClassNotFoundException 
	 */
	public void update(ContentName name) throws XMLStreamException, IOException {
		Library.logger().info("Updating object to " + name);
		CCNVersionedInputStream is = new CCNVersionedInputStream(name, _library);
		update(is);
	}

	public void update(ContentObject object) throws XMLStreamException, IOException {
		CCNInputStream is = new CCNInputStream(object, _library);
		update(is);
	}

	public void update(CCNInputStream inputStream) throws IOException, XMLStreamException {
		try {
			super.update(inputStream);
		} catch (IOException ioex) {
			Library.logger().info("update: got IOException " + ioex.getMessage() + " is data actually just gone? " + inputStream.isGone());
			if (inputStream.isGone()) {
				_isGone = true;
				_currentName = inputStream.deletionInformation().name();
			} else {
				throw ioex;
			}
		} catch (XMLStreamException xsex) {
			Library.logger().info("update: got XMLStreamException " + xsex.getMessage() + " is data actually just gone? " + inputStream.isGone());
			if (inputStream.isGone()) {
				_isGone = true;
				_currentName = inputStream.deletionInformation().name();
			} else {
				throw xsex;
			}
		}
		_currentName = inputStream.baseName();
		_flowControl.addNameSpace(_currentName);
	}

	/**
	 * Save to existing name, if content is dirty. Update version.
	 * @throws IOException 
	 * @throws XMLStreamException 
	 */
	public void save() throws XMLStreamException, IOException {
		if (null == _currentName) {
			throw new IllegalStateException("Cannot save an object without giving it a name!");
		}
		save(VersioningProfile.versionName(_currentName));
	}

	/**
	 * Save content to specific name. If versioned, assume that is the desired
	 * version. If not, add a version to it.
	 * @param name
	 * @throws IOException 
	 * @throws XMLStreamException 
	 */
	public void save(ContentName name) throws XMLStreamException, IOException {
		// move object to this name
		// TODO
		// need to make sure we get back the actual name we're using,
		// even if output stream does automatic versioning
		// probably need to refactor save behavior -- right now, internalWriteObject
		// either writes the object or not; we need to only make a new name if we do
		// write the object, and figure out if that's happened. Also need to make
		// parent behavior just write, put the dirty check higher in the state.

		if (!isDirty()) { // Should we check potentially dirty?
			Library.logger().info("Object not dirty. Not saving.");
			return;
		}
		if (null == name) {
			throw new IllegalStateException("Cannot save an object without giving it a name!");
		}
		_flowControl.addNameSpace(name);
		// CCNVersionedOutputStream will version an unversioned name. 
		// If it gets a versioned name, will respect it.
		CCNVersionedOutputStream cos = new CCNVersionedOutputStream(name, null, null, _flowControl);
		save(cos); // superclass stream save. calls flush but not close on a wrapping
		// digest stream; want to make sure we end up with a single non-MHT signed
		// block and no header on small objects
		cos.close();
		_currentName = cos.getBaseName();
		setPotentiallyDirty(false);
	}
	
	/**
	 * Save this object as GONE. Intended to mark the latest version, rather
	 * than a specific version as GONE. So for now, require that name handed in
	 * is *not* already versioned; throw an IOException if it is.
	 * @param name
	 * @throws IOException
	 */
	public void saveAsGone(ContentName name) throws IOException {
		
		if (VersioningProfile.isVersioned(name)) {
			throw new IOException("Cannot save past versions as gone!");
		}
		
		ContentName versionedName = VersioningProfile.versionName(name);
		ContentObject goneObject = ContentObject.buildContentObject(name, ContentType.GONE, null);
		_flowControl.addNameSpace(name);
		_flowControl.put(goneObject);
		_currentName = versionedName;
		setPotentiallyDirty(false);
	}

	public Timestamp getVersion() {
		if ((null == _currentName) || (null == _lastSaved)) {
			return null;
		}
		try {
			return VersioningProfile.getVersionAsTimestamp(_currentName);
		} catch (VersionMissingException e) {
			return null;
		}
	}

	public ContentName getName() {
		return _currentName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
		+ ((_currentName == null) ? 0 : _currentName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		CCNNetworkObject<?> other = (CCNNetworkObject<?>) obj;
		if (_currentName == null) {
			if (other._currentName != null)
				return false;
		} else if (!_currentName.equals(other._currentName))
			return false;
		return true;
	}

	public boolean isGone() {
		return _isGone;
	}
}