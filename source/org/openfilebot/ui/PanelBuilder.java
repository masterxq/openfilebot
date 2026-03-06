
package org.openfilebot.ui;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.openfilebot.ui.episodelist.EpisodeListPanelBuilder;
import org.openfilebot.ui.filter.FilterPanelBuilder;
import org.openfilebot.ui.list.ListPanelBuilder;
import org.openfilebot.ui.rename.RenamePanelBuilder;
import org.openfilebot.ui.sfv.SfvPanelBuilder;
import org.openfilebot.ui.subtitle.SubtitlePanelBuilder;

public interface PanelBuilder {

	public String getName();

	public Icon getIcon();

	public JComponent create();

	public static PanelBuilder[] defaultSequence() {
		return new PanelBuilder[] { new RenamePanelBuilder(), new EpisodeListPanelBuilder(), new SubtitlePanelBuilder(), new SfvPanelBuilder(), new FilterPanelBuilder(), new ListPanelBuilder() };
	}

	public static PanelBuilder[] episodeHandlerSequence() {
		return new PanelBuilder[] { new RenamePanelBuilder(), new ListPanelBuilder() };
	}

	public static PanelBuilder[] fileHandlerSequence() {
		return new PanelBuilder[] { new RenamePanelBuilder(), new SfvPanelBuilder(), new ListPanelBuilder() };
	}

	public static PanelBuilder[] textHandlerSequence() {
		return new PanelBuilder[] { new RenamePanelBuilder() };
	}

}
