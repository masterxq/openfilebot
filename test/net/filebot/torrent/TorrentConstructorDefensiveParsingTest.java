package net.filebot.torrent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TorrentConstructorDefensiveParsingTest {

	public static void runAssertions() {
		Map<String, Object> fileEntry = new HashMap<String, Object>();
		fileEntry.put("path", Arrays.asList("Season 01", "The.Last.of.Us.S01E01.mkv"));
		fileEntry.put("length", 12345L);

		Map<String, Object> info = new HashMap<String, Object>();
		info.put("name", "The Last of Us");
		info.put("piece length", 16384L);
		info.put("files", Arrays.asList(fileEntry));

		Map<String, Object> root = new HashMap<String, Object>();
		root.put("created by", "qBittorrent/4.6");
		root.put("announce", "https://tracker.example/announce");
		root.put("comment", "Scene release");
		root.put("creation date", 1700000000L);
		root.put("info", info);

		Torrent torrent = new Torrent(root);

		if (!"The Last of Us".equals(torrent.getName())) {
			throw new AssertionError("name mismatch: " + torrent.getName());
		}

		if (!"qBittorrent/4.6".equals(torrent.getCreatedBy())) {
			throw new AssertionError("createdBy mismatch: " + torrent.getCreatedBy());
		}

		if (torrent.getFiles() == null || torrent.getFiles().isEmpty()) {
			throw new AssertionError("files should not be empty");
		}

		String path = torrent.getFiles().get(0).getPath();
		if (!"Season 01/The.Last.of.Us.S01E01.mkv".equals(path)) {
			throw new AssertionError("path mismatch: " + path);
		}
	}

	public static void main(String[] args) {
		runAssertions();
		System.out.println("ALL PASS TorrentConstructorDefensiveParsingTest");
	}
}
