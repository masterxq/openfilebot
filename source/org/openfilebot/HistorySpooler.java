package org.openfilebot;

import static java.nio.channels.Channels.*;
import static org.openfilebot.Logging.*;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.io.output.CloseShieldOutputStream;

import org.openfilebot.History.Element;

public final class HistorySpooler {

	private static final HistorySpooler instance = new HistorySpooler();

	public static HistorySpooler getInstance() {
		return instance;
	}

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(HistorySpooler.getInstance()::commit, "HistorySpoolerShutdownHook")); // commit session history on shutdown
	}

	private final File persistentHistoryFile = ApplicationFolder.AppData.resolve("history.xml");
	private final List<File> legacyHistoryFiles = getLegacyHistoryFiles();

	private int sessionHistoryTotalSize = 0;
	private int persistentHistoryTotalSize = -1;
	private boolean persistentHistoryEnabled = true;

	private final History sessionHistory = new History();

	private HistorySpooler() {
		migrateLegacyHistory();
	}

	private void migrateLegacyHistory() {
		if (hasUsableHistory(persistentHistoryFile)) {
			return;
		}

		for (File legacyHistoryFile : legacyHistoryFiles) {
			if (!legacyHistoryFile.isFile() || legacyHistoryFile.length() <= 0) {
				continue;
			}

			try (FileChannel source = FileChannel.open(legacyHistoryFile.toPath(), StandardOpenOption.READ)) {
				History legacyHistory = History.importHistoryChecked(new CloseShieldInputStream(newInputStream(source)));

				if (legacyHistory.totalSize() <= 0) {
					debug.warning("Skip history migration because legacy history is empty");
					continue;
				}

				persistentHistoryFile.getParentFile().mkdirs();

				try (FileChannel target = FileChannel.open(persistentHistoryFile.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
					try (FileLock lock = target.lock()) {
						target.position(0);
						History.exportHistory(legacyHistory, new CloseShieldOutputStream(newOutputStream(target)));
						target.truncate(target.position());
						persistentHistoryTotalSize = legacyHistory.totalSize();
					}
				}

				log.info(() -> String.format("Migrated %d rename history entries from %s", persistentHistoryTotalSize, legacyHistoryFile));
				return;
			} catch (Exception e) {
				debug.log(Level.WARNING, String.format("Failed to migrate legacy history from %s (ignored)", legacyHistoryFile), e);
			}
		}
	}

	private boolean hasUsableHistory(File file) {
		if (!file.isFile() || file.length() <= 0) {
			return false;
		}

		try (FileChannel source = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
			History history = History.importHistoryChecked(new CloseShieldInputStream(newInputStream(source)));
			if (history.totalSize() > 0) {
				persistentHistoryTotalSize = history.totalSize();
				return true;
			}
		} catch (Exception e) {
			debug.log(Level.WARNING, String.format("Failed to read existing history from %s", file), e);
		}

		return false;
	}

	private List<File> getLegacyHistoryFiles() {
		Set<File> files = new LinkedHashSet<File>();
		files.addAll(Arrays.asList(
				ApplicationFolder.UserHome.resolve(".filebot/history.xml"),
				ApplicationFolder.UserHome.resolve(".config/filebot/history.xml"),
				ApplicationFolder.UserHome.resolve(".config/FileBot/history.xml"),
				ApplicationFolder.UserHome.resolve("AppData/Roaming/FileBot/history.xml"),
				ApplicationFolder.UserHome.resolve("AppData/Local/FileBot/history.xml")));

		String appData = System.getenv("APPDATA");
		if (appData != null && appData.length() > 0) {
			files.add(new File(appData, "FileBot/history.xml"));
		}

		String localAppData = System.getenv("LOCALAPPDATA");
		if (localAppData != null && localAppData.length() > 0) {
			files.add(new File(localAppData, "FileBot/history.xml"));
		}

		return new ArrayList<File>(files);
	}

	public synchronized History getCompleteHistory() throws IOException {
		if (persistentHistoryFile.length() <= 0) {
			return new History(sessionHistory.sequences());
		}

		try (FileChannel channel = FileChannel.open(persistentHistoryFile.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
			try (FileLock lock = channel.lock()) {
				History history = History.importHistory(new CloseShieldInputStream(newInputStream(channel))); // keep JAXB from closing the stream
				history.addAll(sessionHistory.sequences());
				return history;
			}
		}
	}

	public synchronized void commit() {
		if (sessionHistory.sequences().isEmpty() || !persistentHistoryEnabled) {
			return;
		}

		try {
			try (FileChannel channel = FileChannel.open(persistentHistoryFile.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
				try (FileLock lock = channel.lock()) {
					History history = new History();

					// load existing history from previous sessions
					if (channel.size() > 0) {
						try {
							channel.position(0);
							history = History.importHistory(new CloseShieldInputStream(newInputStream(channel))); // keep JAXB from closing the stream
						} catch (Exception e) {
							debug.log(Level.SEVERE, "Failed to read history file", e);
						}
					}

					// write new combined history
					history.addAll(sessionHistory.sequences());

					channel.position(0);
					History.exportHistory(history, new CloseShieldOutputStream(newOutputStream(channel))); // keep JAXB from closing the stream
					channel.truncate(channel.position());

					sessionHistory.clear();
					persistentHistoryTotalSize = history.totalSize();
				}
			}
		} catch (Exception e) {
			debug.log(Level.SEVERE, "Failed to write history file", e);
		}
	}

	public synchronized void append(Map<File, File> elements) {
		append(elements.entrySet());
	}

	public synchronized void append(Iterable<Entry<File, File>> elements) {
		List<Element> sequence = new ArrayList<Element>();

		for (Entry<File, File> element : elements) {
			File k = element.getKey();
			File v = element.getValue();

			if (k != null && v != null) {
				sequence.add(new Element(k.getName(), v.getPath(), k.getParentFile()));
			}
		}

		if (sequence.size() > 0) {
			sessionHistory.add(sequence); // append to session history
			sessionHistoryTotalSize += sequence.size();
		}
	}

	public synchronized void append(History importHistory) {
		sessionHistory.merge(importHistory);
	}

	public synchronized History getSessionHistory() {
		return new History(sessionHistory.sequences());
	}

	public synchronized int getSessionHistoryTotalSize() {
		return sessionHistoryTotalSize;
	}

	public synchronized int getPersistentHistoryTotalSize() {
		return persistentHistoryTotalSize;
	}

	public synchronized void setPersistentHistoryEnabled(boolean persistentHistoryEnabled) {
		this.persistentHistoryEnabled = persistentHistoryEnabled;
	}

}
