package org.openfilebot.subtitle;

import java.util.stream.Stream;

public interface SubtitleDecoder {

	Stream<SubtitleElement> decode(String file);

}