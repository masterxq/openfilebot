package org.openfilebot.platform.gnome;

import java.io.File;
import java.net.URI;

import org.openfilebot.util.SystemProperty;

public interface GVFS {

	File getPathForURI(URI uri);

	public static GVFS getDefaultVFS() {
		GVFS gvfs = SystemProperty.of("org.openfilebot.gio.GVFS", "net.filebot.gio.GVFS", path -> new PlatformGVFS(new File(path))).get();

		// default to native implementation GVFS folder is not set
		if (gvfs == null) {
			gvfs = new NativeGVFS();
		}

		return gvfs;
	}

}
