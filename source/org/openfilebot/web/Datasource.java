package org.openfilebot.web;

import javax.swing.Icon;

public interface Datasource {

	String getIdentifier();

	Icon getIcon();

	default String getName() {
		return getIdentifier();
	}

}
