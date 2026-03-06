package net.filebot.cli;

import static org.junit.Assert.*;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.script.SimpleBindings;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class ScriptShellAutomationTest {

	@Test
	public void autosortScriptMovesFilesToExpectedFolders() throws Throwable {
		File workspace = new File(FileUtils.getTempDirectory(), getClass().getName() + "-" + System.nanoTime());
		File input = new File(workspace, "downloads");
		File output = new File(workspace, "media");

		assertTrue(input.mkdirs());
		assertTrue(output.mkdirs());

		File episode = new File(input, "Demo.Show.S01E01.1080p.mkv");
		File movie = new File(input, "Dune.Part.Two.2024.1080p.mkv");
		File clip = new File(input, "My.Holiday.Clip.mp4");
		File ignored = new File(input, "README.txt");

		assertTrue(episode.createNewFile());
		assertTrue(movie.createNewFile());
		assertTrue(clip.createNewFile());
		assertTrue(ignored.createNewFile());

		String script = String.join("\n", // language=Groovy
				"def inDir = new File(input.toString())",
				"def outDir = new File(output.toString())",
				"inDir.getFiles{ it.isVideo() }.each { f ->",
				"    def sxe = parseEpisodeNumber(f.name)",
				"    def target = sxe ? new File(outDir, \"TV/${f.name}\") : new File(outDir, \"Movies/${f.name}\")",
				"    target.parentFile.mkdirs()",
				"    f.moveTo(target)",
				"}");

		Map<String, Object> globals = new LinkedHashMap<String, Object>();
		globals.put("input", input.getAbsolutePath());
		globals.put("output", output.getAbsolutePath());

		ScriptShell shell = new ScriptShell(name -> script, null, globals);
		shell.runScript("autosort", new SimpleBindings());

		assertFalse(episode.exists());
		assertFalse(movie.exists());
		assertFalse(clip.exists());

		assertTrue(new File(output, "TV/" + episode.getName()).isFile());
		assertTrue(new File(output, "Movies/" + movie.getName()).isFile());
		assertTrue(new File(output, "Movies/" + clip.getName()).isFile());

		assertTrue(ignored.isFile());
		assertFalse(new File(output, "Movies/" + ignored.getName()).exists());

		FileUtils.deleteDirectory(workspace);
	}

}