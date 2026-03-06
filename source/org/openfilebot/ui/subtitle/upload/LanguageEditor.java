package org.openfilebot.ui.subtitle.upload;

import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;

import org.openfilebot.Language;
import org.openfilebot.ui.LanguageComboBox;

class LanguageEditor extends DefaultCellEditor {

	public LanguageEditor() {
		super(createLanguageComboBox());
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		LanguageComboBox editor = (LanguageComboBox) super.getTableCellEditorComponent(table, value, isSelected, row, column);
		editor.getModel().setSelectedItem(value);
		return editor;
	}

	public static LanguageComboBox createLanguageComboBox() {
		LanguageComboBox languageEditor = new LanguageComboBox(Language.getLanguage("en"), null);
		languageEditor.setFocusable(false);
		return languageEditor;
	}

}
