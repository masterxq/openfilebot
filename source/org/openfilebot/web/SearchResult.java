package org.openfilebot.web;

import static java.util.Collections.*;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;

public class SearchResult implements Serializable {

	private static final long serialVersionUID = -4071916423697364893L;

	protected int id;
	protected String name;
	protected String[] aliasNames;
	protected Integer year;

	public SearchResult() {
		// used by serializer
	}

	public SearchResult(int id) {
		this(id, null, EMPTY_STRING_ARRAY);
	}

	public SearchResult(int id, String name) {
		this(id, name, EMPTY_STRING_ARRAY);
	}

	public SearchResult(int id, String name, Integer year) {
		this(id, name, EMPTY_STRING_ARRAY, year);
	}

	public SearchResult(int id, String name, Collection<String> aliasNames) {
		this(id, name, aliasNames.toArray(EMPTY_STRING_ARRAY));
	}

	public SearchResult(int id, String name, Collection<String> aliasNames, Integer year) {
		this(id, name, aliasNames.toArray(EMPTY_STRING_ARRAY), year);
	}

	public SearchResult(int id, String name, String[] aliasNames) {
		this(id, name, aliasNames, null);
	}

	public SearchResult(int id, String name, String[] aliasNames, Integer year) {
		this.id = id;
		this.name = name;
		this.aliasNames = aliasNames == null || aliasNames.length == 0 ? EMPTY_STRING_ARRAY : aliasNames.clone();
		this.year = normalizeYear(year);
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String[] getAliasNames() {
		return aliasNames.clone();
	}

	public Integer getYear() {
		return year;
	}

	public List<String> getEffectiveNames() {
		if (name == null || name.length() == 0) {
			return emptyList();
		}
		if (aliasNames == null || aliasNames.length == 0) {
			return singletonList(name);
		}
		return new AbstractList<String>() {

			@Override
			public String get(int index) {
				return index == 0 ? name : aliasNames[index - 1];
			}

			@Override
			public int size() {
				return 1 + aliasNames.length;
			}
		};
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object other) {
		if (getClass().isInstance(other)) {
			return getId() == ((SearchResult) other).getId();
		}
		return false;
	}

	@Override
	public String toString() {
		return name != null ? name : String.valueOf(id);
	}

	@Override
	public SearchResult clone() {
		return new SearchResult(id, name, aliasNames, year);
	}

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private static Integer normalizeYear(Integer year) {
		return year != null && year > 0 ? year : null;
	}

}
