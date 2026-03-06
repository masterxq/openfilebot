package org.openfilebot.web;

import static java.util.Collections.*;
import static org.openfilebot.Settings.*;
import static org.junit.Assume.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import org.openfilebot.web.OpenSubtitlesSubtitleDescriptor.Property;
import org.openfilebot.web.OpenSubtitlesXmlRpc.Query;
import org.openfilebot.web.OpenSubtitlesXmlRpc.SubFile;
import org.openfilebot.web.OpenSubtitlesXmlRpc.TryUploadResponse;
import redstone.xmlrpc.XmlRpcFault;

public class OpenSubtitlesXmlRpcTest {

	private static OpenSubtitlesXmlRpc xmlrpc = new OpenSubtitlesXmlRpc(String.format("%s %s", getApplicationName(), getApplicationVersion()));

	@BeforeClass
	public static void login() throws Exception {
		// login manually
		xmlrpc.loginAnonymous();
	}

	@Test
	public void search() throws Exception {
		List<SubtitleSearchResult> list;
		try {
			list = xmlrpc.searchMoviesOnIMDB("babylon 5");
		} catch (XmlRpcFault e) {
			assumeNoException(e);
			return;
		}

		assertFalse(list.isEmpty());
		Movie sample = list.get(0);

		assertNotNull(sample.getName());
		assertTrue(sample.getYear() > 1900);
		assertTrue(sample.getImdbId() > 0);
	}

	@Test
	public void searchOST() throws Exception {
		List<SubtitleSearchResult> list;
		try {
			list = xmlrpc.searchMoviesOnIMDB("Linkin.Park.New.Divide.1280-720p.Transformers.Revenge.of.the.Fallen.ost");
		} catch (XmlRpcFault e) {
			assumeNoException(e);
			return;
		}

		assertFalse(list.stream().anyMatch(it -> it.getName().contains("Linkin.Park")));
	}

	@Test
	public void getSubtitleListEnglish() throws Exception {
		List<OpenSubtitlesSubtitleDescriptor> list = xmlrpc.searchSubtitles(singleton(Query.forImdbId(361256, -1, -1, "eng")));
		assumeFalse(list.isEmpty());

		SubtitleDescriptor sample = list.get(0);

		assertEquals("English", sample.getLanguageName());

		// check size
		assertTrue(list.size() > 0);
	}

	@Test
	public void getSubtitleListMovieHash() throws Exception {
		List<OpenSubtitlesSubtitleDescriptor> list = xmlrpc.searchSubtitles(singleton(Query.forHash("2bba5c34b007153b", 717565952, "eng")));
		assumeFalse(list.isEmpty());

		OpenSubtitlesSubtitleDescriptor sample = list.get(0);

		assertTrue(sample.getProperty(Property.SubFileName).toLowerCase().endsWith(".srt"));
		assertEquals("English", sample.getProperty(Property.LanguageName));
		assertEquals("moviehash", sample.getProperty(Property.MatchedBy));
	}

	@Test
	public void tryUploadSubtitles() throws Exception {
		SubFile subtitle = new SubFile();
		subtitle.setSubFileName("firefly.s01e01.serenity.pilot.dvdrip.xvid.srt");
		subtitle.setSubHash("6d9c600fb8b07f87ffcf156e4ed308ca");
		subtitle.setMovieFileName("firefly.s01e01.serenity.pilot.dvdrip.xvid.avi");
		subtitle.setMovieHash("2bba5c34b007153b");
		subtitle.setMovieByteSize(717565952);

		TryUploadResponse response = xmlrpc.tryUploadSubtitles(subtitle);

		assertFalse(response.isUploadRequired());
		assertEquals("4513264", response.getSubtitleData().get(0).get(Property.IDSubtitle.toString()));
		assertEquals("eng", response.getSubtitleData().get(0).get(Property.SubLanguageID.toString()));
	}

	@Test
	public void checkSubHash() throws Exception {
		Map<String, Integer> subHashMap = xmlrpc.checkSubHash(singleton("e12715f466ee73c86694b7ab9f311285"));

		assertEquals("247060", subHashMap.values().iterator().next().toString());
		assertTrue(1 == subHashMap.size());
	}

	@Test
	public void checkSubHashInvalid() throws Exception {
		Map<String, Integer> subHashMap = xmlrpc.checkSubHash(singleton("0123456789abcdef0123456789abcdef"));

		assertEquals("0", subHashMap.values().iterator().next().toString());
		assertTrue(1 == subHashMap.size());
	}

	@Test
	public void checkMovieHash() throws Exception {
		Map<String, Movie> results = xmlrpc.checkMovieHash(singleton("d7aa0275cace4410"), 0);
		Movie movie = results.get("d7aa0275cace4410");

		assertEquals("Iron Man", movie.getName());
		assertEquals(2008, movie.getYear());
		assertEquals(371746, movie.getImdbId());
	}

	@Test
	public void checkMovieHashInvalid() throws Exception {
		Map<String, Movie> results = xmlrpc.checkMovieHash(singleton("0123456789abcdef"), 0);

		// no movie info
		assertTrue(results.isEmpty());
	}

	@Test
	public void getIMDBMovieDetails() throws Exception {
		Movie movie = xmlrpc.getIMDBMovieDetails(371746);
		assumeNotNull(movie);

		assertEquals("Iron Man", movie.getName());
		assertEquals(2008, movie.getYear());
		assertEquals(371746, movie.getImdbId());
	}

	@Test
	public void getIMDBMovieDetailsInvalid() throws Exception {
		try {
			Movie movie = xmlrpc.getIMDBMovieDetails(0);
			assertNull(movie);
		} catch (XmlRpcFault e) {
			assertEquals(408, e.getErrorCode());
		}
	}

	@Test
	public void detectLanguage() throws Exception {
		String text = "Only those that are prepared to fire should be fired at.";

		List<String> languages = xmlrpc.detectLanguage(text.getBytes("UTF-8"));

		assertEquals("eng", languages.get(0));
		assertTrue(1 == languages.size());
	}

	@Test
	public void fetchSubtitle() throws Exception {
		List<OpenSubtitlesSubtitleDescriptor> list = xmlrpc.searchSubtitles(singleton(Query.forImdbId(361256, -1, -1, "eng")));
		assumeFalse(list.isEmpty());

		// check format
		assertEquals("srt", list.get(0).getType());

		// fetch subtitle file
		ByteBuffer data = list.get(0).fetch();

		// check size
		assertTrue(data.remaining() > 0);
	}

	@Ignore
	@Test(expected = IOException.class)
	public void fetchSubtitlesExceedLimit() throws Exception {
		List<OpenSubtitlesSubtitleDescriptor> list = xmlrpc.searchSubtitles(singleton(Query.forImdbId(773262, -1, -1, "eng")));

		for (int i = 0; true; i++) {
			System.out.format("Fetch #%d: %s%n", i, list.get(i).fetch());
		}
	}

	@AfterClass
	public static void logout() throws Exception {
		// logout manually
		xmlrpc.logout();
	}

}
