package org.mtgpeasant.perfectdeck;

import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import lombok.Value;
import org.mtgpeasant.decks.burn.BurnDeckPilot;
import org.mtgpeasant.decks.infect.InfectDeckPilot;
import org.mtgpeasant.decks.reanimator.ReanimatorDeckPilot;
import org.mtgpeasant.decks.stompy.StompyDeckPilot;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.common.cards.Deck;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PerfectDeck extends JFrame {
    public static final double AVG_TRESH = .1d;
    public static final double PERCENT_TRESH = 2d;
    private final JComboBox pilotSelect;
    private final JTabbedPane deckTabs;
    private GoldfishSimulator.DeckStats reference;

    private static final Color BLUE = new Color(0x0074D9);
    private static final Color RED = new Color(0xFF4136);
    private static final Color GRAY = new Color(0xAAAAAA);
    private static final Color NAVY = new Color(0x001f3f);
    private static final Color TEAL = new Color(0x39CCCC);
    private static final Color OLIVE = new Color(0x3D9970);
    private static final Color GREEN = new Color(0x2ECC40);
    private static final Color LIME = new Color(0x01FF70);
    private static final Color YELLOW = new Color(0xFFDC00);
    private static final Color ORANGE = new Color(0xFF851B);
    private static final Color SILVER = new Color(0xDDDDDD);
    private static final Color MAROON = new Color(0x85144b);
    private static final Color FUCHSIA = new Color(0xF012BE);
    private static final Color PURPLE = new Color(0xB10DC9);
    private static final Color AQUA = new Color(0x7FDBFF);

    private static final Color USB = new Color(0xFF8888);

    private static final BiFunction<Integer, Integer, Boolean> EQUALS = (value, max) -> value == max;
    private static final BiFunction<Integer, Integer, Boolean> LOWEREQ = (value, max) -> value <= max;
    private BiFunction<Integer, Integer, Boolean> statsIntFilter = EQUALS;

    private static final AttributeSet COMMENT_STYLE;
    private static final AttributeSet SIDEBOARD_STYLE;
    private static final AttributeSet CARD_STYLE;
    private static final AttributeSet UNKNOWN_CARD_STYLE;
    private static final AttributeSet UNKNOWN_SIDEBOARD_STYLE;

    // stateful fields
    private int currentTabIndex = 0;
    private File currentDir = new File(".");
    private Cards managedCards = null;

    static {
        // create styles for editor
        StyleContext sc = StyleContext.getDefaultStyleContext();
        COMMENT_STYLE = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, OLIVE);
        SIDEBOARD_STYLE = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.GRAY);
        CARD_STYLE = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.BLACK);
        UNKNOWN_CARD_STYLE = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, RED);
        UNKNOWN_SIDEBOARD_STYLE = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, USB);
    }

    @Value
    static class PilotItem {
        final String text;
        final Class<? extends DeckPilot> pilotClass;

        @Override
        public String toString() {
            return text;
        }
    }

    public static void main(String[] args) {
        PerfectDeck window = new PerfectDeck();
        window.setMinimumSize(new Dimension(500, 480));
        // TODO: save settings
        window.setSize(new Dimension(800, 800));
        window.setLocation(100, 100);
//        window.setExtendedState(Frame.MAXIMIZED_BOTH);
        window.setVisible(true);
    }

    public PerfectDeck() throws HeadlessException {
        setTitle("Perfect Deck");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // north panel: deck pilot combo selector
        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel("Deck Pilot: ");
        JLabel pilotDescription = new JLabel("XX cards");
        pilotDescription.setVisible(false);
        pilotDescription.setToolTipText("Click to see managed cards");
        pilotDescription.setForeground(TEAL);
        pilotDescription.setFont(pilotDescription.getFont().deriveFont(12));
        pilotDescription.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (managedCards != null) {
                    JOptionPane.showMessageDialog(PerfectDeck.this, Joiner.on('\n').join(managedCards), "Managed Cards", JOptionPane.PLAIN_MESSAGE);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                pilotDescription.setForeground(OLIVE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                pilotDescription.setForeground(TEAL);
            }
        });

        pilotSelect = new JComboBox();
        pilotSelect.addItem(new PilotItem("Choose...", null));
        // TODO: auto discover all deck pilot classes
        pilotSelect.addItem(new PilotItem("Burn", BurnDeckPilot.class));
        pilotSelect.addItem(new PilotItem("Reanimator", ReanimatorDeckPilot.class));
        pilotSelect.addItem(new PilotItem("Stompy", StompyDeckPilot.class));
        pilotSelect.addItem(new PilotItem("Infect", InfectDeckPilot.class));
        pilotSelect.addActionListener(e -> onPilotSelected((PilotItem) pilotSelect.getSelectedItem(), pilotDescription));

        northPanel.add(label);
        northPanel.add(pilotSelect);
        northPanel.add(Box.createHorizontalStrut(10));
        northPanel.add(pilotDescription);
        northPanel.add(Box.createHorizontalStrut(20));
        JCheckBox cumulatedStats = new JCheckBox("cumulated stats", false);
        northPanel.add(cumulatedStats);
        cumulatedStats.setToolTipText("Toggles cumulated stats");
        cumulatedStats.addActionListener(e -> {
            statsIntFilter = cumulatedStats.isSelected() ? LOWEREQ : EQUALS;
            repaint();
        });

        // center: deck tabbed pane
        deckTabs = new JTabbedPane();
        deckTabs.addTab("Main", createDeckTab(true, "// type in your deck here...\n"));
        // TODO: add a + icon

        //Adding Components to the frame.
        getContentPane().add(BorderLayout.NORTH, northPanel);
        getContentPane().add(BorderLayout.CENTER, deckTabs);
    }

    private void onPilotSelected(PilotItem item, JLabel pilotDescription) {
        // reset known cards
        managedCards = null;
        pilotDescription.setVisible(false);
        if (item.pilotClass != null) {
            managedCards = DeckPilot.loadManagedCards(item.pilotClass);
            if (managedCards != null) {
                pilotDescription.setVisible(true);
                pilotDescription.setText(managedCards.size() + " cards");
            }
        }
        // TODO: parse and colorize every open deck
    }

    private Component createDeckTab(boolean main, String deck) {
        JLabel deckSummary = new JLabel("");
        deckSummary.setForeground(Color.GRAY);

        // deck (text area)
        // TODO: colouring, auto-suggest; supported cards; number of cards (main/side)
        JTextPane deckEditor = new JTextPane();
        deckEditor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> parseAndColorizeDeckEditor(deckEditor, deckSummary));
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> parseAndColorizeDeckEditor(deckEditor, deckSummary));
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
//                SwingUtilities.invokeLater(() -> parseAndColorizeDeckEditor(deckEditor));
            }
        });
        deckEditor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                parseAndColorizeDeckEditor(deckEditor, deckSummary);
            }
        });
        deckEditor.setMinimumSize(new Dimension(400, 300));
        deckEditor.setText(deck);
//        deckEditor.setCharacterAttributes();
        deckEditor.setFont(new Font("monospaced", Font.BOLD, 14));
        deckEditor.setAutoscrolls(true);

        // deck toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setMargin(new Insets(5, 5, 5, 5));

        JButton load = new JButton();
        load.addActionListener(e -> onLoadDeck(deckEditor));
//        open.setIcon(new ImageIcon("Open24"));
        load.setText("load");
        toolBar.add(load);

        JButton save = new JButton();
        save.addActionListener(e -> onSaveDeck(deckEditor));
//        save.setIcon(new ImageIcon("Save24"));
        save.setText("save");
        toolBar.add(save);

        toolBar.add(new JToolBar.Separator());
        JButton duplicate = new JButton();
        duplicate.addActionListener(e -> {
            JTabbedPane tabbedPane = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, (Component) e.getSource());
//            JTabbedPane tabbedPane = (JTabbedPane) pane.getParent();
            String title = "Alt. " + currentTabIndex++;
            Component newTab = createDeckTab(false, deckEditor.getText());
            tabbedPane.addTab(title, newTab);

            JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            tabPanel.setOpaque(false);
            tabPanel.add(new JLabel(title));
            tabPanel.add(Box.createHorizontalStrut(5));
            JLabel closeBtn = new JLabel("❎"); // ❎
            closeBtn.setForeground(Color.BLACK);
//            closeBtn.setBackground(Color.GRAY);
//            closeBtn.setOpaque(true);
//            closeBtn.setFont(new Font("monospaced", Font.BOLD, 16));
            closeBtn.setToolTipText("Close this tab");
            tabPanel.add(closeBtn);
            closeBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    newTab.getParent().remove(newTab);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    closeBtn.setForeground(RED);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    closeBtn.setForeground(Color.BLACK);
                }
            });
            tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, tabPanel);
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        });
//        duplicate.setIcon(new ImageIcon("New24"));
        duplicate.setText("copy");
        duplicate.setToolTipText("Copies this deck into a new tab");
        toolBar.add(duplicate);

        // actions panel
        JPanel deckActionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        deckActionsPanel.setBackground(Color.LIGHT_GRAY);
//        JToolBar deckActionsPanel = new JToolBar();
//        deckActionsPanel.setMargin(new Insets(5, 5, 5, 5));
//        deckActionsPanel.setFloatable(false);

        JLabel observe = new JLabel("observe");
        observe.setForeground(BLUE);
        deckActionsPanel.add(observe);

        // TODO: settings
        JTextField observeInput = createIntegerInput("2", 2);
        deckActionsPanel.add(observeInput);
        JLabel games = new JLabel("games");
        games.setForeground(BLUE);
        deckActionsPanel.add(games);
//        deckActionsPanel.set
        JButton oneGameBut = new JButton("Go!");
        oneGameBut.setToolTipText("Simulate some games (with logs)");
        // TODO: start
        oneGameBut.addActionListener(e -> onObserveGames(deckEditor.getText(), Integer.parseInt(observeInput.getText()), GoldfishSimulator.Start.BOTH));
        oneGameBut.setBackground(BLUE);
        oneGameBut.setForeground(Color.WHITE);
        deckActionsPanel.add(oneGameBut);


        // TODO: OTD/OTP/both
        deckActionsPanel.add(Box.createHorizontalStrut(80));


        JLabel stats_on = new JLabel("stats on");
        stats_on.setForeground(RED);
        deckActionsPanel.add(stats_on);

        // TODO: settings
        JTextField iterationsInput = createIntegerInput("50000", 6);
        deckActionsPanel.add(iterationsInput);

        JLabel games2 = new JLabel("games");
        games2.setForeground(RED);
        deckActionsPanel.add(games2);

        JTable statsTable = new JTable();
        statsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        statsTable.setDefaultRenderer(Object.class, new StatsTableRenderer());
        statsTable.setVisible(false);

        JButton markAsRef = new JButton("mark as ref.");
        markAsRef.setToolTipText("Mark those stats as reference (all other stats will be compared to this)");
        markAsRef.setVisible(false);

        JPanel statsActionsPanel = new JPanel();
        statsActionsPanel.add(markAsRef);


        JButton goldfishBut = new JButton("Go !");
        goldfishBut.setToolTipText("Starts a goldfish simulation and computes statistics");
        // TODO: start
        goldfishBut.addActionListener(e -> onComputeStats(deckEditor.getText(), Integer.parseInt(iterationsInput.getText()), GoldfishSimulator.Start.BOTH, statsTable, markAsRef));
        goldfishBut.setBackground(RED);
        goldfishBut.setForeground(Color.WHITE);
        deckActionsPanel.add(goldfishBut);

        JPanel deckSummaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        deckSummaryPanel.add(deckSummary);

        // north panel contains toolbar (north) / deck area (center)
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BorderLayout());
        northPanel.add(BorderLayout.NORTH, toolBar);
        northPanel.add(BorderLayout.CENTER, createScrollPane(deckEditor, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED));
        northPanel.add(BorderLayout.SOUTH, deckSummaryPanel);

        // south panel contains actions (north) / stats results (center)
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        southPanel.add(deckActionsPanel);
        southPanel.add(Box.createVerticalStrut(10));
        southPanel.add(statsTable.getTableHeader());
        southPanel.add(statsTable);
        southPanel.add(Box.createVerticalStrut(5));
        southPanel.add(statsActionsPanel);

        JPanel pane = new JPanel();
        pane.setLayout(new BorderLayout());
        pane.add(BorderLayout.CENTER, northPanel);
        pane.add(BorderLayout.SOUTH, southPanel);
        return pane;
    }

    private void parseAndColorizeDeckEditor(JTextPane deckEditor, JLabel deckSummary) {
        try {
            String text = deckEditor.getText();
            List<Deck.ParsedDeck.Line> lines = Deck.parseLines(new StringReader(text), managedCards);
            StyledDocument doc = deckEditor.getStyledDocument();
            // reset style
            doc.setCharacterAttributes(0, doc.getLength(), CARD_STYLE, false);

            int offset = 0;
            int main = 0;
            int side = 0;
            for (int ln = 0; ln < lines.size(); ln++) {
                Deck.ParsedDeck.Line line = lines.get(ln);
                int end = offset + line.getLine().length();
                if (line instanceof Deck.ParsedDeck.CommentLine) {
                    doc.setCharacterAttributes(offset, line.getLine().length(), COMMENT_STYLE, false);
                } else if (line instanceof Deck.ParsedDeck.SideboardSectionLine) {
                    doc.setCharacterAttributes(offset, line.getLine().length(), SIDEBOARD_STYLE, false);
                } else if (line instanceof Deck.ParsedDeck.CardLine) {
                    if (((Deck.ParsedDeck.CardLine) line).isMain()) {
                        main += ((Deck.ParsedDeck.CardLine) line).getCount();
                        if (!((Deck.ParsedDeck.CardLine) line).isKnown()) {
                            int cardIdx = ((Deck.ParsedDeck.CardLine) line).getCardIndex();
                            doc.setCharacterAttributes(offset + cardIdx, line.getLine().length() - cardIdx, UNKNOWN_CARD_STYLE, false);
                        }
//                        doc.setCharacterAttributes(offset, line.getLine().length(), CARD_STYLE, false);
                    } else {
                        side += ((Deck.ParsedDeck.CardLine) line).getCount();
                        if (!((Deck.ParsedDeck.CardLine) line).isKnown()) {
                            int cardIdx = ((Deck.ParsedDeck.CardLine) line).getCardIndex();
                            doc.setCharacterAttributes(offset, cardIdx, SIDEBOARD_STYLE, false);
                            doc.setCharacterAttributes(offset + cardIdx, line.getLine().length() - cardIdx, UNKNOWN_SIDEBOARD_STYLE, false);
                        } else {
                            doc.setCharacterAttributes(offset, line.getLine().length(), SIDEBOARD_STYLE, false);
                        }
                    }
                }
                offset = end + 1;
            }
            // update cards count
            deckSummary.setText("Main: " + main + (side == 0 ? "" : " / Side: " + side));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onSaveDeck(JTextPane deckInput) {
        JFileChooser fileDialog = new JFileChooser();
        fileDialog.setDialogType(JFileChooser.SAVE_DIALOG);
        fileDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
//        fileDialog.setDialogTitle("Select deck file");
        fileDialog.setCurrentDirectory(currentDir);
        int result = fileDialog.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileDialog.getSelectedFile();
            currentDir = selectedFile.getParentFile();
            try {
                ByteStreams.copy(new ByteArrayInputStream(deckInput.getText().getBytes(Charset.forName("utf-8"))), new FileOutputStream(selectedFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void onLoadDeck(JTextPane deck) {
        JFileChooser fileDialog = new JFileChooser();
        fileDialog.setDialogType(JFileChooser.OPEN_DIALOG);
        fileDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
//        fileDialog.setDialogTitle("Select deck file");
        fileDialog.setCurrentDirectory(currentDir);
        int result = fileDialog.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileDialog.getSelectedFile();
            currentDir = selectedFile.getParentFile();
            try {
                deck.setText(Files.asCharSource(selectedFile, Charset.forName("utf-8")).read());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void onObserveGames(String deckList, int iterations, GoldfishSimulator.Start start) {
        Class<? extends DeckPilot> pilotClass = ((PilotItem) pilotSelect.getSelectedItem()).pilotClass;
        if (pilotClass == null) {
            JOptionPane.showMessageDialog(this, "Choose a deck pilot first!");
            return;
        }

        try {
            Deck deck = Deck.parse(new StringReader(deckList));

//            System.out.println("Deck loaded: " + deck.getMain().size() + " cards (" + deck.getSideboard().size() + " cards in sideboard)");
//            System.out.println();

            // simulate games
            StringWriter logs = new StringWriter();
            GoldfishSimulator simulator = GoldfishSimulator.builder()
                    .iterations(iterations)
                    .pilotClass(pilotClass)
                    .start(start)
                    .maxTurns(10) // TODO: configurable ?
                    .out(new PrintWriter(logs))
                    .build();

            simulator.simulate(deck);

//            System.out.println("... game ended");

            // show game logs
            JDialog dialog = new JDialog(this, "Game logs", true);
            // TODO: colorize game logs
            JTextArea gameLogs = new JTextArea();
            gameLogs.setEditable(false);
            gameLogs.setFont(new Font("monospaced", Font.PLAIN, 14));
            gameLogs.setText(logs.toString());
            gameLogs.setMargin(new Insets(5, 5, 5, 5));

            dialog.add(createScrollPane(gameLogs, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED));
            dialog.setSize(500, 800);
            dialog.setLocation(200, 200);

            dialog.setVisible(true);
        } catch (Exception e) {
//            StringWriter stack = new StringWriter();
//            e.printStackTrace(new PrintWriter(stack));
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static JScrollPane createScrollPane(Component content, int hPolicy, int vPolicy) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setHorizontalScrollBarPolicy(hPolicy);
        scrollPane.setVerticalScrollBarPolicy(vPolicy);
        return scrollPane;
    }

    private static JTextField createIntegerInput(String text, int columns) {
        JTextField input = new JTextField(text, columns);
        input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() < '0' || e.getKeyChar() > '9') {
                    e.consume();
                }
            }
        });
        return input;
    }

    private void onComputeStats(String deckList, int iterations, GoldfishSimulator.Start start, JTable statsTable, JButton markAsRef) {
        Class<? extends DeckPilot> pilotClass = ((PilotItem) pilotSelect.getSelectedItem()).pilotClass;
        if (pilotClass == null) {
            JOptionPane.showMessageDialog(this, "Choose a deck pilot first!");
            return;
        }

        try {
//            System.out.println("Simulate goldfish (" + iterations + " iterations)");
            Deck deck = Deck.parse(new StringReader(deckList));

//            System.out.println("Deck loaded: " + deck.getMain().size() + " cards (" + deck.getSideboard().size() + " cards in sideboard)");
//            System.out.println();

            // simulate games
            GoldfishSimulator simulator = GoldfishSimulator.builder()
                    .iterations(iterations)
                    .pilotClass(pilotClass)
                    .start(GoldfishSimulator.Start.BOTH)
                    .maxTurns(10) // TODO: configurable ?
                    .build();

            // TODO: progress bar
            GoldfishSimulator.DeckStats stats = simulator.simulate(deck);

//            System.out.println("... simulation ended");

            // get (significant) win turns (columns)
            java.util.List<Integer> winTurns = stats.getWinTurns(result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON)
                    .stream()
                    .filter(turn -> {
                        long count = stats.count(result -> result.getEndTurn() == turn);
                        return moreThanOnePercent(count, stats.getIterations());
                    })
                    .collect(Collectors.toList());

            // make table columns
            List<String> columns = new ArrayList<>();
            columns.add("mulligans");
            if (start != GoldfishSimulator.Start.OTD) {
                columns.add("avg turn OTP");
            }
            if (start != GoldfishSimulator.Start.OTP) {
                columns.add("avg turn OTD");
            }
            winTurns.forEach(turn -> {
                if (start != GoldfishSimulator.Start.OTD) {
                    columns.add("T" + turn + " OTP");
                }
                if (start != GoldfishSimulator.Start.OTP) {
                    columns.add("T" + turn + " OTD");
                }
            });

            List<List<StatCell>> rows = new ArrayList<>();

            // one row per mulligans taken
            stats.getMulligans().forEach(mulligansTaken -> {
                long totalGamesWithThisNumberOfMulligans = stats.count(result -> result.getMulligans() == mulligansTaken);
                if (moreThanOnePercent(totalGamesWithThisNumberOfMulligans, stats.getIterations())) {
                    rows.add(createStatsRow(stats, mulligansTaken, start, winTurns));
                }
            });

            // last row is global
            rows.add(createStatsRow(stats, -1, start, winTurns));

            // update table
            statsTable.setModel(new AbstractTableModel() {
                public int getColumnCount() {
                    return columns.size();
                }

                public String getColumnName(int column) {
                    return columns.get(column);
                }

                public int getRowCount() {
                    return rows.size();
                }

                public Object getValueAt(int row, int col) {
                    return rows.get(row).get(col);
                }
            });

            // set pref columns preferred width
            for (int column = 0; column < statsTable.getColumnCount(); column++) {
                TableColumn tableColumn = statsTable.getColumnModel().getColumn(column);
                if (column == 0) {
                    tableColumn.setPreferredWidth(100);
                } else if ((start == GoldfishSimulator.Start.BOTH && column <= 2) || (start != GoldfishSimulator.Start.BOTH && column == 1)) {
                    tableColumn.setPreferredWidth(100);
                } else {
                    tableColumn.setPreferredWidth(50);
                }
            }
            statsTable.setVisible(true);
            markAsRef.setVisible(true);
            while (markAsRef.getActionListeners().length > 0) {
                markAsRef.removeActionListener(markAsRef.getActionListeners()[0]);
            }
            markAsRef.addActionListener(e -> {
                reference = stats;
                // re-render this table: other will be automatically refreshed
                statsTable.repaint();
            });
        } catch (Exception e) {
//            StringWriter stack = new StringWriter();
//            e.printStackTrace(new PrintWriter(stack));
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<StatCell> createStatsRow(GoldfishSimulator.DeckStats stats, int mulligans, GoldfishSimulator.Start start, List<Integer> winTurns) {
        List<StatCell> row = new ArrayList<>(winTurns.size() + 1);
        // row title
        row.add(new StatCell(stats, null, mulligans, 0));

        // first column: avg win turn
        if (start != GoldfishSimulator.Start.OTD) {
            row.add(new StatCell(stats, GoldfishSimulator.Start.OTP, mulligans, 0));
        }
        if (start != GoldfishSimulator.Start.OTP) {
            row.add(new StatCell(stats, GoldfishSimulator.Start.OTD, mulligans, 0));
        }

        // one column per win turn
        winTurns.forEach(turn -> {
            if (start != GoldfishSimulator.Start.OTD) {
                row.add(new StatCell(stats, GoldfishSimulator.Start.OTP, mulligans, turn));
            }
            if (start != GoldfishSimulator.Start.OTP) {
                row.add(new StatCell(stats, GoldfishSimulator.Start.OTD, mulligans, turn));
            }
        });
        return row;
    }

    class StatsTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            TableModel model = table.getModel();
            StatCell cell = (StatCell) model.getValueAt(row, column);
            String text = cell.toString(statsIntFilter);
            Component cmp = super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
            cell.applyStyle(cmp, reference);
            return cmp;
        }
    }

    @Value
    private static class Average implements Comparable<Average> {
        final double value;
        final double mad; // mean absolute deviation

        public String toString() {
            return String.format("%.2f ±%.2f", value, mad);
        }

        @Override
        public int compareTo(Average other) {
            return Double.compare(value, other.value);
        }
    }

    @Value
    private static class Percentage implements Comparable<Percentage> {
        final long count;
        final long total;

        public String toString() {
            //        return String.format("%.1f%% (%d/%d)", (100f * (float) count / (float) total), count, total);
            return String.format("%.1f%%", value());
        }

        protected double value() {
            return 100d * (double) count / (double) total;
        }

        @Override
        public int compareTo(Percentage other) {
            return Double.compare(value(), other.value());
        }
    }

    @Value
    private static class StatCell {
        final GoldfishSimulator.DeckStats stats;
        final GoldfishSimulator.Start start;
        final int mulligans;
        final int winTurn;

        enum Comparison {better, worse, same}

        public Comparison compareTo(GoldfishSimulator.DeckStats other) {
            if (other == stats) {
                return Comparison.same;
            }
            Comparable thisCmp = value(stats, LOWEREQ);
            Comparable otherCmp = value(other, LOWEREQ);
            if (thisCmp instanceof Average) {
                // the lower the better
                double diff = ((Average) otherCmp).value - ((Average) thisCmp).value;
                if (diff >= AVG_TRESH) {
                    return Comparison.better;
                } else if (diff <= -AVG_TRESH) {
                    return Comparison.worse;
                } else {
                    return Comparison.same;
                }
            } else if (thisCmp instanceof Percentage) {
                // the greater the better
                double diff = ((Percentage) thisCmp).value() - ((Percentage) otherCmp).value();
                if (diff >= PERCENT_TRESH) {
                    return Comparison.better;
                } else if (diff <= -PERCENT_TRESH) {
                    return Comparison.worse;
                } else {
                    return Comparison.same;
                }
            } else {
                return Comparison.same;
            }
        }

        public Comparable value(GoldfishSimulator.DeckStats stats, BiFunction<Integer, Integer, Boolean> intMatcher) {
            Predicate<GoldfishSimulator.GameResult> withMulligans = mulligans < 0 ? Predicates.alwaysTrue() : result -> intMatcher.apply(result.getMulligans(), mulligans);
            if (start == null) {
                if (mulligans < 0) {
                    return "GLOBAL";
                } else {
                    // percentage of hands kept with that many mulligans
                    long total = stats.getIterations();
                    long count = stats.count(withMulligans);
                    return new Percentage(count, total);
                }
            }

            Predicate<GoldfishSimulator.GameResult> withMulligansAndStart = withMulligans.and(result -> start == result.getStart());
            if (winTurn == 0) {
                // average win turn + avg mean difference
                return new Average(stats.getAverageWinTurn(withMulligansAndStart), stats.getWinTurnMAD(withMulligansAndStart));
            } else {
                // percentage
                Predicate<GoldfishSimulator.GameResult> withMulligansAndStartAndWinTurn = withMulligansAndStart.and(result -> intMatcher.apply(result.getEndTurn(), winTurn));
                long total = stats.count(withMulligansAndStart);
                long count = stats.count(withMulligansAndStartAndWinTurn);
                return new Percentage(count, total);
            }
        }

        public String toString(BiFunction<Integer, Integer, Boolean> intMatcher) {
            if (start == null && mulligans >= 0) {
                return mulligans + " (" + value(stats, intMatcher) + ")";
            } else {
                return value(stats, intMatcher).toString();
            }
        }

        public String toString() {
            return toString(EQUALS);
        }

        public void applyStyle(Component cmp, GoldfishSimulator.DeckStats reference) {
            // background
            if (mulligans < 0) {
                // last row
                cmp.setBackground(SILVER);
            } else {
                cmp.setBackground(Color.WHITE);
            }
            // font
            if (winTurn <= 0) {
                // first 3 columns are bold
                // TODO: different if not OTP+OTD
                cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));
            }
            // foreground color
            cmp.setForeground(Color.BLACK);
            if (reference != null) {
                Comparison cc = compareTo(reference);
                if (cc == Comparison.better) {
                    cmp.setForeground(OLIVE);
                } else if (cc == Comparison.worse) {
                    cmp.setForeground(RED);
                }
            }
        }
    }

    private static boolean moreThanOnePercent(long count, int total) {
        return count * 100 / total > 1;
    }

}

