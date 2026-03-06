
package org.openfilebot.ui.sfv;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.openfilebot.ResourceManager;
import org.openfilebot.ui.PanelBuilder;

public class SfvPanelBuilder implements PanelBuilder {

	@Override
	public String getName() {
		return "SFV";
	}

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("panel.sfv");
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof SfvPanelBuilder;
	}

	@Override
	public JComponent create() {
		return new SfvPanel();
	}

}
