package org.openfilebot.format;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import org.openfilebot.web.Episode;
import org.openfilebot.web.Movie;
import org.openfilebot.web.MoviePart;
import org.openfilebot.web.MultiEpisode;
import org.openfilebot.web.SeriesInfo;

public class MediaBindingBeanCompatibilityTest {

	private Set<String> getDefineKeys() {
		return Arrays.stream(MediaBindingBean.class.getMethods()).map(m -> m.getAnnotation(Define.class)).filter(Objects::nonNull).flatMap(a -> Arrays.stream(a.value())).filter(k -> k != null && k.length() > 0).collect(Collectors.toSet());
	}

	private Set<String> getPreviewExpressionRoots() {
		String expressions = ResourceBundle.getBundle("org.openfilebot.ui.rename.BindingDialog").getString("expressions");

		Set<String> roots = new HashSet<String>();
		for (String expression : expressions.split(",")) {
			String token = expression.trim();
			if (token.isEmpty()) {
				continue;
			}

			roots.add(token);
			roots.add(token.split("\\.", 2)[0]);
			roots.add(token.split("\\[", 2)[0]);
		}

		return roots;
	}

	@Test
	public void includesExpectedCompatibilityBindings() {
		Set<String> keys = getDefineKeys();

		assertTrue(keys.contains("ci"));
		assertTrue(keys.contains("hdr"));
		assertTrue(keys.contains("kodi"));
		assertTrue(keys.contains("ffprobe"));
		assertTrue(keys.contains("photo"));
	}

	@Test
	public void bindingsAreListedInPreviewOrExplicitlyExcluded() {
		Set<String> keys = getDefineKeys();
		Set<String> preview = getPreviewExpressionRoots();

		Set<String> excluded = new HashSet<String>(Arrays.asList("object", "xattr", "ci", "episodelist", "menu", "image", "chapters", "exif", "f", "home", "output", "defines", "label", "i", "self", "model", "json"));

		Set<String> missing = keys.stream().filter(k -> !preview.contains(k)).collect(Collectors.toSet());
		Set<String> notExplicitlyExcluded = missing.stream().filter(k -> !excluded.contains(k)).collect(Collectors.toSet());

		assertTrue("Bindings missing from BindingDialog expressions without explicit exclusion: " + notExplicitlyExcluded, notExplicitlyExcluded.isEmpty());
	}

	@Test
	public void collectionIndexUsesMoviePartIndex() {
		Movie movie = new Movie("Avatar", 2009);
		MoviePart part = new MoviePart(movie, 2, 3);
		MediaBindingBean bean = new MediaBindingBean(part, new File("Avatar.CD2.mkv"));

		assertEquals(Integer.valueOf(2), bean.getCollectionIndex());
	}

	@Test
	public void kodiPathUsesExpandedMultiEpisodeSxE() throws Exception {
		SeriesInfo seriesInfo = new SeriesInfo();
		seriesInfo.setDatabase("TheTVDB");
		seriesInfo.setName("Demo Show");

		Episode e1 = new Episode("Demo Show", 1, 1, "Alpha", null, null, null, 1, seriesInfo);
		Episode e2 = new Episode("Demo Show", 1, 2, "Beta", null, null, null, 2, seriesInfo);
		Episode e3 = new Episode("Demo Show", 1, 3, "Gamma", null, null, null, 3, seriesInfo);

		MultiEpisode multi = new MultiEpisode(Arrays.asList(e1, e2, e3));
		MediaBindingBean bean = new MediaBindingBean(multi, new File("Demo.Show.S01E01-03.mkv"));

		String path = bean.getKodiStandardPath().getPath();
		assertEquals("TV Shows/Demo Show/Season 01/Demo Show - 1x01x02x03 - Alpha & Beta & Gamma", path);
		assertFalse(path.contains("S01E01-E03"));
	}

	@Test
	public void kodiPathForSingleEpisodeUsesSxEAndExpectedFolders() throws Exception {
		SeriesInfo seriesInfo = new SeriesInfo();
		seriesInfo.setDatabase("TheTVDB");
		seriesInfo.setName("Demo Show");

		Episode episode = new Episode("Demo Show", 1, 2, "Pilot", null, null, null, 2, seriesInfo);
		MediaBindingBean bean = new MediaBindingBean(episode, new File("Demo.Show.S01E02.mkv"));

		assertEquals("TV Shows/Demo Show/Season 01/Demo Show - 1x02 - Pilot", bean.getKodiStandardPath().getPath());
	}
}
