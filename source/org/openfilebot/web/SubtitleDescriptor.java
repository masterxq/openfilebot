
package org.openfilebot.web;


import java.nio.ByteBuffer;

import org.openfilebot.vfs.FileInfo;


public interface SubtitleDescriptor extends FileInfo {

	@Override
	String getName();


	String getLanguageName();


	@Override
	String getType();


	ByteBuffer fetch() throws Exception;

}
