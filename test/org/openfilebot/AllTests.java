package org.openfilebot;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import org.openfilebot.cli.ScriptShellAutomationTest;
import org.openfilebot.format.ExpressionFormatTest;
import org.openfilebot.hash.VerificationFormatTest;
import org.openfilebot.media.MediaDetectionTest;
import org.openfilebot.media.ReleaseInfoTest;
import org.openfilebot.media.VideoFormatTest;
import org.openfilebot.mediainfo.MediaInfoTest;
import org.openfilebot.similarity.EpisodeMetricsTest;
import org.openfilebot.similarity.SimilarityTestSuite;
import org.openfilebot.subtitle.SubtitleReaderTestSuite;
import org.openfilebot.ui.SupportDialogTest;
import org.openfilebot.ui.rename.MatchModelTest;
import org.openfilebot.util.UtilTestSuite;
import org.openfilebot.web.WebTestSuite;

@RunWith(Suite.class)
@SuiteClasses({ ScriptShellAutomationTest.class, ExpressionFormatTest.class, VerificationFormatTest.class, MatchModelTest.class, SupportDialogTest.class, EpisodeMetricsTest.class, ReleaseInfoTest.class, VideoFormatTest.class, MediaDetectionTest.class, MediaInfoTest.class, SimilarityTestSuite.class, WebTestSuite.class, SubtitleReaderTestSuite.class, UtilTestSuite.class })
public class AllTests {

}
