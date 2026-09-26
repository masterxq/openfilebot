package org.openfilebot.web;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.openfilebot.Settings;

public class TheTVDBClientTest {

	static TheTVDBClient db = new TheTVDBClient(Settings.getApiKey("thetvdb"));

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

		// English name is kept as alias
		assertTrue(java.util.Arrays.asList(first.getAliasNames()).contains("Buffy the Vampire Slayer"));
	}

	@Test
	public void searchNoResults() throws Exception {
		List<SearchResult> results = db.search("qwertzuiopasdfghjklyxcvbnm", Locale.ENGLISH);

		assertTrue(results.isEmpty());
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
		// TheTVDB v4 reports the epoch placeholder date for unknown airdates
		assertTrue(last.getAirdate() == null || last.getAirdate().getYear() == 1970);
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

		// default to English titles if the Japanese translation is not available
		assertEquals("Wonderfalls", first.getSeriesName());
		assertEquals("Wax Lion", first.getTitle());
	}

	@Test
	public void getEpisodeListGerman() throws Exception {
		List<Episode> list = db.getEpisodeList(firefly, SortOrder.Airdate, Locale.GERMAN);

		Episode trainJob = list.stream().filter(it -> it.getId() == 297989).findFirst().get();

		assertEquals("Firefly", trainJob.getSeriesName());
		assertEquals("Schmutzige Geschäfte", trainJob.getTitle());
		assertEquals("1", trainJob.getSeason().toString());
		assertEquals("1", trainJob.getEpisode().toString());
		assertEquals("2", trainJob.getAbsolute().toString());
		assertEquals("de", trainJob.getSeriesInfo().getLanguage());
	}

	@Test
	public void getEpisodeListIllegalSeries() throws Exception {
		List<Episode> list = db.getEpisodeList(new SearchResult(999999999, "*** DOES NOT EXIST ***"), SortOrder.Airdate, Locale.ENGLISH);

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
	public void getEpisodeListNumberingAbsolute() throws Exception {
		List<Episode> list = db.getEpisodeList(firefly, SortOrder.Absolute, Locale.ENGLISH);

		Episode first = list.get(0);
		assertEquals("Firefly", first.getSeriesName());
		assertEquals("Serenity", first.getTitle());
		assertEquals(null, first.getSeason());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getAbsolute().toString());

		Episode second = list.get(1);
		assertEquals("The Train Job", second.getTitle());
		assertEquals("2", second.getEpisode().toString());
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

	@Test
	public void getEpisodeListLink() {
		assertEquals("https://www.thetvdb.com/?tab=seasonall&id=78874", db.getEpisodeListLink(firefly).toString());
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
		assertTrue(it.getGenres().contains("Action"));
		assertEquals("en", it.getLanguage());
		assertEquals("45", it.getRuntime().toString());
		assertEquals("Chuck", it.getName());
		assertEquals("2007-09-24", it.getStartDate().toString());
		assertEquals("NBC", it.getNetwork());
		assertEquals("Ended", it.getStatus());
		assertEquals("TV-PG", it.getCertification());
		assertEquals("tt0934814", it.getImdbId());
		assertEquals("Friday", it.getAirsDayOfWeek());
		assertEquals("20:00", it.getAirsTime());
		assertNotNull(it.getOverview());
		assertTrue(it.getOverview().length() >= 100);
		assertTrue(it.getLastUpdated() > 0);
	}

	@Test
	public void getSeriesInfoGerman() throws Exception {
		TheTVDBSeriesInfo it = db.getSeriesInfo(70327, Locale.GERMAN);

		assertEquals("Buffy", it.getName());
		assertEquals("de", it.getLanguage());
		assertTrue(it.getAliasNames().contains("Buffy the Vampire Slayer"));
		assertTrue(it.getOverview().contains("Auserwählte"));
	}

	@Test
	public void getArtwork() throws Exception {
		Artwork i = db.getArtwork(buffy.getId(), "fanart", Locale.ENGLISH).get(0);

		assertEquals("fanart", i.getTags().get(0));
		assertTrue(i.getTags().stream().anyMatch(it -> it.matches("\\d+x\\d+")));
		assertTrue(i.getUrl().toString().contains("/banners/fanart/"));
		assertTrue(i.matches("fanart"));
		assertFalse(i.matches("fanart", "1"));
		assertTrue(i.getRating() > 0);
	}

	@Test
	public void getArtworkBanner() throws Exception {
		List<Artwork> banners = db.getArtwork(80348, "banner", Locale.ENGLISH);

		assertFalse(banners.isEmpty());
		assertTrue(banners.get(0).getUrl().toString().contains("/banners/graphical/"));
	}

	@Test
	public void getArtworkUnsupportedCategory() throws Exception {
		assertTrue(db.getArtwork(80348, "does-not-exist", Locale.ENGLISH).isEmpty());
	}

	@Test
	public void getLanguages() throws Exception {
		List<String> languages = db.getLanguages();
		assertTrue(languages.contains("eng"));
		assertTrue(languages.contains("deu"));
		assertTrue(languages.contains("jpn"));
	}

	@Test
	public void getLanguageCode() {
		assertEquals("eng", db.getLanguageCode(null));
		assertEquals("eng", db.getLanguageCode(Locale.ROOT));
		assertEquals("eng", db.getLanguageCode(Locale.ENGLISH));
		assertEquals("deu", db.getLanguageCode(Locale.GERMAN));
		assertEquals("jpn", db.getLanguageCode(Locale.JAPANESE));
		assertEquals("heb", db.getLanguageCode(new Locale("iw")));
		assertEquals("por", db.getLanguageCode(new Locale("pt")));
		assertEquals("pt", db.getLanguageCode(new Locale("pt", "BR")));
		assertEquals("zho", db.getLanguageCode(Locale.CHINESE));
		assertEquals("zhtw", db.getLanguageCode(Locale.TAIWAN));
	}

	@Test
	public void getActors() throws Exception {
		List<Person> cast = db.getActors(firefly.getId(), Locale.ENGLISH);
		assertFalse(cast.isEmpty());
		assertTrue(cast.stream().anyMatch(p -> "Alan Tudyk".equals(p.getName())));
		Person p = cast.get(0);
		assertEquals("Nathan Fillion", p.getName());
		assertEquals("Actor", p.getJob());
		assertEquals(null, p.getDepartment());
		assertNotNull(p.getOrder());
		assertTrue(p.getImage().toString().contains("/banners/"));
	}

	@Test
	public void getEpisodeInfo() throws Exception {
		EpisodeInfo i = db.getEpisodeInfo(296337, Locale.ENGLISH);

		assertEquals("78845", i.getSeriesId().toString());
		assertEquals("296337", i.getId().toString());
		assertNotNull(i.getOverview());
		assertFalse(i.getOverview().isEmpty());
		assertFalse(i.getDirectors().isEmpty());
		assertFalse(i.getWriters().isEmpty());
		assertFalse(i.getActors().isEmpty());
	}

}
