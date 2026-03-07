
package org.openfilebot.ui.rename;

import static org.openfilebot.util.FileUtilities.*;

import java.util.Map;

import org.openfilebot.similarity.Match;
import org.openfilebot.web.Movie;
import org.openfilebot.web.MoviePart;

class MovieFormatter implements MatchFormatter {

	@Override
	public boolean canFormat(Match<?, ?> match) {
		return match.getValue() instanceof Movie;
	}

	@Override
	public String preview(Match<?, ?> match) {
		Movie movie = (Movie) match.getValue();
		StringBuilder name = new StringBuilder();
		Integer year = movie.getYear();

		// format as single-file or multi-part movie
		name.append(movie.getName());
		if (year != null) {
			name.append(" (").append(year).append(")");
		}

		if (movie instanceof MoviePart) {
			MoviePart part = (MoviePart) movie;
			if (part.getPartCount() > 1) {
				name.append(".CD").append(part.getPartIndex());
			}
		}

		// remove path separators if the name contains any / or \
		return replacePathSeparators(name);
	}

	@Override
	public String format(Match<?, ?> match, boolean extension, Map<?, ?> context) {
		return preview(match);
	}

}
