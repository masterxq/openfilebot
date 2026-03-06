package org.openfilebot;

import static java.awt.GraphicsEnvironment.*;
import static java.util.stream.Collectors.*;
import static org.openfilebot.Logging.*;
import static org.openfilebot.Settings.*;
import static org.openfilebot.util.FileUtilities.*;
import static org.openfilebot.util.XPathUtilities.*;
import static org.openfilebot.util.ui.SwingUI.*;

import java.awt.Dialog.ModalityType;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.logging.Handler;
import java.util.logging.Level;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.kohsuke.args4j.CmdLineException;

import org.openfilebot.cli.ArgumentBean;
import org.openfilebot.cli.ArgumentProcessor;
import org.openfilebot.format.ExpressionFormat;
import org.openfilebot.platform.mac.MacAppUtilities;
import org.openfilebot.ui.FileBotMenuBar;
import org.openfilebot.ui.MainFrame;
import org.openfilebot.ui.NotificationHandler;
import org.openfilebot.ui.PanelBuilder;
import org.openfilebot.ui.SinglePanelFrame;
import org.openfilebot.ui.transfer.FileTransferable;
import org.openfilebot.util.JsonUtilities;
import org.openfilebot.util.ui.SwingEventBus;
import net.miginfocom.swing.MigLayout;

public class Main {

	public static void main(String[] argv) {
		try {
			// parse arguments
			ArgumentBean args = new ArgumentBean(argv);

			// just print help message or version string and then exit
			if (args.printHelp()) {
				log.info(String.format("%s%n%n%s", getApplicationIdentifier(), args.usage()));
				System.exit(0);
			}

			if (args.printVersion()) {
				log.info(String.join(" / ", getApplicationIdentifier(), getJavaRuntimeIdentifier(), getSystemIdentifier()));
				System.exit(0);
			}

			if (args.clearCache() || args.clearUserData()) {
				// clear persistent user preferences
				if (args.clearUserData()) {
					log.info("Reset preferences");
					Settings.forPackage(Main.class).clear();
				}

				// clear caches
				if (args.clearCache()) {
					// clear cache must be called manually
					if (System.console() == null) {
						log.severe("`filebot -clear-cache` has been disabled due to abuse.");
						System.exit(1);
					}

					log.info("Clear cache");
					for (File folder : getChildren(ApplicationFolder.Cache.get(), FOLDERS)) {
						log.fine("* Delete " + folder);
						delete(folder);
					}
				}

				// just clear cache and/or settings and then exit
				System.exit(0);
			}

			// make sure we can access application arguments at any time
			setApplicationArguments(args);

			// update system properties
			initializeSystemProperties(args);
			initializeLogging(args);

			// initialize this stuff before anything else
			CacheManager.getInstance();
			initializeSecurityManager();

			// initialize history spooler
			HistorySpooler.getInstance().setPersistentHistoryEnabled(useRenameHistory());

			// CLI mode => run command-line interface and then exit
			if (args.runCLI()) {
				int status = new ArgumentProcessor().run(args);
				System.exit(status);
			}

			if (isHeadless()) {
				log.info(String.format("%s / %s (headless)%n%n%s", getApplicationIdentifier(), getJavaRuntimeIdentifier(), args.usage()));
				System.exit(1);
			}

			// GUI mode => start user interface
			SwingUtilities.invokeLater(() -> {
				startUserInterface(args);

				// run background tasks
				newSwingWorker(() -> onStart(args)).execute();
			});
		} catch (CmdLineException e) {
			// illegal arguments => print CLI error message
			log.severe(e::getMessage);
			System.exit(1);
		} catch (Throwable e) {
			// unexpected error => dump stack
			debug.log(Level.SEVERE, "Error during startup", e);
			System.exit(1);
		}
	}

	private static void onStart(ArgumentBean args) {
		// publish file arguments
		List<File> files = args.getFiles(false);
		if (files.size() > 0) {
			SwingEventBus.getInstance().post(new FileTransferable(files));
		}

		// JavaFX is used for ProgressMonitor
		try {
			initJavaFX();
		} catch (Throwable e) {
			log.log(Level.SEVERE, "Failed to initialize JavaFX. Please install JavaFX.", e);
		}

		// check for application updates
		if (!"skip".equals(System.getProperty("application.update"))) {
			try {
				checkUpdate();
			} catch (Throwable e) {
				debug.log(Level.WARNING, "Failed to check for updates", e);
			}
		}
	}

	private static void startUserInterface(ArgumentBean args) {
		// use FlatLaf for all platforms
		setNimbusLookAndFeel();

		// start multi panel or single panel frame
		PanelBuilder[] panels = args.getPanelBuilders();
		JFrame frame = panels.length > 1 ? new MainFrame(panels) : new SinglePanelFrame(panels[0]);

		try {
			restoreWindowBounds(frame, Settings.forPackage(MainFrame.class)); // restore previous size and location
		} catch (Exception e) {
			frame.setLocation(120, 80); // make sure the main window is not displayed out of screen bounds
		}

		frame.addWindowListener(windowClosed(evt -> {
			evt.getWindow().setVisible(false);

			// make sure any long running operations are done now and not later on the shutdown hook thread
			HistorySpooler.getInstance().commit();

			System.exit(0);
		}));

		// configure main window
		if (isMacApp()) {
			// Mac specific configuration
			MacAppUtilities.initializeApplication(FileBotMenuBar.createHelp(), files -> SwingEventBus.getInstance().post(new FileTransferable(files)));
		} else if (isUbuntuApp()) {
			// Ubuntu/Debian specific configuration
			frame.setIconImages(ResourceManager.getApplicationIcons());
		} else if (isWindowsApp()) {
			// Windows specific configuration
			frame.setIconImages(ResourceManager.getApplicationIcons());
		} else {
			// generic Linux/FreeBSD/Solaris configuration
			frame.setIconImages(ResourceManager.getApplicationIcons());
		}

		// start application
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
	}

	/**
	 * Show update notifications if updates are available
	 */
	private static void checkUpdate() throws Exception {
		Cache cache = Cache.getCache(getApplicationName(), CacheType.Persistent);
		Map<?, ?> update = JsonUtilities.asMap(cache.json("update.url", s -> new URL(getApplicationProperty(s))).expire(Cache.ONE_WEEK).retry(0).get());

		String currentVersion = getApplicationVersion();
		String latestVersion = Optional.ofNullable(JsonUtilities.getString(update, "tag_name")).orElse(JsonUtilities.getString(update, "name"));

		if (compareVersion(latestVersion, currentVersion) > 0) {
			String discussion = Optional.ofNullable(JsonUtilities.getString(update, "html_url")).orElse(getApplicationProperty("link.mws"));
			String download = selectDownloadUrl(update, latestVersion, discussion);
			String title = String.format("Update Available: %s", latestVersion);
			String message = String.format("A new version is available (%s → %s).", currentVersion, latestVersion);

			SwingUtilities.invokeLater(() -> {
				JDialog dialog = new JDialog(JFrame.getFrames()[0], title, ModalityType.APPLICATION_MODAL);
				JPanel pane = new JPanel(new MigLayout("fill, nogrid, insets dialog"));
				dialog.setContentPane(pane);

				pane.add(new JLabel(ResourceManager.getIcon("window.icon.medium")), "aligny top");
				pane.add(new JLabel(message), "aligny top, gap 10, wrap paragraph:push");

				pane.add(newButton("Download", ResourceManager.getIcon("dialog.continue"), evt -> {
					try {
						File defaultDir = new File(System.getProperty("user.home"), "Downloads");
						if (!defaultDir.isDirectory()) {
							defaultDir = new File(System.getProperty("user.home"));
						}

						String filename = "openfilebot_" + latestVersion + ".jar";
						try {
							String path = new URL(download).getPath();
							filename = path.substring(path.lastIndexOf('/') + 1);
						} catch (Exception e) {
							// ignore
						}

						File defaultSaveFile = new File(defaultDir, filename);
						File targetFile = UserFiles.showSaveDialogSelectFile(false, defaultSaveFile, "Download Update", evt);

						if (targetFile != null) {
							dialog.setVisible(false);

							org.openfilebot.util.ui.ProgressMonitor.runTask("Downloading Update", targetFile.getName(), (progressMessage, progress, cancelled) -> {
								progressMessage.accept("Connecting...");
								java.net.URLConnection c = new URL(download).openConnection();
								long length = c.getContentLengthLong();

								try (java.io.InputStream in = c.getInputStream(); java.io.OutputStream out = new java.io.FileOutputStream(targetFile)) {
									byte[] buffer = new byte[8192];
									long totalRead = 0;
									int read;
									while ((read = in.read(buffer)) >= 0) {
										if (cancelled.get()) {
											targetFile.delete();
											return null;
										}
										out.write(buffer, 0, read);
										totalRead += read;
										if (length > 0) {
											progress.accept(totalRead, length);
											progressMessage.accept(String.format("Downloading... %d / %d MB", totalRead / 1024 / 1024, length / 1024 / 1024));
										} else {
											progressMessage.accept(String.format("Downloading... %d MB", totalRead / 1024 / 1024));
										}
									}
								}
								return targetFile;
							});
						}
					} catch (Exception e) {
						// fallback if anything fails
						openURI(download);
						dialog.setVisible(false);
					}
				}), "tag ok");

				pane.add(newButton("Details", ResourceManager.getIcon("action.report"), evt -> {
					openURI(discussion);
				}), "tag help2");

				pane.add(newButton("Ignore", ResourceManager.getIcon("dialog.cancel"), evt -> {
					dialog.setVisible(false);
				}), "tag cancel");

				dialog.pack();
				dialog.setLocation(getOffsetLocation(dialog.getOwner()));
				dialog.setVisible(true);
			});
		}
	}

	private static int compareVersion(String left, String right) {
		int[] a = parseVersion(left);
		int[] b = parseVersion(right);

		int length = Math.max(a.length, b.length);
		for (int i = 0; i < length; i++) {
			int x = i < a.length ? a[i] : 0;
			int y = i < b.length ? b[i] : 0;
			if (x != y) {
				return Integer.compare(x, y);
			}
		}

		return 0;
	}

	private static int[] parseVersion(String value) {
		if (value == null) {
			return new int[0];
		}

		return Pattern.compile("\\d+").matcher(value).results().mapToInt(m -> Integer.parseInt(m.group())).toArray();
	}

	private static String selectDownloadUrl(Map<?, ?> update, String latestVersion, String fallbackUrl) {
		String deployment = Optional.ofNullable(getApplicationDeployment()).orElse("jar").toLowerCase(Locale.ROOT);
		String version = Optional.ofNullable(latestVersion).orElse("").toLowerCase(Locale.ROOT);
		String preferredDebArch = getPreferredDebArch();

		int bestScore = Integer.MIN_VALUE;
		String bestUrl = fallbackUrl;

		for (Map<?, ?> asset : JsonUtilities.streamJsonObjects(update, "assets").collect(toList())) {
			String name = Optional.ofNullable(JsonUtilities.getString(asset, "name")).orElse("").toLowerCase(Locale.ROOT);
			String url = JsonUtilities.getString(asset, "browser_download_url");

			if (url == null || name.isEmpty()) {
				continue;
			}

			if (name.endsWith(".asc") || name.endsWith(".changes")) {
				continue;
			}

			int score = 0;
			if (!version.isEmpty() && name.contains(version)) {
				score += 40;
			}

			if ("deb".equals(deployment)) {
				if (!name.endsWith(".deb"))
					continue;
				score += 100;
				if (preferredDebArch != null && name.contains("_" + preferredDebArch + ".deb")) {
					score += 50;
				}
			} else if ("msi".equals(deployment)) {
				if (!name.endsWith(".msi"))
					continue;
				score += 100;
				if (is64BitArch() && (name.contains("x64") || name.contains("amd64"))) {
					score += 20;
				}
				if (!is64BitArch() && (name.contains("x86") || name.contains("i386"))) {
					score += 20;
				}
			} else if ("appx".equals(deployment)) {
				if (!(name.endsWith(".appx") || name.endsWith(".msix")))
					continue;
				score += 100;
			} else if ("spk".equals(deployment)) {
				if (!name.endsWith(".spk"))
					continue;
				score += 100;
			} else if ("app".equals(deployment) || "mas".equals(deployment)) {
				if (!(name.endsWith(".pkg") || name.endsWith(".dmg") || name.endsWith(".tar.xz")))
					continue;
				score += 100;
			} else {
				if (!name.endsWith(".jar") || name.contains("-src"))
					continue;
				score += 100;
			}

			if (score > bestScore) {
				bestScore = score;
				bestUrl = url;
			}
		}

		return bestUrl;
	}

	private static String getPreferredDebArch() {
		String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
		if (arch.contains("aarch64") || arch.contains("arm64"))
			return "arm64";
		if (arch.contains("arm"))
			return "armhf";
		if (arch.contains("64"))
			return "amd64";
		if (arch.contains("86") || arch.contains("i386") || arch.contains("i686"))
			return "i386";
		return null;
	}

	private static boolean is64BitArch() {
		return System.getProperty("os.arch", "").contains("64");
	}

	private static void restoreWindowBounds(JFrame window, Settings settings) {
		// store bounds on close
		window.addWindowListener(windowClosed(evt -> {
			// don't save window bounds if window is maximized
			if (!isMaximized(window)) {
				settings.put("window.x", String.valueOf(window.getX()));
				settings.put("window.y", String.valueOf(window.getY()));
				settings.put("window.width", String.valueOf(window.getWidth()));
				settings.put("window.height", String.valueOf(window.getHeight()));
			}
		}));

		// restore bounds
		int x = Integer.parseInt(settings.get("window.x"));
		int y = Integer.parseInt(settings.get("window.y"));
		int width = Integer.parseInt(settings.get("window.width"));
		int height = Integer.parseInt(settings.get("window.height"));
		window.setBounds(x, y, width, height);
	}

	/**
	 * SecurityManager was deprecated for removal and is no longer available on modern JDK versions.
	 */
	private static void initializeSecurityManager() {
		debug.fine("Skip SecurityManager initialization");
	}

	public static void initializeSystemProperties(ArgumentBean args) {
		System.setProperty("http.agent", String.format("%s %s", getApplicationName(), getApplicationVersion()));
		System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
		System.setProperty("sun.net.client.defaultReadTimeout", "60000");

		System.setProperty("swing.crossplatformlaf", "javax.swing.plaf.nimbus.NimbusLookAndFeel");
		System.setProperty("grape.root", ApplicationFolder.AppData.resolve("grape").getPath());
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

		if (args.unixfs) {
			System.setProperty("unixfs", "true");
		}

		if (args.disableExtendedAttributes) {
			System.setProperty("useExtendedFileAttributes", "false");
			System.setProperty("useCreationDate", "false");
		}
	}

	public static void initializeLogging(ArgumentBean args) throws IOException {
		// make sure that these folders exist
		ApplicationFolder.TemporaryFiles.get().mkdirs();
		ApplicationFolder.AppData.get().mkdirs();

		if (args.runCLI()) {
			// CLI logging settings
			log.setLevel(args.getLogLevel());
		} else {
			// GUI logging settings
			log.setLevel(Level.INFO);
			log.addHandler(new NotificationHandler(getApplicationName()));

			// log errors to file
			try {
				Handler errorLogHandler = createSimpleFileHandler(ApplicationFolder.AppData.resolve("error.log"), Level.WARNING);
				log.addHandler(errorLogHandler);
				debug.addHandler(errorLogHandler);
			} catch (Exception e) {
				log.log(Level.WARNING, "Failed to initialize error log", e);
			}
		}

		// tee stdout and stderr to log file if --log-file is set
		if (args.logFile != null) {
			Handler logFileHandler = createLogFileHandler(args.getLogFile(), args.logLock, Level.ALL);
			log.addHandler(logFileHandler);
			debug.addHandler(logFileHandler);
		}
	}

}
