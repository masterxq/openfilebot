
package org.openfilebot.web;


import static org.junit.Assert.*;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


@RunWith(Parameterized.class)
public class OpenSubtitlesHasherTest {

	private String expectedHash;
	private File file;


	public OpenSubtitlesHasherTest(String expectedHash, File file) {
		this.file = file;
		this.expectedHash = expectedHash;
	}


	@Parameters
	public static Collection<Object[]> parameters() throws Exception {
		Object[][] parameters = new Object[3][];

		parameters[0] = fixture("small-text", generateBytes(97, 7));
		parameters[1] = fixture("single-chunk", generateBytes(OpenSubtitlesHasher.HASH_CHUNK_SIZE + 31, 13));
		parameters[2] = fixture("double-chunk", generateBytes(2 * OpenSubtitlesHasher.HASH_CHUNK_SIZE + 17, 29));

		return Arrays.asList(parameters);
	}

	private static Object[] fixture(String label, byte[] data) throws Exception {
		File file = File.createTempFile("OpenSubtitlesHasherTest-" + label + "-", ".bin");
		file.deleteOnExit();

		try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
			out.write(data);
		}

		return new Object[] { computeExpectedHash(data), file };
	}

	private static byte[] generateBytes(int size, int seed) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(size);
		int value = seed;

		for (int i = 0; i < size; i++) {
			value = (value * 1103515245 + 12345) & 0x7fffffff;
			out.write(value & 0xFF);
		}

		return out.toByteArray();
	}

	private static String computeExpectedHash(byte[] data) {
		int chunkSize = Math.min(OpenSubtitlesHasher.HASH_CHUNK_SIZE, data.length);

		long head = checksumLittleEndian(data, 0, chunkSize);
		long tail = checksumLittleEndian(data, data.length - chunkSize, chunkSize);

		return String.format("%016x", (long) data.length + head + tail);
	}

	private static long checksumLittleEndian(byte[] data, int offset, int length) {
		long hash = 0;

		for (int i = offset; i + 7 < offset + length; i += 8) {
			long value = ((long) data[i] & 0xff)
					| (((long) data[i + 1] & 0xff) << 8)
					| (((long) data[i + 2] & 0xff) << 16)
					| (((long) data[i + 3] & 0xff) << 24)
					| (((long) data[i + 4] & 0xff) << 32)
					| (((long) data[i + 5] & 0xff) << 40)
					| (((long) data[i + 6] & 0xff) << 48)
					| (((long) data[i + 7] & 0xff) << 56);

			hash += value;
		}

		return hash;
	}


	@Test
	public void computeHashFile() throws Exception {
		assertEquals(expectedHash, OpenSubtitlesHasher.computeHash(file));
	}


	@Test
	public void computeHashStream() throws Exception {
		assertEquals(expectedHash, OpenSubtitlesHasher.computeHash(new FileInputStream(file), file.length()));
	}

}
