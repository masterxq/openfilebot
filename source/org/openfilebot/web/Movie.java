package org.openfilebot.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Movie extends SearchResult {

	protected Integer year;
	protected int imdbId;
	protected int tmdbId;

	// optional movie name language hint
	protected String language;

	public Movie() {
		// used by deserializer
	}

	public Movie(int imdbId) {
		this(null, null, (Integer) null, imdbId, 0, null);
	}

	public Movie(String name, int year) {
		this(name, null, Integer.valueOf(year), 0, 0, null);
	}

	public Movie(String name, int year, int imdbId) {
		this(name, null, Integer.valueOf(year), imdbId, 0, null);
	}

	public Movie(String name, String[] aliasNames, int year, int imdbId, int tmdbId, Locale locale) {
		this(name, aliasNames, Integer.valueOf(year), imdbId, tmdbId, locale);
	}

	public Movie(String name, String[] aliasNames, Integer year, int imdbId, int tmdbId, Locale locale) {
		super(tmdbId > 0 ? tmdbId : imdbId > 0 ? imdbId : 0, name, aliasNames);
		this.year = normalizeYear(year);
		this.imdbId = imdbId;
		this.tmdbId = tmdbId;
		this.language = locale == null ? null : locale.getLanguage();
	}

	public Integer getYear() {
		return year;
	}

	public int getImdbId() {
		return imdbId;
	}

	public int getTmdbId() {
		return tmdbId;
	}

	public Locale getLanguage() {
		return language == null ? null : new Locale(language);
	}

	public String getNameWithYear() {
		return toString(name, year);
	}

	@Override
	public List<String> getEffectiveNames() {
		if (aliasNames == null || aliasNames.length == 0) {
			return Collections.singletonList(toString(name, year));
		}

		List<String> names = new ArrayList<String>(1 + aliasNames.length);
		names.add(toString(name, year));
		for (String alias : aliasNames) {
			names.add(toString(alias, year));
		}
		return names;
	}

	public List<String> getEffectiveNamesWithoutYear() {
		return super.getEffectiveNames();
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Movie) {
			Movie other = (Movie) object;

			if (tmdbId > 0 && other.tmdbId > 0) {
				return tmdbId == other.tmdbId;
			}
			if (imdbId > 0 && other.imdbId > 0) {
				return imdbId == other.imdbId;
			}

			return Objects.equals(year, other.year) && name.equals(other.name);
		}

		return false;
	}

	@Override
	public Movie clone() {
		return new Movie(name, aliasNames, year, imdbId, tmdbId, getLanguage());
	}

	@Override
	public String toString() {
		return toString(name, year);
	}

	private static String toString(String name, Integer year) {
		if (year == null) {
			return name;
		}

		return String.format("%s (%04d)", name, year < 0 ? 0 : year);
	}

	private static Integer normalizeYear(Integer year) {
		return year != null && year > 0 ? year : null;
	}

}
