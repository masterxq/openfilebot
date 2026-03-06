package org.openfilebot.web;

import static org.openfilebot.CachedResource.*;
import static org.junit.Assert.*;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.junit.Ignore;
import org.junit.Test;

import org.openfilebot.Cache;
import org.openfilebot.CacheType;
import org.openfilebot.CachedResource;

public class TMDbClientTest {

	static TMDbClient db = new TMDbClient("66308fb6e3fd850dde4c7d21df2e8306", false);

	@Test
	public void searchByName() throws Exception {
		List<Movie> result = db.searchMovie("Serenity", Locale.CHINESE);
		Movie movie = result.get(0);

		assertEquals("冲出宁静号", movie.getName());
		assertEquals(2005, movie.getYear());
		assertEquals(-1, movie.getImdbId());
		assertEquals(16320, movie.getTmdbId());
	}

	@Test
	public void searchByNameWithYearShortName() throws Exception {
		List<Movie> result = db.searchMovie("Up 2009", Locale.ENGLISH);
		Movie movie = result.get(0);

		assertEquals("Up", movie.getName());
		assertEquals(2009, movie.getYear());
		assertEquals(-1, movie.getImdbId());
		assertEquals(14160, movie.getTmdbId());
	}

	@Test
	public void searchByNameWithYearNumberName() throws Exception {
		List<Movie> result = db.searchMovie("9 (2009)", Locale.ENGLISH);
		assertFalse(result.isEmpty());
		assertTrue(result.stream().anyMatch(movie -> movie.getTmdbId() == 12244 && movie.getYear() == 2009));
	}

	@Test
	public void searchByNameGerman() throws Exception {
		List<Movie> result = db.searchMovie("Die Gelbe Hölle", Locale.GERMAN);
		assertFalse(result.isEmpty());
		assertTrue(result.stream().anyMatch(movie -> movie.getName().toLowerCase(Locale.ROOT).contains("gelbe hölle") || movie.getEffectiveNames().stream().anyMatch(it -> it.contains("Camp on Blood Island"))));
	}

	@Test
	public void searchByNameMexican() throws Exception {
		List<Movie> result = db.searchMovie("Suicide Squad", new Locale("es", "MX"));
		Movie movie = result.get(0);

		assertEquals("Escuadrón Suicida", movie.getName());
		assertEquals(2016, movie.getYear());
		assertEquals(-1, movie.getImdbId());
		assertEquals(297761, movie.getTmdbId());
	}

	@Test
	public void searchByIMDB() throws Exception {
		Movie movie = db.getMovieDescriptor(new Movie(418279), Locale.ENGLISH);

		assertEquals("Transformers", movie.getName());
		assertEquals(2007, movie.getYear(), 0);
		assertEquals(418279, movie.getImdbId(), 0);
		assertEquals(1858, movie.getTmdbId(), 0);
	}

	@Test
	public void getMovieInfo() throws Exception {
		MovieInfo movie = db.getMovieInfo(new Movie(418279), Locale.ENGLISH, true);

		assertEquals("Transformers", movie.getName());
		assertEquals("2007-06-27", movie.getReleased().toString());
		assertEquals("PG-13", movie.getCertification());
		assertEquals("PG-13", movie.getCertifications().get("US"));
		assertFalse(movie.getSpokenLanguages().isEmpty());
		assertFalse(movie.getActors().isEmpty());
		assertNotNull(movie.getDirector());
		assertTrue(movie.getTrailers().isEmpty() || movie.getTrailers().stream().anyMatch(it -> it.toString().contains("YouTube::")));
	}

	@Test
	public void getMovieInfoForceLanguageCode() throws Exception {
		MovieInfo shiva = db.getMovieInfo(new Movie(1260396), Locale.forLanguageTag("he-IL"), false);
		assertEquals("שבעה", shiva.getName());

		MovieInfo raid = db.getMovieInfo(new Movie(1899353), Locale.forLanguageTag("id-ID"), false);
		assertNotNull(raid.getName());
		assertFalse(raid.getName().trim().isEmpty());
	}

	@Test
	public void getAlternativeTitles() throws Exception {
		Map<String, List<String>> titles = db.getAlternativeTitles(16320); // Serenity

		assertTrue(titles.values().stream().filter(Objects::nonNull).flatMap(List::stream).anyMatch("宁静号"::equals));
	}

	@Test
	public void getArtwork() throws Exception {
		Artwork a = db.getArtwork(16320, "backdrops", Locale.ROOT).get(0);
		assertTrue(a.getTags().contains("backdrops"));
		assertEquals("https", a.getUrl().getProtocol());
		assertEquals("image.tmdb.org", a.getUrl().getHost());
	}

	@Test
	public void getPeople() throws Exception {
		Person p = db.getMovieInfo("16320", Locale.ENGLISH, true).getCrew().get(0);
		assertEquals("Nathan Fillion", p.getName());
		assertTrue(p.getCharacter().contains("Mal"));
		assertEquals(null, p.getJob());
		assertEquals(null, p.getDepartment());
		assertEquals("0", p.getOrder().toString());
		assertEquals("https", p.getImage().getProtocol());
		assertEquals("image.tmdb.org", p.getImage().getHost());
	}

	@Test
	public void discoverPeriod() throws Exception {
		List<Movie> results = db.discover(LocalDate.parse("2014-09-15"), LocalDate.parse("2014-10-22"), Locale.ENGLISH);
		assertFalse(results.isEmpty());
		assertTrue(results.stream().allMatch(it -> it.getYear() >= 2014 && it.getYear() <= 2015));
	}

	@Test
	public void discoverBestOfYear() throws Exception {
		List<Movie> results = db.discover(2015, Locale.ENGLISH);
		assertFalse(results.isEmpty());
		assertTrue(results.stream().anyMatch(it -> it.getTmdbId() == 76341 && it.getYear() == 2015));
	}

	@Ignore
	@Test
	public void floodLimit() throws Exception {
		for (Locale it : Locale.getAvailableLocales()) {
			List<Movie> results = db.searchMovie("Serenity", it);
			assertEquals(16320, results.get(0).getTmdbId());
		}
	}

	@Ignore
	@Test
	public void etag() throws Exception {
		Cache cache = Cache.getCache("test", CacheType.Persistent);
		Cache etagStorage = Cache.getCache("etag", CacheType.Persistent);
		CachedResource<String, byte[]> resource = cache.bytes("http://devel.squid-cache.org/old_projects.html#etag", URL::new).fetch(fetchIfNoneMatch(etagStorage::get, etagStorage::put)).expire(Duration.ZERO);
		assertArrayEquals(resource.get(), resource.get());
	}

}
