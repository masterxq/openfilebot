package org.openfilebot.web;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class EpisodeFormatMultiNumberApiTest {

	@Test
	public void pending() throws Exception {
		runAssertions();
	}

	public static void runAssertions() throws Exception {
		EpisodeFormat formatter = EpisodeFormat.SeasonEpisode;

		Method formatMultiSxE = EpisodeFormat.class.getMethod("formatMultiSxE", Iterable.class);
		Method formatMultiS00E00 = EpisodeFormat.class.getMethod("formatMultiS00E00", Iterable.class);

		List<Episode> episodes = Arrays.asList(
			new Episode("The Last of Us", 1, 1, "When You're Lost in the Darkness"),
			new Episode("The Last of Us", 1, 2, "Infected"),
			new Episode("The Last of Us", 1, 3, "Long Long Time")
		);

		String sx = (String) formatMultiSxE.invoke(formatter, episodes);
		String s00 = (String) formatMultiS00E00.invoke(formatter, episodes);

		if (!"1x01x02x03".equals(sx)) {
			throw new AssertionError("formatMultiSxE mismatch: " + sx);
		}

		if (!"S01E01-E02-E03".equals(s00)) {
			throw new AssertionError("formatMultiS00E00 mismatch: " + s00);
		}
	}

	public static void main(String[] args) throws Exception {
		runAssertions();
		System.out.println("ALL PASS EpisodeFormatMultiNumberApiTest");
	}
}
