package org.openfilebot.web;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Locale;

import org.junit.Test;

public class TheTVDBClientTest {

	static TheTVDBClient db = new TheTVDBClient("BA864DEE427E384A");

	SearchResult buffy = new SearchResult(70327, "Buffy the Vampire Slayer");
	SearchResult wonderfalls = new SearchResult(78845, "Wonderfalls");
	SearchResult firefly = new SearchResult(78874, "Firefly");

	@Test
	public void search() throws Exception {
		// test default language and query escaping (blanks)
		List<SearchResult> results = db.search("babylon 5", Locale.ENGLISH);

		assertFalse(results.isEmpty());
		assertTrue(results.stream().anyMatch(it -> it.getId() == 70726 && "Babylon 5".equals(it.getName())));
	}

	@Test
	public void searchGerman() throws Exception {
		List<SearchResult> results = db.search("Buffy", Locale.GERMAN);

		SearchResult first = results.get(0);
		assertEquals("Buffy", first.getName());
		assertEquals(70327, first.getId());
	}

	@Test
	public void getEpisodeListAll() throws Exception {
		List<Episode> list = db.getEpisodeList(buffy, SortOrder.Airdate, Locale.ENGLISH);

		assertEquals(145, list.size());

		// check ordinary episode
		Episode first = list.get(0);
		assertEquals("Buffy the Vampire Slayer", first.getSeriesName());
		assertEquals("1997-03-10", first.getSeriesInfo().getStartDate().toString());
		assertEquals("Welcome to the Hellmouth (1)", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getSeason().toString());
		assertEquals("1", first.getAbsolute().toString());
		assertEquals("1997-03-10", first.getAirdate().toString());

		// check special episode
		Episode last = list.get(list.size() - 1);
		assertEquals("Buffy the Vampire Slayer", last.getSeriesName());
		assertEquals("Unaired Pilot", last.getTitle());
		assertEquals(null, last.getSeason());
		assertEquals(null, last.getEpisode());
		assertEquals(null, last.getAbsolute());
		assertEquals("1", last.getSpecial().toString());
		assertEquals(null, last.getAirdate());
	}

	@Test
	public void getEpisodeListSingleSeason() throws Exception {
		List<Episode> list = db.getEpisodeList(wonderfalls, SortOrder.Airdate, Locale.ENGLISH);

		Episode first = list.get(0);

		assertEquals("Wonderfalls", first.getSeriesName());
		assertEquals("2004-03-12", first.getSeriesInfo().getStartDate().toString());
		assertEquals("Wax Lion", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getSeason().toString());
		assertTrue(first.getAbsolute() == null || "1".equals(first.getAbsolute().toString()));
		assertEquals("2004-03-12", first.getAirdate().toString());
		assertEquals("296337", first.getId().toString());
	}

	@Test
	public void getEpisodeListMissingInformation() throws Exception {
		List<Episode> list = db.getEpisodeList(wonderfalls, SortOrder.Airdate, Locale.JAPANESE);

		Episode first = list.get(0);

		assertEquals("Wonderfalls", first.getSeriesName());
		assertEquals("Wax Lion", first.getTitle());
	}

	@Test
	public void getEpisodeListIllegalSeries() throws Exception {
		List<Episode> list = db.getEpisodeList(new SearchResult(313193, "*** DOES NOT EXIST ***"), SortOrder.Airdate, Locale.ENGLISH);
		assertTrue(list.isEmpty());
	}

	@Test
	public void getEpisodeListNumberingDVD() throws Exception {
		List<Episode> list = db.getEpisodeList(firefly, SortOrder.DVD, Locale.ENGLISH);

		Episode first = list.get(0);
		assertEquals("Firefly", first.getSeriesName());
		assertEquals("2002-09-20", first.getSeriesInfo().getStartDate().toString());
		assertEquals("Serenity", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getSeason().toString());
		assertEquals("1", first.getAbsolute().toString());
		assertEquals("2002-12-20", first.getAirdate().toString());
	}

	@Test
	public void getEpisodeListNumberingAbsoluteAirdate() throws Exception {
		List<Episode> list = db.getEpisodeList(firefly, SortOrder.AbsoluteAirdate, Locale.ENGLISH);

		Episode first = list.get(0);
		assertEquals("Firefly", first.getSeriesName());
		assertEquals("2002-09-20", first.getSeriesInfo().getStartDate().toString());
		assertEquals("The Train Job", first.getTitle());
		assertEquals("20020920", first.getEpisode().toString());
		assertEquals(null, first.getSeason());
		assertEquals("2", first.getAbsolute().toString());
		assertEquals("2002-09-20", first.getAirdate().toString());
	}

	public void getEpisodeListLink() {
		assertEquals("http://www.thetvdb.com/?tab=seasonall&id=78874", db.getEpisodeListLink(firefly).toString());
	}

	@Test
	public void lookupByID() throws Exception {
		SearchResult series = db.lookupByID(78874, Locale.ENGLISH);
		assertEquals("Firefly", series.getName());
		assertEquals(78874, series.getId());
	}

	@Test
	public void lookupByIMDbID() throws Exception {
		SearchResult series = db.lookupByIMDbID(303461, Locale.ENGLISH);
		assertEquals("Firefly", series.getName());
		assertEquals(78874, series.getId());
	}

	@Test
	public void getSeriesInfo() throws Exception {
		TheTVDBSeriesInfo it = db.getSeriesInfo(80348, Locale.ENGLISH);

		assertEquals(80348, it.getId(), 0);
		assertEquals("Action", it.getGenres().get(0));
		assertEquals("en", it.getLanguage());
		assertEquals("45", it.getRuntime().toString());
		assertEquals("Chuck", it.getName());
		assertEquals(9.0, it.getRating(), 0.5);
		assertTrue(it.getRatingCount() >= 1000);
		assertEquals("tt0934814", it.getImdbId());
		assertEquals("Friday", it.getAirsDayOfWeek());
		assertEquals("8:00 PM", it.getAirsTime());
		assertNotNull(it.getOverview());
		assertTrue(it.getOverview().length() >= 100);
		assertTrue(it.getBannerUrl().toString().contains("/banners/graphical/"));
	}

	@Test
	public void getArtwork() throws Exception {
		Artwork i = db.getArtwork(buffy.getId(), "fanart", Locale.ENGLISH).get(0);

		assertEquals("fanart", i.getTags().get(0));
		assertEquals("graphical", i.getTags().get(1));
		assertTrue(i.getTags().stream().anyMatch(it -> it.matches("\\d+x\\d+")));
		assertTrue(i.getUrl().toString().contains("/banners/fanart/"));
		assertTrue(i.matches("fanart"));
		assertFalse(i.matches("fanart", "1"));
		assertTrue(i.getRating() > 0);
	}

	@Test
	public void getLanguages() throws Exception {
		List<String> languages = db.getLanguages();
		assertTrue(languages.contains("en"));
		assertTrue(languages.contains("de"));
		assertTrue(languages.contains("ja"));
	}

	@Test
	public void getActors() throws Exception {
		List<Person> cast = db.getActors(firefly.getId(), Locale.ENGLISH);
		assertFalse(cast.isEmpty());
		assertTrue(cast.stream().anyMatch(p -> "Alan Tudyk".equals(p.getName())));
		Person p = cast.get(0);
		assertEquals("Actor", p.getJob());
		assertEquals(null, p.getDepartment());
		assertNotNull(p.getOrder());
		assertTrue(p.getImage().toString().contains("/banners/actors/"));
	}

	@Test
	public void getEpisodeInfo() throws Exception {
		EpisodeInfo i = db.getEpisodeInfo(296337, Locale.ENGLISH);

		assertEquals("78845", i.getSeriesId().toString());
		assertEquals("296337", i.getId().toString());
		assertEquals(8.2, i.getRating(), 0.1);
		assertTrue(i.getVotes() > 0);
		assertNotNull(i.getOverview());
		assertFalse(i.getOverview().isEmpty());
		assertFalse(i.getDirectors().isEmpty());
		assertFalse(i.getWriters().isEmpty());
		assertFalse(i.getActors().isEmpty());
	}

}
