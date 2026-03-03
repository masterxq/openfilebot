package net.filebot.similarity;

import java.util.Arrays;
import java.util.List;

public class SeasonEpisodeMatcherRealisticNamingTest {

	private static final class Case {
		final String filename;
		final String expected;

		Case(String filename, String expected) {
			this.filename = filename;
			this.expected = expected;
		}
	}

	private static final List<Case> CASES = Arrays.asList(
		new Case("The.Last.of.Us.S01-01.1080p.WEB-DL.mkv", "[1x01]"),
		new Case("Chicago.Fire.S08-09.720p.HDTV.x264.mkv", "[8x09]"),
		new Case("Dark.S03-07-08.GERMAN.WEBRip.mkv", "[3x07, 3x08]"),
		new Case("Doctor.Who.2005.S2005-01.BluRay.mkv", "[2005x01]"),
		new Case("House.of.the.Dragon.S02-09-10.2160p.mkv", "[2x09, 2x10]")
	);

	public static void runAssertions() {
		SeasonEpisodeMatcher matcher = new SeasonEpisodeMatcher(SeasonEpisodeMatcher.DEFAULT_SANITY, false);

		int failed = 0;
		for (Case c : CASES) {
			String actual = String.valueOf(matcher.match(c.filename));
			boolean ok = c.expected.equals(actual);
			System.out.println((ok ? "PASS" : "FAIL") + " | " + c.filename + " | expected=" + c.expected + " | actual=" + actual);
			if (!ok) {
				failed++;
			}
		}

		if (failed > 0) {
			throw new AssertionError("FAILED " + failed + " / " + CASES.size());
		}
	}

	public static void main(String[] args) {
		runAssertions();
		System.out.println("ALL PASS " + CASES.size());
	}
}
