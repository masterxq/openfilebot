package org.openfilebot.cli;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.openfilebot.Language;
import org.openfilebot.RenameAction;
import org.openfilebot.format.ExpressionFileFormat;
import org.openfilebot.format.ExpressionFilter;
import org.openfilebot.format.ExpressionFormat;
import org.openfilebot.hash.HashType;
import org.openfilebot.subtitle.SubtitleFormat;
import org.openfilebot.subtitle.SubtitleNaming;
import org.openfilebot.web.Datasource;
import org.openfilebot.web.EpisodeListProvider;
import org.openfilebot.web.SortOrder;

public interface CmdlineInterface {

	List<File> rename(Collection<File> files, RenameAction action, ConflictAction conflict, File output, ExpressionFileFormat format, Datasource db, String query, SortOrder order, ExpressionFilter filter, Locale locale, boolean strict, ExecCommand exec) throws Exception;

	List<File> rename(EpisodeListProvider db, String query, ExpressionFileFormat format, ExpressionFilter filter, SortOrder order, Locale locale, boolean strict, List<File> files, RenameAction action, ConflictAction conflict, File output, ExecCommand exec) throws Exception;

	List<File> rename(Map<File, File> rename, RenameAction action, ConflictAction conflict) throws Exception;

	List<File> revert(Collection<File> files, FileFilter filter, RenameAction action) throws Exception;

	List<File> getSubtitles(Collection<File> files, String query, Language language, SubtitleFormat output, Charset encoding, SubtitleNaming format, boolean strict) throws Exception;

	List<File> getMissingSubtitles(Collection<File> files, String query, Language language, SubtitleFormat output, Charset encoding, SubtitleNaming format, boolean strict) throws Exception;

	boolean check(Collection<File> files) throws Exception;

	File compute(Collection<File> files, File output, HashType hash, Charset encoding) throws Exception;

	Stream<String> fetchEpisodeList(EpisodeListProvider db, String query, ExpressionFormat format, ExpressionFilter filter, SortOrder order, Locale locale, boolean strict) throws Exception;

	Stream<String> getMediaInfo(Collection<File> files, FileFilter filter, ExpressionFormat format) throws Exception;

	boolean execute(Collection<File> files, FileFilter filter, ExecCommand exec) throws Exception;

	List<File> extract(Collection<File> files, File output, ConflictAction conflict, FileFilter filter, boolean forceExtractAll) throws Exception;

}
