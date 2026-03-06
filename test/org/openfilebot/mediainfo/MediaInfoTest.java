package org.openfilebot.mediainfo;

import static org.junit.Assume.*;
import static org.junit.Assert.*;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import org.openfilebot.mediainfo.MediaInfo.StreamKind;

public class MediaInfoTest {

	private static final File SAMPLE_FIXTURE = new File("test/resources/mediainfo/big_buck_bunny_320x180_5s.mp4");

	File getSampleFile(String name) throws Exception {
		File tmpdir = new File(FileUtils.getTempDirectory(), getClass().getName());
		File sample = new File(tmpdir, "big_buck_bunny_320x180_5s.mp4");

		assumeTrue("Local sample fixture is missing: " + SAMPLE_FIXTURE.getPath(), SAMPLE_FIXTURE.isFile());

		if (!sample.exists()) {
			FileUtils.copyFile(SAMPLE_FIXTURE, sample);
		}

		File file = new File(tmpdir, name + ".mp4");
		if (!file.exists()) {
			FileUtils.copyFile(sample, file);
		}

		return file;
	}

	void testSampleFile(String name) throws Exception {
		MediaInfo mi = new MediaInfo().open(getSampleFile(name));

		assertEquals("MPEG-4", mi.get(StreamKind.General, 0, "Format"));
		assertEquals("AVC", mi.get(StreamKind.Video, 0, "Format"));
		assertEquals("AAC", mi.get(StreamKind.Audio, 0, "Format"));
	}

	@Test
	public void open() throws Exception {
		testSampleFile("English");
	}

	@Test
	public void openUnicode() throws Exception {
		testSampleFile("中文");
		testSampleFile("日本語");
	}

	@Test
	public void openDiacriticalMarks() throws Exception {
		testSampleFile("Español");
		testSampleFile("Österreichisch");
	}

}
