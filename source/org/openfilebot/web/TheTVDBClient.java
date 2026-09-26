package org.openfilebot.web;

import static java.nio.charset.StandardCharsets.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.openfilebot.Logging.*;
import static org.openfilebot.util.JsonUtilities.*;
import static org.openfilebot.util.StringUtilities.*;
import static org.openfilebot.web.EpisodeUtilities.*;
import static org.openfilebot.web.WebRequest.*;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.Icon;

import org.openfilebot.Cache;
import org.openfilebot.CacheType;
import org.openfilebot.CachedResource.Fetch;
import org.openfilebot.ResourceManager;

/**
 * TheTVDB API v4 client.
 *
 * @see https://thetvdb.github.io/v4-api/
 */
public class TheTVDBClient extends AbstractEpisodeListProvider implements ArtworkProvider {

	private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
	private static final String DEFAULT_LANGUAGE = "eng";

	private static final String API_ENDPOINT = "https://api4.thetvdb.com/v4/";
	private static final String ARTWORK_ENDPOINT = "https://artworks.thetvdb.com/banners/";

	private final String apikey;
	private final String pin;

	public TheTVDBClient(String apikey) {
		this(apikey, null);
	}

	/**
	 * @param apikey
	 *            v4 project API key
	 * @param pin
	 *            optional subscriber PIN (only required for user-supported project keys)
	 */
	public TheTVDBClient(String apikey, String pin) {
		this.apikey = apikey;
		this.pin = pin == null || pin.isEmpty() ? null : pin;
	}

	@Override
	public String getIdentifier() {
		return "TheTVDB";
	}

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.thetvdb");
	}

	@Override
	public boolean hasSeasonSupport() {
		return true;
	}

	protected URL getEndpoint(String path) throws Exception {
		return new URL(API_ENDPOINT + path);
	}

	// ---------------------------------------------------------------------------------------------------------------
	// authentication
	// ---------------------------------------------------------------------------------------------------------------

	private final Object tokenLock = new Object();
	private String token = null;
	private Instant tokenExpireInstant = null;

	// v4 tokens are valid for one month, but a daily refresh is cheap and keeps us safe against server-side revocation
	private static final Duration TOKEN_LIFETIME = Duration.ofHours(23);

	private String getAuthorizationToken() {
		synchronized (tokenLock) {
			if (token == null || (tokenExpireInstant != null && Instant.now().isAfter(tokenExpireInstant))) {
				try {
					Map<String, Object> login = new LinkedHashMap<String, Object>(2);
					login.put("apikey", apikey);
					if (pin != null) {
						login.put("pin", pin);
					}

					Object json = postJson("login", login);
					String value = getString(getMap(json, "data"), "token");
					if (value == null) {
						throw new IllegalStateException(getString(json, "message"));
					}

					token = value;
					tokenExpireInstant = Instant.now().plus(TOKEN_LIFETIME);
				} catch (Exception e) {
					throw new IllegalStateException("Failed to retrieve authorization token: " + e.getMessage(), e);
				}
			}
			return token;
		}
	}

	private void invalidateAuthorizationToken() {
		synchronized (tokenLock) {
			token = null;
			tokenExpireInstant = null;
		}
	}

	private Map<String, String> getRequestHeader() {
		Map<String, String> header = new LinkedHashMap<String, String>(2);
		header.put("Accept", "application/json");
		header.put("Authorization", "Bearer " + getAuthorizationToken());
		return header;
	}

	// ---------------------------------------------------------------------------------------------------------------
	// requests
	// ---------------------------------------------------------------------------------------------------------------

	protected Object postJson(String path, Object object) throws Exception {
		ByteBuffer response = post(getEndpoint(path), json(object, false).getBytes(UTF_8), "application/json", singletonMap("Accept", "application/json"));
		return readJson(UTF_8.decode(response));
	}

	/**
	 * Fetch and cache a JSON resource. HTTP errors (including 404 Not Found) are thrown as exceptions and never cached, so a temporary API outage does not poison the cache with empty results.
	 */
	// separate cache namespace so cached v2 responses and search results are never reused for v4
	private static final String CACHE_NAMESPACE = "_v4";

	@Override
	protected Cache getCache(String section) {
		return Cache.getCache(getName() + CACHE_NAMESPACE + "_" + section, CacheType.Daily);
	}

	protected Object requestJson(String path, Duration expirationTime) throws Exception {
		Cache cache = Cache.getCache(getName() + CACHE_NAMESPACE, CacheType.Monthly);

		Fetch fetch = (url, lastModified) -> {
			debug.fine(WebRequest.log(url, lastModified, null));
			return WebRequest.fetch(url, lastModified, null, getRequestHeader(), null);
		};

		try {
			return cache.json(path, this::getEndpoint).fetch(fetch).expire(expirationTime).get();
		} catch (Exception e) {
			// token may have been revoked server-side => login again and retry once
			if (isUnauthorized(e)) {
				debug.fine("Authorization token rejected, requesting new token");
				invalidateAuthorizationToken();
				return cache.json(path, this::getEndpoint).fetch(fetch).expire(expirationTime).get();
			}
			throw e;
		}
	}

	/**
	 * Fetch the {@code data} node of a JSON resource, or an empty map if the resource does not exist.
	 */
	protected Object requestData(String path, Duration expirationTime) throws Exception {
		try {
			return asMap(requestJson(path, expirationTime)).get("data");
		} catch (Exception e) {
			if (isNotFound(e)) {
				debug.warning(format("Resource not found: %s", path));
				return emptyMap();
			}
			throw e;
		}
	}

	private static boolean isNotFound(Throwable e) {
		for (Throwable current = e; current != null; current = current.getCause()) {
			if (current instanceof FileNotFoundException) {
				return true;
			}
		}
		return false;
	}

	private static boolean isUnauthorized(Throwable e) {
		for (Throwable current = e; current != null; current = current.getCause()) {
			String message = current.getMessage();
			if (message != null && message.contains("response code: 401")) {
				return true;
			}
		}
		return false;
	}

	// ---------------------------------------------------------------------------------------------------------------
	// language codes
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * Map Java Locale to TheTVDB v4 language code (ISO 639-3 with a few TheTVDB-specific exceptions).
	 */
	protected String getLanguageCode(Locale locale) {
		if (locale == null || locale.getLanguage().isEmpty()) {
			return DEFAULT_LANGUAGE;
		}

		String language = locale.getLanguage();
		String country = locale.getCountry();

		// TheTVDB-specific codes
		if ("pt".equals(language) && "BR".equals(country)) {
			return "pt"; // Portuguese - Brazil (Portuguese - Portugal is "por")
		}
		if ("zh".equals(language) && ("TW".equals(country) || "HK".equals(country) || "Hant".equals(locale.getScript()))) {
			return "zhtw"; // Chinese - Taiwan
		}

		try {
			return locale.getISO3Language(); // e.g. de => deu, iw/he => heb, in/id => ind
		} catch (MissingResourceException e) {
			return language;
		}
	}

	private static final Map<String, Locale> LOCALE_BY_LANGUAGE_CODE = new HashMap<String, Locale>();

	/**
	 * Map TheTVDB v4 language code back to a Java Locale.
	 */
	protected Locale getLocale(String code) {
		if (code == null || code.isEmpty()) {
			return null;
		}

		synchronized (LOCALE_BY_LANGUAGE_CODE) {
			if (LOCALE_BY_LANGUAGE_CODE.isEmpty()) {
				for (String language : Locale.getISOLanguages()) {
					Locale locale = new Locale(language);
					try {
						LOCALE_BY_LANGUAGE_CODE.putIfAbsent(locale.getISO3Language(), locale);
					} catch (MissingResourceException e) {
						// ignore languages without ISO3 code
					}
				}
				LOCALE_BY_LANGUAGE_CODE.put("pt", new Locale("pt", "BR"));
				LOCALE_BY_LANGUAGE_CODE.put("zhtw", Locale.TAIWAN);
			}
			return LOCALE_BY_LANGUAGE_CODE.getOrDefault(code, new Locale(code));
		}
	}

	private static String getTranslation(Object translations, String key, String language, String field) {
		return streamJsonObjects(translations, key).filter(it -> language.equals(getString(it, "language"))).map(it -> getString(it, field)).filter(Objects::nonNull).findFirst().orElse(null);
	}

	// ---------------------------------------------------------------------------------------------------------------
	// search
	// ---------------------------------------------------------------------------------------------------------------

	@Override
	public List<SearchResult> fetchSearchResult(String query, Locale locale) throws Exception {
		String language = getLanguageCode(locale);

		Map<String, Object> parameters = new LinkedHashMap<String, Object>(2);
		parameters.put("query", query);
		parameters.put("type", "series");

		Object json = requestJson("search?" + encodeParameters(parameters, true), Cache.ONE_DAY);
		if (!asMap(json).containsKey("data")) {
			throw new IllegalStateException(String.format("TheTVDB search failed: %s", getString(json, "message")));
		}

		return streamJsonObjects(json, "data").map(it -> {
			// e.g. tvdb_id, name, aliases, translations, year, first_air_time, status, overview
			Integer id = getInteger(it, "tvdb_id");
			String primaryName = getString(it, "name");

			if (id == null || primaryName == null) {
				debug.warning(format("Ignore invalid series: %s", it));
				return null;
			}

			Map<?, ?> translations = getMap(it, "translations");
			String name = getString(translations, language);
			if (name == null) {
				name = primaryName;
			}

			Set<String> aliasNames = new LinkedHashSet<String>();
			aliasNames.add(primaryName);
			aliasNames.add(getString(translations, DEFAULT_LANGUAGE));
			stream(getArray(it, "aliases")).map(Object::toString).forEach(aliasNames::add);
			aliasNames.remove(null);
			aliasNames.remove(name);

			Integer year = getInteger(it, "year");
			if (year == null) {
				SimpleDate firstAired = getStringValue(it, "first_air_time", SimpleDate::parse);
				year = firstAired == null ? null : firstAired.getYear();
			}

			return new SearchResult(id, name, aliasNames.toArray(new String[0]), year);
		}).filter(Objects::nonNull).collect(toList());
	}

	public SearchResult lookupByID(int id, Locale locale) throws Exception {
		if (id <= 0) {
			throw new IllegalArgumentException("Illegal TheTVDB ID: " + id);
		}

		SeriesInfo info = getSeriesInfo(new SearchResult(id), locale);
		return new SearchResult(id, info.getName(), info.getAliasNames());
	}

	public SearchResult lookupByIMDbID(int imdbid, Locale locale) throws Exception {
		if (imdbid <= 0) {
			throw new IllegalArgumentException("Illegal IMDbID ID: " + imdbid);
		}

		Object data = requestData(String.format("search/remoteid/tt%07d", imdbid), Cache.ONE_MONTH);

		return streamJsonObjects(data).map(it -> getMap(it, "series")).filter(it -> it.size() > 0).map(it -> {
			int id = getInteger(it, "id");
			String name = getString(it, "name");
			String[] aliasNames = streamJsonObjects(it, "aliases").map(a -> getString(a, "name")).filter(Objects::nonNull).distinct().toArray(String[]::new);
			return new SearchResult(id, name, aliasNames);
		}).findFirst().orElse(null);
	}

	// ---------------------------------------------------------------------------------------------------------------
	// series info
	// ---------------------------------------------------------------------------------------------------------------

	@Override
	public TheTVDBSeriesInfo getSeriesInfo(int id, Locale language) throws Exception {
		return getSeriesInfo(new SearchResult(id), language);
	}

	@Override
	public TheTVDBSeriesInfo getSeriesInfo(SearchResult series, Locale locale) throws Exception {
		String language = getLanguageCode(locale);

		// short=true omits artworks, characters and episodes, translations are embedded via meta=translations
		Object data = requestData("series/" + series.getId() + "/extended?meta=translations&short=true", Cache.ONE_WEEK);
		Object translations = getMap(data, "translations");

		TheTVDBSeriesInfo info = new TheTVDBSeriesInfo(this, locale, series.getId());

		// localized name and overview (default to primary name if the translation is not available)
		String primaryName = getString(data, "name");
		String name = getTranslation(translations, "nameTranslations", language, "name");
		info.setName(name != null ? name : primaryName);

		String overview = getTranslation(translations, "overviewTranslations", language, "overview");
		info.setOverview(overview != null ? overview : getString(data, "overview"));

		// aliases: search result aliases, primary name, English name and all TheTVDB aliases
		Set<String> aliasNames = new LinkedHashSet<String>(asList(series.getAliasNames()));
		aliasNames.add(primaryName);
		aliasNames.add(getTranslation(translations, "nameTranslations", DEFAULT_LANGUAGE, "name"));
		streamJsonObjects(data, "aliases").map(it -> getString(it, "name")).forEach(aliasNames::add);
		aliasNames.remove(null);
		aliasNames.remove(info.getName());
		info.setAliasNames(aliasNames.toArray(new String[0]));

		// prefer US content rating (e.g. TV-14) but accept any rating
		Map<?, ?>[] contentRatings = getMapArray(data, "contentRatings");
		info.setCertification(stream(contentRatings).filter(it -> "usa".equals(getString(it, "country"))).map(it -> getString(it, "name")).filter(Objects::nonNull).findFirst().orElseGet(() -> contentRatings.length > 0 ? getString(contentRatings[0], "name") : null));

		String network = getString(getMap(data, "originalNetwork"), "name");
		info.setNetwork(network != null ? network : getString(getMap(data, "latestNetwork"), "name"));
		info.setStatus(getString(getMap(data, "status"), "name"));

		// API v4 does not expose user ratings (score is a popularity value, not a rating)
		info.setRating(null);
		info.setRatingCount(null);

		info.setRuntime(getInteger(data, "averageRuntime"));
		info.setGenres(streamJsonObjects(data, "genres").map(it -> getString(it, "name")).filter(Objects::nonNull).collect(toList()));
		info.setStartDate(getStringValue(data, "firstAired", SimpleDate::parse));

		// TheTVDB SeriesInfo extras
		info.setImdbId(streamJsonObjects(data, "remoteIds").filter(it -> "IMDB".equalsIgnoreCase(getString(it, "sourceName"))).map(it -> getString(it, "id")).filter(Objects::nonNull).findFirst().orElse(null));
		info.setAirsDayOfWeek(getAirsDayOfWeek(getMap(data, "airsDays")));
		info.setAirsTime(getString(data, "airsTime"));
		info.setBannerUrl(null); // not included in the short series record, use getArtwork(id, "banner", locale) instead
		info.setLastUpdated(getStringValue(data, "lastUpdated", TheTVDBClient::parseTimestamp));

		return info;
	}

	private static final List<String> DAYS_OF_WEEK = List.of("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday");

	private static String getAirsDayOfWeek(Map<?, ?> airsDays) {
		return DAYS_OF_WEEK.stream().filter(day -> Boolean.TRUE.equals(airsDays.get(day))).map(day -> Character.toUpperCase(day.charAt(0)) + day.substring(1)).findFirst().orElse(null);
	}

	private static Long parseTimestamp(String value) {
		// e.g. 2026-09-26 06:43:46
		return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toEpochSecond(ZoneOffset.UTC);
	}

	// ---------------------------------------------------------------------------------------------------------------
	// episode list
	// ---------------------------------------------------------------------------------------------------------------

	@Override
	protected SeriesData fetchSeriesData(SearchResult series, SortOrder sortOrder, Locale locale) throws Exception {
		// fetch series info
		TheTVDBSeriesInfo info = getSeriesInfo(series, locale);
		info.setOrder(sortOrder.name());

		// ignore preferred language if basic series information isn't even available
		if (info.getName() == null) {
			if (!locale.equals(DEFAULT_LOCALE)) {
				return fetchSeriesData(series, sortOrder, DEFAULT_LOCALE);
			}

			debug.warning(format("Series not found: %s [%d]", series.getName(), series.getId()));

			SeriesInfo notFound = new SeriesInfo(this, sortOrder, locale, series.getId());
			notFound.setName(series.getName());
			return new SeriesData(notFound, emptyList());
		}

		String language = getLanguageCode(locale);
		List<Map<?, ?>> records = fetchEpisodeRecords(series.getId(), getSeasonType(sortOrder), language);

		// series may not have a dedicated absolute order
		if (records.isEmpty() && sortOrder == SortOrder.Absolute) {
			records = fetchEpisodeRecords(series.getId(), getSeasonType(SortOrder.Airdate), language);
		}

		// default to English episode titles if the preferred language is not available
		Map<Integer, String> defaultTitles = emptyMap();
		if (!locale.equals(DEFAULT_LOCALE) && records.stream().anyMatch(it -> getString(it, "name") == null)) {
			try {
				defaultTitles = getEpisodeList(series, sortOrder, DEFAULT_LOCALE).stream().filter(e -> e.getId() != null && e.getTitle() != null).collect(toMap(Episode::getId, Episode::getTitle, (a, b) -> a));
			} catch (Exception e) {
				debug.warning(cause("Failed to retrieve default episode titles", e));
			}
		}

		List<Episode> episodes = new ArrayList<Episode>();
		List<Episode> specials = new ArrayList<Episode>();

		for (Map<?, ?> it : records) {
			Integer id = getInteger(it, "id");
			String episodeName = getString(it, "name");
			if (episodeName == null) {
				episodeName = defaultTitles.get(id);
			}

			Integer absoluteNumber = getInteger(it, "absoluteNumber");
			if (absoluteNumber != null && absoluteNumber <= 0) {
				absoluteNumber = null;
			}

			SimpleDate airdate = getStringValue(it, "aired", SimpleDate::parse);

			// numbering according to the requested season type
			Integer seasonNumber = getInteger(it, "seasonNumber");
			Integer episodeNumber = getInteger(it, "number");

			if (seasonNumber != null && seasonNumber <= 0) {
				// handle as special episode
				specials.add(new Episode(info.getName(), null, null, episodeName, absoluteNumber, episodeNumber, airdate, id, new SeriesInfo(info)));
				continue;
			}

			if (sortOrder == SortOrder.Absolute) {
				// absolute order lists all episodes as season 1 with number = absolute number
				seasonNumber = null;
				if (absoluteNumber == null) {
					absoluteNumber = episodeNumber;
				}
			} else if (sortOrder == SortOrder.AbsoluteAirdate && airdate != null) {
				// use airdate as absolute episode number
				seasonNumber = null;
				episodeNumber = airdate.getYear() * 1_00_00 + airdate.getMonth() * 1_00 + airdate.getDay();
			}

			episodes.add(new Episode(info.getName(), seasonNumber, episodeNumber, episodeName, absoluteNumber, null, airdate, id, new SeriesInfo(info)));
		}

		// episodes may not be ordered by DVD episode number
		episodes.sort(episodeComparator());

		// add specials at the end
		episodes.addAll(specials);

		return new SeriesData(info, episodes);
	}

	protected String getSeasonType(SortOrder sortOrder) {
		switch (sortOrder) {
		case DVD:
			return "dvd";
		case Absolute:
			return "absolute";
		default:
			return "official";
		}
	}

	protected List<Map<?, ?>> fetchEpisodeRecords(int seriesId, String seasonType, String language) throws Exception {
		List<Map<?, ?>> records = new ArrayList<Map<?, ?>>();

		for (int page = 0; page < 100; page++) {
			Object json;
			try {
				json = requestJson("series/" + seriesId + "/episodes/" + seasonType + "/" + language + "?page=" + page, Cache.ONE_DAY);
			} catch (Exception e) {
				if (isNotFound(e)) {
					break;
				}
				throw e;
			}

			streamJsonObjects(getMap(json, "data"), "episodes").forEach(records::add);

			if (getString(getMap(json, "links"), "next") == null) {
				break;
			}
		}

		return records;
	}

	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		return URI.create("https://www.thetvdb.com/?tab=seasonall&id=" + searchResult.getId());
	}

	// ---------------------------------------------------------------------------------------------------------------
	// artwork
	// ---------------------------------------------------------------------------------------------------------------

	private static final Map<String, Integer> ARTWORK_TYPES = new LinkedHashMap<String, Integer>();

	static {
		// see https://api4.thetvdb.com/v4/artwork/types
		Stream.of("banner", "banners", "series", "graphical").forEach(it -> ARTWORK_TYPES.put(it, 1));
		Stream.of("poster", "posters").forEach(it -> ARTWORK_TYPES.put(it, 2));
		Stream.of("fanart", "background", "backgrounds").forEach(it -> ARTWORK_TYPES.put(it, 3));
		Stream.of("icon", "icons").forEach(it -> ARTWORK_TYPES.put(it, 5));
		Stream.of("seasonwide", "seasonbanner", "seasonbanners").forEach(it -> ARTWORK_TYPES.put(it, 6));
		Stream.of("season", "seasonposter", "seasonposters").forEach(it -> ARTWORK_TYPES.put(it, 7));
		Stream.of("seasonbackground", "seasonbackgrounds").forEach(it -> ARTWORK_TYPES.put(it, 8));
		ARTWORK_TYPES.put("clearart", 22);
		ARTWORK_TYPES.put("clearlogo", 23);
	}

	@Override
	public List<Artwork> getArtwork(int id, String category, Locale locale) throws Exception {
		Integer type = ARTWORK_TYPES.get(category.toLowerCase(Locale.ROOT));
		if (type == null) {
			debug.warning(format("Unsupported artwork category: %s (supported: %s)", category, ARTWORK_TYPES.keySet()));
			return emptyList();
		}

		Object data = requestData("series/" + id + "/artworks?type=" + type, Cache.ONE_MONTH);
		String language = getLanguageCode(locale);

		// prefer artwork in the requested language (or language-neutral artwork), then sort by score
		Comparator<Artwork> order = Comparator.<Artwork, Boolean> comparing(it -> it.getLanguage() != null && !language.equals(getLanguageCode(it.getLanguage()))).thenComparing(Artwork.RATING_ORDER);

		return streamJsonObjects(data, "artworks").map(it -> {
			URL url = getStringValue(it, "image", this::resolveImage);
			if (url == null) {
				return null;
			}

			Integer width = getInteger(it, "width");
			Integer height = getInteger(it, "height");
			String resolution = width != null && height != null && width > 0 && height > 0 ? width + "x" + height : null;

			Double score = getDecimal(it, "score");

			return new Artwork(Stream.of(category, resolution).filter(Objects::nonNull), url, getLocale(getString(it, "language")), score == null ? 0 : score);
		}).filter(Objects::nonNull).sorted(order).collect(toList());
	}

	protected URL resolveImage(String path) {
		if (path == null || path.isEmpty()) {
			return null;
		}

		try {
			return new URL(path.startsWith("http") ? path : ARTWORK_ENDPOINT + path);
		} catch (Exception e) {
			throw new IllegalArgumentException(path, e);
		}
	}

	// ---------------------------------------------------------------------------------------------------------------
	// misc
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * @return list of TheTVDB v4 language codes (e.g. eng, deu, jpn)
	 */
	public List<String> getLanguages() throws Exception {
		Object data = requestData("languages", Cache.ONE_MONTH);
		return streamJsonObjects(data).map(it -> getString(it, "id")).filter(Objects::nonNull).collect(toList());
	}

	public List<Person> getActors(int seriesId, Locale locale) throws Exception {
		// characters are only included in the full series record
		Object data = requestData("series/" + seriesId + "/extended", Cache.ONE_MONTH);

		return streamJsonObjects(data, "characters").filter(it -> Person.ACTOR.equalsIgnoreCase(getString(it, "peopleType"))).map(this::getPerson).filter(Objects::nonNull).sorted(Person.CREDIT_ORDER).collect(toList());
	}

	public EpisodeInfo getEpisodeInfo(int id, Locale locale) throws Exception {
		String language = getLanguageCode(locale);

		Object data = requestData("episodes/" + id + "/extended?meta=translations", Cache.ONE_MONTH);

		Integer seriesId = getInteger(data, "seriesId");

		String overview = getTranslation(getMap(data, "translations"), "overviewTranslations", language, "overview");
		if (overview == null) {
			overview = getString(data, "overview");
		}

		List<Person> people = streamJsonObjects(data, "characters").map(this::getPerson).filter(Objects::nonNull).sorted(Person.CREDIT_ORDER).collect(toList());

		// API v4 does not expose episode ratings
		return new EpisodeInfo(this, locale, seriesId, id, people, overview, null, null);
	}

	private Person getPerson(Map<?, ?> it) {
		// e.g. personName, name (character), peopleType, sort, image, personImgURL
		String name = getString(it, "personName");
		String job = getString(it, "peopleType");
		if (name == null || job == null) {
			return null;
		}

		String character = getString(it, "name");
		Integer order = getInteger(it, "sort");

		URL image = getStringValue(it, "image", this::resolveImage);
		if (image == null) {
			image = getStringValue(it, "personImgURL", this::resolveImage);
		}

		// normalize job names to Person constants
		for (String knownJob : new String[] { Person.ACTOR, Person.DIRECTOR, Person.WRITER, Person.GUEST_STAR }) {
			if (knownJob.equalsIgnoreCase(job)) {
				job = knownJob;
			}
		}

		return new Person(name, character, job, null, order, image);
	}

}
