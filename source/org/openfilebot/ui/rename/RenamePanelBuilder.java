
package org.openfilebot.ui.rename;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.openfilebot.ResourceManager;
import org.openfilebot.ui.PanelBuilder;

public class RenamePanelBuilder implements PanelBuilder {

	@Override
	public String getName() {
		return "Rename";
	}

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("panel.rename");
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof RenamePanelBuilder;
	}

	@Override
	public JComponent create() {
		return new RenamePanel();
	}

}
