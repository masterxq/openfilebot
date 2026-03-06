package org.openfilebot;

import static org.openfilebot.Settings.*;

import java.io.File;
import java.util.Locale;

public enum ApplicationFolder {

	// real user home (the user.home will point to the application-specific container in sandbox environments)
	UserHome(isMacSandbox() ? System.getProperty("UserHome") : System.getProperty("user.home")),

	AppData(System.getProperty("application.dir", UserHome.resolve(defaultAppDataName()).getPath())),

	TemporaryFiles(System.getProperty("java.io.tmpdir")),

	Cache(System.getProperty("application.cache", AppData.resolve("cache").getPath()));

	private final File path;

	private static String defaultAppDataName() {
		String appName = getApplicationName();
		if (appName == null || appName.trim().isEmpty()) {
			return ".filebot";
		}

		String normalized = appName.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "-");
		return "." + normalized;
	}

	ApplicationFolder(String path) {
		this.path = new File(path);
	}

	public File get() {
		return path;
	}

	public File resolve(String name) {
		return new File(path, name);
	}

}
