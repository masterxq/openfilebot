package org.openfilebot.web;

import static org.junit.Assume.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Locale;

import org.junit.Test;

public class AnidbClientTest {

	static AnidbClient anidb = new AnidbClient("filebot", 6);

	/**
	 * 74 episodes
	 */
	SearchResult monsterSearchResult = new SearchResult(1539, "Monster");

	/**
	 * 45 episodes
	 */
	SearchResult twelvekingdomsSearchResult = new SearchResult(26, "Juuni Kokuki");

	/**
	 * 38 episodes, lots of special characters
	 */
	SearchResult princessTutuSearchResult = new SearchResult(516, "Princess Tutu");

	@Test
	public void getAnimeTitles() throws Exception {
		SearchResult[] animeTitles = anidb.getAnimeTitles();
		assumeTrue(animeTitles.length > 0);
		assertTrue(animeTitles.length > 1000);
	}

	@Test
	public void search() throws Exception {
		List<SearchResult> results = anidb.search("one piece", Locale.ENGLISH);
		assumeFalse(results.isEmpty());

		assertTrue(results.stream().anyMatch(result -> result.getId() == 69 && "One Piece".equals(result.getName())));
	}

	@Test
	public void searchNoMatch() throws Exception {
		List<SearchResult> results = anidb.search("i will not find anything for this query string", Locale.ENGLISH);

		assertTrue(results.isEmpty());
	}

	@Test
	public void searchTitleAlias() throws Exception {
		// Seikai no Senki (main title), Banner of the Stars (official English title)
		List<SearchResult> banner = anidb.search("banner of the stars", Locale.ENGLISH);
		List<SearchResult> seikai = anidb.search("seikai no senki", Locale.ENGLISH);
		List<SearchResult> naruto = anidb.search("naruto", Locale.ENGLISH);

		assumeFalse(banner.isEmpty() || seikai.isEmpty() || naruto.isEmpty());
		assertEquals("Seikai no Senki", banner.get(0).getName());
		assertEquals("Seikai no Senki", seikai.get(0).getName());

		// no matching title
		assertEquals("Naruto", naruto.get(0).getName());
	}

	@Test
	public void getEpisodeListAll() throws Exception {
		List<Episode> list = anidb.getEpisodeList(monsterSearchResult, SortOrder.Airdate, Locale.ENGLISH);

		assertEquals(77, list.size());

		Episode first = list.get(0);

		assertEquals("Monster", first.getSeriesName());
		assertEquals("2004-04-07", first.getSeriesInfo().getStartDate().toString());
		assertEquals("Herr Dr. Tenma", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getAbsolute().toString());
		assertEquals(null, first.getSeason());
		assertEquals("2004-04-07", first.getAirdate().toString());
		assertEquals("17843", first.getId().toString());
	}

	@Test
	public void getEpisodeListAllShortLink() throws Exception {
		List<Episode> list = anidb.getEpisodeList(twelvekingdomsSearchResult, SortOrder.Airdate, Locale.ENGLISH);

		assertEquals(47, list.size());

		Episode first = list.get(0);

		assertEquals("The Twelve Kingdoms", first.getSeriesName());
		assertEquals("2002-04-09", first.getSeriesInfo().getStartDate().toString());
		assertEquals("Shadow of the Moon, The Sea of Shadow - Chapter 1", first.getTitle());
		assertEquals("1", first.getEpisode().toString());
		assertEquals("1", first.getAbsolute().toString());
		assertEquals(null, first.getSeason());
		assertEquals("2002-04-09", first.getAirdate().toString());
	}

	@Test
	public void getEpisodeListEncoding() throws Exception {
		String title = anidb.getEpisodeList(princessTutuSearchResult, SortOrder.Airdate, Locale.ENGLISH).get(6).getTitle();
		assertTrue(title.toLowerCase(Locale.ROOT).contains("blauen donau"));
	}

	@Test
	public void getEpisodeListI18N() throws Exception {
		List<Episode> list = anidb.getEpisodeList(monsterSearchResult, SortOrder.Airdate, Locale.JAPANESE);

		Episode last = list.get(73);
		assertEquals("MONSTER", last.getSeriesName());
		assertEquals("2004-04-07", last.getSeriesInfo().getStartDate().toString());
		assertEquals("本当の怪物", last.getTitle());
		assertEquals("74", last.getEpisode().toString());
		assertEquals("74", last.getAbsolute().toString());
		assertEquals(null, last.getSeason());
		assertEquals("2005-09-28", last.getAirdate().toString());
	}

	@Test
	public void getEpisodeListTrimRecap() throws Exception {
		assertEquals("Sea God of the East, Azure Sea of the West - Transition Chapter", anidb.getEpisodeList(twelvekingdomsSearchResult, SortOrder.Airdate, Locale.ENGLISH).get(44).getTitle());
	}

	@Test
	public void getEpisodeListLink() throws Exception {
		assertEquals("http://anidb.net/a1539", anidb.getEpisodeListLink(monsterSearchResult).toURL().toString());
	}

}
