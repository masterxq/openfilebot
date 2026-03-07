package org.openfilebot.ui.subtitle;

import static javax.swing.BorderFactory.*;

import java.awt.Color;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

public class SimpleComboBox extends JComboBox {

	public SimpleComboBox(Icon dropDownArrowIcon) {
		setUI(new SimpleComboBoxUI(dropDownArrowIcon));
		setBorder(createEmptyBorder());
	}

	private static class SimpleComboBoxUI extends BasicComboBoxUI {

		private final Icon dropDownArrowIcon;

		public SimpleComboBoxUI(Icon dropDownArrowIcon) {
			this.dropDownArrowIcon = dropDownArrowIcon;
		}

		@Override
		protected JButton createArrowButton() {
			JButton button = new JButton(dropDownArrowIcon);
			button.setContentAreaFilled(false);
			button.setBorderPainted(false);
			button.setFocusPainted(false);
			button.setOpaque(false);

			return button;
		}

		@Override
		protected ComboPopup createPopup() {
			return new BasicComboPopup(comboBox) {

				@Override
				protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
					Rectangle bounds = super.computePopupBounds(px, py, pw, ph);

					// allow combobox popup to be wider than the combobox itself
					bounds.width = Math.max(bounds.width, list.getPreferredSize().width);

					return bounds;
				}

				@Override
				protected void configurePopup() {
					super.configurePopup();

					Color popupBackground = UIManager.getColor("ComboBox.background");
					if (popupBackground == null) {
						popupBackground = Color.white;
					}

					setOpaque(true);
					list.setBackground(popupBackground);
					setBackground(popupBackground);

					// use gray instead of black border for combobox popup
					Color borderColor = UIManager.getColor("Component.borderColor");
					if (borderColor == null) {
						borderColor = Color.gray;
					}
					setBorder(createCompoundBorder(createLineBorder(borderColor, 1), createEmptyBorder(1, 1, 1, 1)));
				}
			};
		}
	}

}
