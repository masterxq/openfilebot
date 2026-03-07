package org.openfilebot.ui;

import static java.awt.Cursor.*;
import static org.openfilebot.util.ui.SwingUI.*;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;

import org.openfilebot.WebServices;
import org.openfilebot.ResourceManager;
import org.openfilebot.util.ui.DefaultFancyListCellRenderer;
import org.openfilebot.web.Artwork;
import org.openfilebot.web.ArtworkProvider;
import org.openfilebot.web.Datasource;
import org.openfilebot.web.EpisodeListProvider;
import org.openfilebot.web.Movie;
import org.openfilebot.web.MovieInfo;
import org.openfilebot.web.SearchResult;
import org.openfilebot.web.SeriesInfo;
import net.miginfocom.swing.MigLayout;

public class SelectDialog<T> extends JDialog {

	private static final int MAX_ALIAS_LINES = 3;

	private JLabel messageLabel = new JLabel();
	private JCheckBox autoRepeatCheckBox = new JCheckBox();
	private JLabel artworkLabel = new JLabel();
	private JEditorPane metadataView = new JEditorPane();

	private JList<T> list;
	private String command = null;
	private Datasource datasource = null;
	private SwingWorker<SearchResultMetadata, Void> metadataWorker = null;
	private long metadataRequestId = 0;
	private SearchResult pendingMetadataItem = null;
	private final Map<String, SearchResultMetadata> metadataCache = new HashMap<String, SearchResultMetadata>();
	private final Timer metadataFetchTimer;

	public SelectDialog(Component parent, Collection<? extends T> options) {
		this(parent, options, false, false, null);
	}

	public SelectDialog(Component parent, Collection<? extends T> options, boolean autoRepeatEnabled, boolean autoRepeatSelected, JComponent header) {
		super(getWindow(parent), "Select", ModalityType.DOCUMENT_MODAL);

		metadataFetchTimer = new Timer(220, evt -> startMetadataFetch());
		metadataFetchTimer.setRepeats(false);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		// initialize list
		DefaultListModel<T> model = new DefaultListModel<T>();
		for (T option : options) {
			model.addElement(option);
		}
		list = new JList<T>(model);

		// select first element
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		if (list.getModel().getSize() > 0) {
			list.setSelectedIndex(0);
		}

		DefaultFancyListCellRenderer renderer = new DefaultFancyListCellRenderer(4) {

			@Override
			@SuppressWarnings("rawtypes")
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, convertValueToString(value), index, isSelected, cellHasFocus);
				configureValue(this, value);
				return this;
			}
		};

		renderer.setHighlightingEnabled(false);

		@SuppressWarnings("unchecked")
		javax.swing.ListCellRenderer<? super T> cellRenderer = (javax.swing.ListCellRenderer<? super T>) renderer;
		list.setCellRenderer(cellRenderer);
		list.addMouseListener(mouseListener);
		list.addListSelectionListener(evt -> {
			if (!evt.getValueIsAdjusting() && list.getModel().getSize() > 0 && list.getSelectedIndex() < 0) {
				list.setSelectedIndex(0);
			}
			updateMetadataPanel();
		});

		JComponent c = (JComponent) getContentPane();
		c.setLayout(new MigLayout("insets 1.5mm 1.5mm 2.7mm 1.5mm, nogrid, novisualpadding, fill", "[grow,fill][240!,fill]", header == null ? "[pref!][fill][pref!]" : "[min!][min!][fill][pref!]"));

		if (header != null) {
			c.add(header, "spanx 2, wmin 150px, hmin pref, growx, wrap");
		}
		c.add(messageLabel, "spanx 2, wmin 150px, hmin pref, growx, wrap");
		c.add(new JScrollPane(list), "wmin 150px, hmin 150px, grow");
		c.add(createMetadataPanel(), "w 240!, hmin 150px, growy, pushy, wrap 2mm");

		c.add(new JButton(selectAction), "align center, id select");
		c.add(new JButton(cancelAction), "gap unrel, id cancel");

		// add repeat button
		if (autoRepeatEnabled) {
			autoRepeatCheckBox.addChangeListener(evt -> autoRepeatCheckBox.setToolTipText(autoRepeatCheckBox.isSelected() ? "Select and remember for next time" : "Select once and ask again next time"));
			autoRepeatCheckBox.setCursor(getPredefinedCursor(HAND_CURSOR));
			autoRepeatCheckBox.setIcon(ResourceManager.getIcon("button.repeat"));
			autoRepeatCheckBox.setSelectedIcon(ResourceManager.getIcon("button.repeat.selected"));
			autoRepeatCheckBox.setSelected(autoRepeatSelected);
			c.add(autoRepeatCheckBox, "pos 1al select.y n select.y2");
		}

		// set default size and location
		setMinimumSize(new Dimension(640, 360));

		// Shortcut Enter
		installAction(list, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), selectAction);

		updateMetadataPanel();
	}

	private JComponent createMetadataPanel() {
		JPanel panel = new JPanel(new MigLayout("insets 0, fillx, wrap 1", "[grow,fill]", "[min!][grow]"));
		panel.setMinimumSize(new Dimension(240, 150));
		panel.setPreferredSize(new Dimension(240, 320));

		artworkLabel.setHorizontalAlignment(JLabel.CENTER);
		artworkLabel.setVerticalAlignment(JLabel.TOP);
		artworkLabel.setPreferredSize(new Dimension(200, 200));
		artworkLabel.setText("No image");

		metadataView.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
		metadataView.setContentType("text/html");
		metadataView.setEditable(false);
		metadataView.setOpaque(false);
		metadataView.setMargin(new Insets(0, 0, 0, 0));
		metadataView.setBorder(null);
		metadataView.setFont(UIManager.getFont("Label.font"));
		metadataView.setCursor(getPredefinedCursor(DEFAULT_CURSOR));
		metadataView.setCaretColor(metadataView.getBackground());
		metadataView.getCaret().setBlinkRate(0);
		metadataView.getCaret().setVisible(false);
		metadataView.addHyperlinkListener(evt -> {
			if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				URL url = evt.getURL();
				if (url != null) {
					openURI(url.toString());
				} else if (evt.getDescription() != null) {
					openURI(evt.getDescription());
				}
			}
		});

		JScrollPane metadataScrollPane = new JScrollPane(metadataView, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		metadataScrollPane.setBorder(null);
		metadataScrollPane.setOpaque(false);
		metadataScrollPane.getViewport().setOpaque(false);

		panel.add(artworkLabel, "hmin 180px, growx");
		panel.add(metadataScrollPane, "grow, pushy");

		return panel;
	}

	protected String convertValueToString(Object value) {
		return value.toString();
	}

	public void setDatasource(Datasource datasource) {
		this.datasource = datasource;
		updateMetadataPanel();
	}

	protected void configureValue(DefaultFancyListCellRenderer render, Object value) {
		if (value instanceof SearchResult) {
			render.setToolTipText(getTooltipText((SearchResult) value));
		} else if (value instanceof File) {
			render.setToolTipText(((File) value).getAbsolutePath());
		} else {
			render.setToolTipText(null);
		}
	}

	protected String getTooltipText(SearchResult item) {
		return buildMetadataHtml(item, null, null);
	}

	protected String buildMetadataHtml(SearchResult item, Integer year, URI link) {
		StringBuilder html = new StringBuilder(256);
		html.append("<html><body style='margin:0; padding:0;'>");
		html.append("<b>").append(escapeHTML(item.toString())).append("</b><br>");

		if (year != null && year > 0) {
			html.append("Year: ").append(Integer.toString(year)).append("<br>");
		}

		html.append("ID: ").append(Integer.toString(item.getId()));
		if (link != null) {
			html.append(" <a href='").append(escapeHTML(link.toString())).append("'>(in ").append(escapeHTML(getProviderDisplayName(item))).append(")</a>");
		}

		String[] names = item.getAliasNames();
		if (names.length > 0) {
			int displayed = 0;
			int total = 0;

			html.append("<br><br>AKA:");
			for (String n : names) {
				if (n == null || n.isEmpty()) {
					continue;
				}

				total++;
				if (displayed < MAX_ALIAS_LINES) {
					html.append("<br>• ").append(escapeHTML(n));
					displayed++;
				}
			}

			if (total > displayed) {
				html.append("<br>• … +").append(Integer.toString(total - displayed)).append(" more");
			}
		}

		html.append("</body></html>");
		return html.toString();
	}

	private String getProviderDisplayName(SearchResult item) {
		if (item instanceof Movie) {
			Movie movie = (Movie) item;
			if (movie.getTmdbId() > 0) {
				return "TheMovieDB";
			}
			if (movie.getImdbId() > 0) {
				return "IMDb";
			}
		}

		if (datasource != null && datasource.getName() != null && !datasource.getName().isEmpty()) {
			return datasource.getName();
		}

		return "Database";
	}

	private void updateMetadataPanel() {
		T value = list.getSelectedValue();

		if (!(value instanceof SearchResult)) {
			if (value instanceof File) {
				metadataView.setText("<html><body style='margin:0; padding:0;'><b>Path</b><br>" + escapeHTML(((File) value).getAbsolutePath()) + "</body></html>");
			} else {
				metadataView.setText(value == null ? "" : "<html><body style='margin:0; padding:0;'><b>" + escapeHTML(value.toString()) + "</b></body></html>");
			}
			metadataView.setCaretPosition(0);
			artworkLabel.setIcon(null);
			artworkLabel.setText("No image");
			return;
		}

		SearchResult searchResult = (SearchResult) value;
		URI entryLink = getEntryLink(searchResult);
		metadataView.setText(buildMetadataHtml(searchResult, getImmediateYear(searchResult), entryLink));
		metadataView.setCaretPosition(0);
		artworkLabel.setIcon(null);
		artworkLabel.setText("Loading…");

		String key = getMetadataCacheKey(searchResult);
		SearchResultMetadata cached = metadataCache.get(key);
		if (cached != null) {
			Integer year = cached.year != null ? cached.year : getImmediateYear(searchResult);
			metadataView.setText(buildMetadataHtml(searchResult, year, entryLink));
			metadataView.setCaretPosition(0);
			if (cached.poster != null) {
				artworkLabel.setIcon(cached.poster);
				artworkLabel.setText(null);
			} else {
				artworkLabel.setIcon(null);
				artworkLabel.setText("No image");
			}
			return;
		}

		pendingMetadataItem = searchResult;
		metadataFetchTimer.restart();
	}

	private void startMetadataFetch() {
		SearchResult searchResult = pendingMetadataItem;
		if (searchResult == null) {
			return;
		}
		URI entryLink = getEntryLink(searchResult);

		if (metadataWorker != null) {
			metadataWorker.cancel(false);
		}

		long requestId = ++metadataRequestId;
		metadataWorker = newSwingWorker(() -> fetchMetadata(searchResult), metadata -> {
			if (requestId != metadataRequestId) {
				return;
			}

			metadataCache.put(getMetadataCacheKey(searchResult), metadata);

			Integer year = metadata.year != null ? metadata.year : getImmediateYear(searchResult);
			metadataView.setText(buildMetadataHtml(searchResult, year, entryLink));
			metadataView.setCaretPosition(0);
			if (metadata.poster != null) {
				artworkLabel.setIcon(metadata.poster);
				artworkLabel.setText(null);
			} else {
				artworkLabel.setIcon(null);
				artworkLabel.setText("No image");
			}

		}, error -> {
			if (requestId != metadataRequestId) {
				return;
			}

			metadataView.setText(buildMetadataHtml(searchResult, getImmediateYear(searchResult), entryLink));
			metadataView.setCaretPosition(0);
			artworkLabel.setIcon(null);
			artworkLabel.setText("No image");
		});
		metadataWorker.execute();
	}

	private Integer getImmediateYear(SearchResult item) {
		if (item instanceof Movie) {
			Integer year = ((Movie) item).getYear();
			if (year > 0) {
				return year;
			}
		}

		Integer year = item.getYear();
		return year != null && year > 0 ? year : null;
	}

	private String getMetadataCacheKey(SearchResult item) {
		String source = datasource != null ? datasource.getIdentifier() : "";
		return source + "::" + item.getClass().getName() + "::" + item.getId();
	}

	private SearchResultMetadata fetchMetadata(SearchResult item) throws Exception {
		Integer year = getImmediateYear(item);
		ImageIcon poster = null;

		if (item instanceof Movie) {
			Movie movie = (Movie) item;
			Integer movieYear = movie.getYear();
			year = movieYear != null && movieYear > 0 ? movieYear : null;

			if (movie.getTmdbId() > 0) {
				MovieInfo info = WebServices.TheMovieDB.getMovieInfo(movie, Locale.ENGLISH, true);
				poster = createPosterIcon(info == null ? null : info.getPoster());
			} else if (movie.getImdbId() > 0) {
				MovieInfo info = WebServices.OMDb.getMovieInfo(movie);
				poster = createPosterIcon(info == null ? null : info.getPoster());
			}

			return new SearchResultMetadata(year, poster);
		}

		if (datasource instanceof EpisodeListProvider) {
			EpisodeListProvider provider = (EpisodeListProvider) datasource;
			if (year == null) {
				SeriesInfo info = provider.getSeriesInfo(item, Locale.ENGLISH);
				year = info != null && info.getStartDate() != null ? info.getStartDate().getYear() : null;
			}

			if (provider instanceof ArtworkProvider) {
				List<Artwork> artwork = ((ArtworkProvider) provider).getArtwork(item.getId(), "poster", Locale.ENGLISH);
				if (artwork.isEmpty()) {
					artwork = ((ArtworkProvider) provider).getArtwork(item.getId(), "posters", Locale.ENGLISH);
				}
				poster = createPosterIcon(artwork.isEmpty() ? null : artwork.get(0).getUrl());
			}
		}

		return new SearchResultMetadata(year, poster);
	}

	private java.net.URI getEntryLink(SearchResult item) {
		if (item instanceof Movie) {
			Movie movie = (Movie) item;
			if (movie.getTmdbId() > 0) {
				return java.net.URI.create("https://www.themoviedb.org/movie/" + movie.getTmdbId());
			}
			if (movie.getImdbId() > 0) {
				return java.net.URI.create(String.format("https://www.imdb.com/title/tt%07d", movie.getImdbId()));
			}
		}

		if (datasource instanceof EpisodeListProvider) {
			return ((EpisodeListProvider) datasource).getEpisodeListLink(item);
		}

		String source = datasource != null ? datasource.getIdentifier() : getTitle();
		if (source == null) {
			return null;
		}

		source = source.toLowerCase(Locale.ROOT);
		if (source.contains("thetvdb")) {
			return java.net.URI.create("https://www.thetvdb.com/?tab=seasonall&id=" + item.getId());
		}
		if (source.contains("themoviedb") && source.contains("tv")) {
			return java.net.URI.create("https://www.themoviedb.org/tv/" + item.getId());
		}
		if (source.contains("tvmaze")) {
			return java.net.URI.create("http://www.tvmaze.com/shows/" + item.getId());
		}
		if (source.contains("anidb")) {
			return java.net.URI.create("http://anidb.net/a" + item.getId());
		}

		return null;
	}

	private ImageIcon createPosterIcon(URL url) {
		if (url == null) {
			return null;
		}

		try {
			BufferedImage image = ImageIO.read(getPreviewImageURL(url));
			if (image == null) {
				return null;
			}

			Dimension box = artworkLabel.getSize();
			int maxWidth = box.width > 0 ? box.width : 220;
			int maxHeight = box.height > 0 ? box.height : 320;

			double sx = (double) maxWidth / image.getWidth();
			double sy = (double) maxHeight / image.getHeight();
			double scale = Math.min(1.0, Math.min(sx, sy));

			int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
			int height = Math.max(1, (int) Math.round(image.getHeight() * scale));

			Image scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
			return new ImageIcon(scaled);
		} catch (Exception e) {
			return null;
		}
	}

	private URL getPreviewImageURL(URL url) {
		try {
			String value = url.toString();

			if (value.contains("image.tmdb.org/t/p/original/")) {
				return new URL(value.replace("/original/", "/w342/"));
			}
		} catch (Exception e) {
			// ignore and use original URL
		}

		return url;
	}

	public JLabel getMessageLabel() {
		return messageLabel;
	}

	public JCheckBox getAutoRepeatCheckBox() {
		return autoRepeatCheckBox;
	}

	public String getSelectedAction() {
		return command;
	}

	public T getSelectedValue() {
		return SELECT.equals(command) ? list.getSelectedValue() : null;
	}

	public void close() {
		metadataFetchTimer.stop();
		if (metadataWorker != null) {
			metadataWorker.cancel(false);
		}
		setVisible(false);
		dispose();
	}

	public Action getSelectAction() {
		return selectAction;
	}

	public Action getCancelAction() {
		return cancelAction;
	}

	public static final String SELECT = "Select";
	public static final String CANCEL = "Cancel";

	private final Action selectAction = newAction(SELECT, ResourceManager.getIcon("dialog.continue"), evt -> {
		command = SELECT;
		close();
	});

	private final Action cancelAction = newAction(CANCEL, ResourceManager.getIcon("dialog.cancel"), evt -> {
		command = CANCEL;
		close();
	});

	private final MouseAdapter mouseListener = mouseClicked(evt -> {
		if (SwingUtilities.isLeftMouseButton(evt) && (evt.getClickCount() == 2)) {
			selectAction.actionPerformed(new ActionEvent(evt.getSource(), ActionEvent.ACTION_PERFORMED, SELECT));
		}
	});

	private static final String KEY_REPEAT = "dialog.select.repeat";
	private static final String KEY_WIDTH = "dialog.select.width";
	private static final String KEY_HEIGHT = "dialog.select.height";

	public void saveState(Preferences prefs) {
		prefs.putBoolean(KEY_REPEAT, autoRepeatCheckBox.isSelected());
		prefs.putInt(KEY_WIDTH, getWidth());
		prefs.putInt(KEY_HEIGHT, getHeight());
	}

	public void restoreState(Preferences prefs) {
		autoRepeatCheckBox.setSelected(prefs.getBoolean(KEY_REPEAT, autoRepeatCheckBox.isSelected()));
		setSize(prefs.getInt(KEY_WIDTH, getWidth()), prefs.getInt(KEY_HEIGHT, getHeight()));
	}

	private static class SearchResultMetadata {

		public final Integer year;
		public final ImageIcon poster;

		public SearchResultMetadata(Integer year, ImageIcon poster) {
			this.year = year;
			this.poster = poster;
		}
	}

}
