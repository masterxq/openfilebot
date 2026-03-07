package org.openfilebot.ui;

import static javax.swing.BorderFactory.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.openfilebot.util.ui.GradientStyle;
import org.openfilebot.util.ui.notification.SeparatorBorder;
import org.openfilebot.util.ui.notification.SeparatorBorder.Position;

public class HeaderPanel extends JComponent {

	private JLabel titleLabel = new JLabel();

	private float[] gradientFractions = { 0.0f, 0.5f, 1.0f };
	private Color[] gradientColors = { Color.LIGHT_GRAY, Color.LIGHT_GRAY, Color.LIGHT_GRAY };

	public HeaderPanel() {
		setLayout(new BorderLayout());
		updateThemeColors();

		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setOpaque(false);

		titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
		titleLabel.setVerticalAlignment(SwingConstants.CENTER);
		titleLabel.setOpaque(false);
		titleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));

		centerPanel.setBorder(createEmptyBorder());
		centerPanel.add(titleLabel, BorderLayout.CENTER);

		add(centerPanel, BorderLayout.CENTER);

		Color separator = UIManager.getColor("Separator.foreground") != null ? UIManager.getColor("Separator.foreground") : new Color(0xACACAC);
		setBorder(new SeparatorBorder(1, separator, separator, GradientStyle.LEFT_TO_RIGHT, Position.BOTTOM));
	}

	private void updateThemeColors() {
		Color panelBackground = UIManager.getColor("Panel.background") != null ? UIManager.getColor("Panel.background") : Color.WHITE;
		Color labelForeground = UIManager.getColor("Label.foreground") != null ? UIManager.getColor("Label.foreground") : new Color(0x101010);

		setBackground(panelBackground);
		titleLabel.setForeground(labelForeground);
		gradientColors = new Color[] { panelBackground, panelBackground, panelBackground };
	}

	@Override
	public void updateUI() {
		super.updateUI();
		if (titleLabel != null) {
			updateThemeColors();
		}
	}

	public void setTitle(String title) {
		titleLabel.setText(title);
	}

	public JLabel getTitleLabel() {
		return titleLabel;
	}

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;

		LinearGradientPaint paint = new LinearGradientPaint(0, 0, getWidth(), 0, gradientFractions, gradientColors);

		g2d.setPaint(paint);
		g2d.fill(getBounds());
	}

}
