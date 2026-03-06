
package org.openfilebot.ui.rename;

import java.util.Map;

import org.openfilebot.similarity.Match;

public interface MatchFormatter {

	public boolean canFormat(Match<?, ?> match);

	public String preview(Match<?, ?> match);

	public String format(Match<?, ?> match, boolean extension, Map<?, ?> context) throws Exception;

}
