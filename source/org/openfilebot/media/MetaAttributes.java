package org.openfilebot.media;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;

import org.openfilebot.MetaAttributeView;
import org.openfilebot.vfs.SimpleFileInfo;
import org.openfilebot.web.AudioTrack;
import org.openfilebot.web.Episode;
import org.openfilebot.web.Movie;
import org.openfilebot.web.MoviePart;
import org.openfilebot.web.MultiEpisode;

public class MetaAttributes {

	public static final String LEGACY_FILENAME_KEY = "net.filebot.filename";
	public static final String LEGACY_METADATA_KEY = "net.filebot.metadata";
	public static final String FILENAME_KEY = "org.openfilebot.filename";
	public static final String METADATA_KEY = "org.openfilebot.metadata";

	public static final Map<String, String> JSON_TYPE_MAP = unmodifiableMap(Stream.of(Episode.class, MultiEpisode.class, Movie.class, MoviePart.class, AudioTrack.class, SimpleFileInfo.class).collect(toMap(Class::getName, Class::getSimpleName)));

	private final BasicFileAttributeView fileAttributeView;
	private final MetaAttributeView metaAttributeView;

	public MetaAttributes(File file) throws IOException {
		this.metaAttributeView = new MetaAttributeView(file);
		this.fileAttributeView = Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class);
	}

	public void setCreationDate(long millis) throws IOException {
		fileAttributeView.setTimes(null, null, FileTime.fromMillis(millis));
	}

	public long getCreationDate(long time) {
		try {
			return fileAttributeView.readAttributes().creationTime().toMillis();
		} catch (IOException e) {
			return 0;
		}
	}

	public void setOriginalName(String name) {
		metaAttributeView.put(FILENAME_KEY, name);
		metaAttributeView.put(LEGACY_FILENAME_KEY, name);
	}

	public String getOriginalName() {
		String value = metaAttributeView.get(FILENAME_KEY);
		if (value != null && value.length() > 0) {
			return value;
		}

		return metaAttributeView.get(LEGACY_FILENAME_KEY);
	}

	public void setObject(Object object) {
		String json = toJson(object);
		metaAttributeView.put(METADATA_KEY, json);
		metaAttributeView.put(LEGACY_METADATA_KEY, json);
	}

	public Object getObject() {
		String json = metaAttributeView.get(METADATA_KEY);
		if (json == null || json.length() == 0) {
			json = metaAttributeView.get(LEGACY_METADATA_KEY);
		}

		if (json != null && json.length() > 0) {
			return toObject(json);
		}
		return null;
	}

	public void clear() {
		metaAttributeView.put(FILENAME_KEY, null);
		metaAttributeView.put(METADATA_KEY, null);
		metaAttributeView.put(LEGACY_FILENAME_KEY, null);
		metaAttributeView.put(LEGACY_METADATA_KEY, null);
	}

	public static String toJson(Object object) {
		Map<String, Object> options = new HashMap<String, Object>();
		options.put(JsonWriter.TYPE_NAME_MAP, JSON_TYPE_MAP);
		options.put(JsonWriter.SKIP_NULL_FIELDS, true);

		return JsonWriter.objectToJson(object, options);
	}

	public static Object toObject(String json) {
		if (json == null || json.isEmpty()) {
			return null;
		}

		Map<String, Object> options = new HashMap<String, Object>();
		options.put(JsonReader.TYPE_NAME_MAP, JSON_TYPE_MAP);

		// options must be a modifiable map
		return JsonReader.jsonToJava(json, options);
	}

}
