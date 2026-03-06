package org.openfilebot.ui;

import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.*;
import static java.util.Arrays.*;
import static java.util.Comparator.*;
import static javax.swing.BorderFactory.*;
import static javax.swing.KeyStroke.*;
import static javax.swing.ScrollPaneConstants.*;
import static org.openfilebot.Logging.*;
import static org.openfilebot.Settings.*;
import static org.openfilebot.util.ui.SwingUI.*;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dialog.ModalExclusionType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.google.common.eventbus.Subscribe;

import org.openfilebot.CacheManager;
import org.openfilebot.Settings;
import org.openfilebot.ResourceManager;
import org.openfilebot.cli.GroovyPad;
import org.openfilebot.util.PreferencesMap.PreferencesEntry;
import org.openfilebot.util.ui.DefaultFancyListCellRenderer;
import org.openfilebot.util.ui.ActionPopup;
import org.openfilebot.util.ui.ShadowBorder;
import org.openfilebot.util.ui.SwingEventBus;
import net.miginfocom.swing.MigLayout;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;

public class MainFrame extends JFrame {

	private static final PreferencesEntry<String> persistentSelectedPanel = Settings.forPackage(MainFrame.class).entry("panel.selected").defaultValue("0");

	private JTabbedPane tabbedPane;

	public MainFrame(PanelBuilder[] panels) {
		super(isAutoUpdateEnabled() ? getApplicationName() : String.format("%s %s", getApplicationName(), getApplicationVersion()));

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.putClientProperty("JTabbedPane.tabType", "card"); // FlatLaf card styling
		tabbedPane.putClientProperty("JTabbedPane.showTabSeparators", true);
		tabbedPane.putClientProperty("JTabbedPane.tabAreaAlignment", "fill"); // Make trailing component align right

		// Add trailing settings button via FlatLaf trailing component feature
		JToolBar trailingBar = new JToolBar();
		trailingBar.setFloatable(false);
		trailingBar.setBorder(createEmptyBorder());
		
		trailingBar.add(Box.createHorizontalGlue()); // Push button to the far right
		
		JButton globalSettingsButton = new JButton(ResourceManager.getIcon("action.preferences"));
		globalSettingsButton.setFocusable(false);
		globalSettingsButton.setToolTipText("Global Settings & Options");
		
		// Example empty popup menu attachment placeholder
		JPopupMenu globalSettingsMenu = new JPopupMenu();
		globalSettingsMenu.add("Preferences"); 
		globalSettingsMenu.add("Language");

		globalSettingsButton.addActionListener(e -> {
			globalSettingsMenu.show(globalSettingsButton, 0, globalSettingsButton.getHeight());
		});
		
		trailingBar.add(globalSettingsButton);
		tabbedPane.putClientProperty("JTabbedPane.trailingComponent", trailingBar);

		JComponent c = (JComponent) getContentPane();
		c.setLayout(new MigLayout("insets 0, fill", "[fill]", "fill"));
		c.add(tabbedPane, "grow");

		for (PanelBuilder builder : panels) {
			JPanel dummy = new JPanel();
			dummy.putClientProperty(PanelBuilder.class.getName(), builder);
			tabbedPane.addTab(builder.getName(), builder.getIcon(), dummy);
		}

		tabbedPane.addChangeListener(evt -> {
			int idx = tabbedPane.getSelectedIndex();
			if (idx >= 0) {
				JComponent comp = (JComponent) tabbedPane.getComponentAt(idx);
				PanelBuilder builder = (PanelBuilder) comp.getClientProperty(PanelBuilder.class.getName());
				showPanel(builder);
				persistentSelectedPanel.setValue(Integer.toString(idx));
			}
		});

		// restore selected panel
		try {
			int selectedIndex = Integer.parseInt(persistentSelectedPanel.getValue());
			if (selectedIndex >= 0 && selectedIndex < tabbedPane.getTabCount()) {
				tabbedPane.setSelectedIndex(selectedIndex);
			}
		} catch (Exception e) {
			debug.log(Level.WARNING, e, e::getMessage);
		}

		// show initial panel
		if (tabbedPane.getSelectedIndex() >= 0) {
			JComponent comp = (JComponent) tabbedPane.getComponentAt(tabbedPane.getSelectedIndex());
			showPanel((PanelBuilder) comp.getClientProperty(PanelBuilder.class.getName()));
		}

		setSize(1060, 650);
		setMinimumSize(new Dimension(900, 340));

		// KEYBOARD SHORTCUTS
		installAction(getRootPane(), getKeyStroke(VK_DELETE, CTRL_DOWN_MASK | SHIFT_DOWN_MASK), newAction("Clear Cache", evt -> {
			withWaitCursor(getRootPane(), () -> {
				CacheManager.getInstance().clearAll();
				log.info("Cache has been cleared");
			});
		}));

		installAction(getRootPane(), getKeyStroke(VK_F5, 0), newAction("Run", evt -> {
			withWaitCursor(getRootPane(), () -> {
				GroovyPad pad = new GroovyPad();

				pad.addWindowListener(new WindowAdapter() {

					@Override
					public void windowOpened(WindowEvent e) {
						setVisible(false);
					};

					@Override
					public void windowClosing(WindowEvent e) {
						setVisible(true);
					};
				});

				pad.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
				pad.setModalExclusionType(ModalExclusionType.TOOLKIT_EXCLUDE);
				pad.setLocationByPlatform(true);
				pad.setVisible(true);
			});
		}));

		SwingEventBus.getInstance().register(this);
	}

	@Subscribe
	public void selectPanel(PanelBuilder panel) {
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			JComponent comp = (JComponent) tabbedPane.getComponentAt(i);
			PanelBuilder builder = (PanelBuilder) comp.getClientProperty(PanelBuilder.class.getName());
			if (builder.equals(panel)) {
				tabbedPane.setSelectedIndex(i);
				break;
			}
		}
	}

	private void showPanel(PanelBuilder selectedBuilder) {
		if (selectedBuilder == null)
			return;

		int idx = tabbedPane.getSelectedIndex();
		JComponent comp = (JComponent) tabbedPane.getComponentAt(idx);

		// lazy initialization
		if (comp.getClass() == JPanel.class) {
			JComponent realPanel = selectedBuilder.create();
			realPanel.putClientProperty(PanelBuilder.class.getName(), selectedBuilder);
			tabbedPane.setComponentAt(idx, realPanel);
			comp = realPanel;
		}

		// handle event bus registration
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			JComponent p = (JComponent) tabbedPane.getComponentAt(i);
			if (p.getClass() != JPanel.class) {
				Boolean isRegistered = (Boolean) p.getClientProperty("EventBus.registered");
				if (isRegistered == null) {
					isRegistered = Boolean.FALSE;
				}

				if (i == idx) {
					if (!isRegistered) {
						SwingEventBus.getInstance().register(p);
						p.putClientProperty("EventBus.registered", Boolean.TRUE);
					}
				} else {
					if (isRegistered) {
						SwingEventBus.getInstance().unregister(p);
						p.putClientProperty("EventBus.registered", Boolean.FALSE);
					}
				}
			}
		}
	}

}
