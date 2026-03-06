package net.filebot.similarity;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

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

	@Test
	public void realisticNamingCases() {
		SeasonEpisodeMatcher matcher = new SeasonEpisodeMatcher(SeasonEpisodeMatcher.DEFAULT_SANITY, false);

		for (Case c : CASES) {
			String actual = String.valueOf(matcher.match(c.filename));
			assertEquals("Failed case: " + c.filename, c.expected, actual);
		}
	}

	@Test
	public void documentedPatternExamples() {
		SeasonEpisodeMatcher matcher = new SeasonEpisodeMatcher(SeasonEpisodeMatcher.DEFAULT_SANITY, false);

		assertEpisodeMatch(matcher, "Show Season 01 Episode 02", "[1x02]");
		assertEpisodeMatch(matcher, "Show.S01E01-E05", "[1x01, 1x02, 1x03, 1x04, 1x05]");

		assertEpisodeMatch(matcher, "Show.S01E01", "[1x01]");
		assertEpisodeMatch(matcher, "show.s01e02", "[1x02]");
		assertEpisodeMatch(matcher, "[s01]_[e02]", "[1x02]");
		assertEpisodeMatch(matcher, "show.s01.e02", "[1x02]");
		assertEpisodeMatch(matcher, "show.s01e02a", "[1x02]");
		assertEpisodeMatch(matcher, "show.s2010e01", "[2010x01]");
		assertEpisodeMatch(matcher, "show.s01e01-02-03-04", "[1x01, 1x02, 1x03, 1x04]");
		assertEpisodeMatch(matcher, "[s01]_[e01-02-03-04]", "[1x01, 1x02, 1x03, 1x04]");
		assertEpisodeMatch(matcher, "show.S01-01", "[1x01]");
		assertEpisodeMatch(matcher, "show.S03-07-08", "[3x07, 3x08]");
		assertEpisodeMatch(matcher, "doctor.who.2004-2x10", "[2x10]");

		assertEpisodeMatch(matcher, "1x01-1x02", "[1x01, 1x02]");
		assertEpisodeMatch(matcher, "1x01", "[1x01]");
		assertEpisodeMatch(matcher, "1.02", "[1x02]");
		assertEpisodeMatch(matcher, "1x01a", "[1x01]");
		assertEpisodeMatch(matcher, "10x01", "[10x01]");
		assertEpisodeMatch(matcher, "10.02", "[10x02]");
		assertEpisodeMatch(matcher, "1x01-02-03-04", "[1x01, 1x02, 1x03, 1x04]");
		assertEpisodeMatch(matcher, "1x01x02x03x04", "[1x01, 1x02, 1x03, 1x04]");

		assertEpisodeMatch(matcher, "101-105", "[101, 102, 103, 104, 105]");
		assertEpisodeMatch(matcher, "ep1", "[01]");
		assertEpisodeMatch(matcher, "ep.1", "[01]");
		assertEpisodeMatch(matcher, "[01]", "[01]");
		assertEpisodeMatch(matcher, "[102]", "[1x02, 102]");
		assertEpisodeMatch(matcher, "[1003]", "[10x03]");
		assertEpisodeMatch(matcher, "[10102]", "[1x01, 1x02]");
		assertEpisodeMatch(matcher, "1 of 2", "[01]");
		assertEpisodeMatch(matcher, "1of2", "[01]");
	}

	private void assertEpisodeMatch(SeasonEpisodeMatcher matcher, String filename, String expected) {
		String actual = String.valueOf(matcher.match(filename));
		assertEquals("Failed pattern example: " + filename, expected, actual);
	}

	@Test
	public void bracketedYearTitleShouldNotBeParsedAsEpisode() {
		SeasonEpisodeMatcher matcher = new SeasonEpisodeMatcher(SeasonEpisodeMatcher.DEFAULT_SANITY, false);

		assertEquals("Expected no episode match for title year in brackets", null, matcher.match("lost.(2002)"));
		assertEquals("Expected no episode match for title year in brackets", null, matcher.match("The.Movie.Title.(1999).1080p"));

		assertEpisodeMatch(matcher, "lost.(2002).S01E01", "[1x01]");
		assertEpisodeMatch(matcher, "lost.(2002).1x01-02", "[1x01, 1x02]");
	}

	@Test
	public void numericSeriesTitlePositiveCases() {
		SeasonEpisodeMatcher matcher = new SeasonEpisodeMatcher(SeasonEpisodeMatcher.DEFAULT_SANITY, false);

		assertEpisodeMatch(matcher, "Doctor.Who.2005.2x10", "[2x10]");
		assertEpisodeMatch(matcher, "Doctor.Who.(2005).S02E10", "[2x10]");
		assertEpisodeMatch(matcher, "Doctor.Who.2005-S02E10", "[2x10]");
		assertEpisodeMatch(matcher, "Doctor.Who.2005.2x10-11", "[2x10, 2x11]");

		assertEpisodeMatch(matcher, "The.100.S02E03", "[2x03]");
		assertEpisodeMatch(matcher, "The.100.2x03-04", "[2x03, 2x04]");

		assertEpisodeMatch(matcher, "11.22.63.S01E01", "[1x01]");
		assertEpisodeMatch(matcher, "11.22.63.1x01-02", "[1x01, 1x02]");

		assertEpisodeMatch(matcher, "24.S07E01", "[7x01]");
		assertEpisodeMatch(matcher, "24.7x01-02", "[7x01, 7x02]");

		assertEpisodeMatch(matcher, "9-1-1.S06E08", "[6x08]");
	}

}
