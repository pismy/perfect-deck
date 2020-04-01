package org.mtgpeasant.perfectdeck;

import com.google.common.base.Predicates;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import lombok.Value;
import org.mtgpeasant.decks.burn.BurnDeckPilot;
import org.mtgpeasant.decks.infect.InfectDeckPilot;
import org.mtgpeasant.decks.reanimator.ReanimatorDeckPilot;
import org.mtgpeasant.decks.stompy.StompyDeckPilot;
import org.mtgpeasant.perfectdeck.common.cards.Deck;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PerfectDeck extends JFrame {
    private final JComboBox pilotSelect;
    private final JTabbedPane deckTabs;
    private GoldfishSimulator.DeckStats reference;

    public static final Color BLUE = new Color(0x0074D9);
    public static final Color RED = new Color(0xFF4136);
    public static final Color GRAY = new Color(0xAAAAAA);
    public static final Color ORANGE = new Color(0xFF851B);
    public static final Color NAVY = new Color(0x001f3f);
    public static final Color TEAL = new Color(0x39CCCC);
    public static final Color OLIVE = new Color(0x3D9970);
    public static final Color YELLOW = new Color(0xFFDC00);
    public static final Color SILVER = new Color(0xDDDDDD);
    private int altIndex = 0;

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
        window.setSize(new Dimension(640, 800));
//        window.setExtendedState(Frame.MAXIMIZED_BOTH);
        window.setVisible(true);
    }

    public PerfectDeck() throws HeadlessException {
        setTitle("Perfect Deck");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel("Deck Pilot: ");
        pilotSelect = new JComboBox();
        pilotSelect.addItem(new PilotItem("Choose...", null));
        pilotSelect.addItem(new PilotItem("Burn", BurnDeckPilot.class));
        pilotSelect.addItem(new PilotItem("Reanimator", ReanimatorDeckPilot.class));
        pilotSelect.addItem(new PilotItem("Stompy", StompyDeckPilot.class));
        pilotSelect.addItem(new PilotItem("Infect", InfectDeckPilot.class));

        northPanel.add(label);
        northPanel.add(pilotSelect);

        deckTabs = new JTabbedPane();
        deckTabs.addTab("Main", deckTab(true, "// type in your deck here...\n"));

        //Adding Components to the frame.
        getContentPane().add(BorderLayout.NORTH, northPanel);
        getContentPane().add(BorderLayout.CENTER, deckTabs);
    }

    private Component deckTab(boolean main, String deck) {
        JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // deck (text area)
        JTextPane deckEditor = new JTextPane();
        deckEditor.setMinimumSize(new Dimension(400, 300));
        deckEditor.setText(deck);
//        deckEditor.setCharacterAttributes();
        deckEditor.setFont(new Font("monospaced", Font.BOLD, 16));
        deckEditor.setAutoscrolls(true);

        // deck toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setMargin(new Insets(5, 5, 5, 5));

        JButton open = new JButton();
        open.addActionListener(e -> openDeck(deckEditor));
//        open.setIcon(new ImageIcon("Open24"));
        open.setText("open");
        toolBar.add(open);

        JButton save = new JButton();
        save.addActionListener(e -> saveDeck(deckEditor));
//        save.setIcon(new ImageIcon("Save24"));
        save.setText("save");
        toolBar.add(save);

        toolBar.add(new JToolBar.Separator());
        JButton duplicate = new JButton();
        duplicate.addActionListener(e -> {
            JTabbedPane tabbedPane = (JTabbedPane) pane.getParent();
            String title = "Alt. " + altIndex++;
            Component newTab = deckTab(false, deckEditor.getText());
            tabbedPane.addTab(title, newTab);

            JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            tabPanel.setOpaque(false);
            tabPanel.add(new JLabel(title));
            JLabel closeBtn = new JLabel("X");
            closeBtn.setForeground(RED);
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
            });
            tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, tabPanel);
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        });
//        duplicate.setIcon(new ImageIcon("New24"));
        duplicate.setText("copy");
        duplicate.setToolTipText("Copies this deck into a new tab");
        toolBar.add(duplicate);

        // actions panel
        JPanel actionsPanel = new JPanel();

        actionsPanel.add(new JLabel("observe"));

        JTextField observeInput = new JTextField("2", 1);
        observeInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() < '0' || e.getKeyChar() > '9') {
                    e.consume();
                }
            }
        });
        actionsPanel.add(observeInput);
        actionsPanel.add(new JLabel("games"));
//        actionsPanel.set
        JButton oneGameBut = new JButton("Go!");
        oneGameBut.setToolTipText("Simulate some games (with logs)");
        // TODO: start
        oneGameBut.addActionListener(e -> simulateOneGame(deckEditor.getText(), Integer.parseInt(observeInput.getText()), GoldfishSimulator.Start.BOTH));
        oneGameBut.setBackground(BLUE);
        oneGameBut.setForeground(Color.WHITE);
        actionsPanel.add(oneGameBut);


        // TODO: OTD/OTP/both
        actionsPanel.add(Box.createHorizontalStrut(80));


        actionsPanel.add(new JLabel("stats on"));

        JTextField iterationsInput = new JTextField("50000", 6);
        iterationsInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() < '0' || e.getKeyChar() > '9') {
                    e.consume();
                }
            }
        });
        actionsPanel.add(iterationsInput);

        actionsPanel.add(new JLabel("games"));

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
        goldfishBut.addActionListener(e -> simulateGoldfish(deckEditor.getText(), Integer.parseInt(iterationsInput.getText()), GoldfishSimulator.Start.BOTH, statsTable, markAsRef));
        goldfishBut.setBackground(RED);
        goldfishBut.setForeground(Color.WHITE);
        actionsPanel.add(goldfishBut);

        // north panel contains toolbar (north) / deck area (center)
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BorderLayout());
        northPanel.add(BorderLayout.NORTH, toolBar);
        northPanel.add(BorderLayout.CENTER, scrollPane(deckEditor, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED));

        // south panel contains actions (north) / stats results (center)
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BorderLayout());
        southPanel.add(BorderLayout.NORTH, actionsPanel);
        southPanel.add(BorderLayout.CENTER, new JScrollPane(statsTable));
        southPanel.add(BorderLayout.SOUTH, statsActionsPanel);

        pane.setTopComponent(northPanel);
        pane.setBottomComponent(southPanel);
        pane.setDividerLocation(.8d);

        return pane;
    }

    private void saveDeck(JTextPane deckInput) {
        System.out.println("open...");
        JFileChooser fileDialog = new JFileChooser();
        fileDialog.setDialogType(JFileChooser.SAVE_DIALOG);
        fileDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
//        fileDialog.setDialogTitle("Select deck file");
        fileDialog.setCurrentDirectory(new File("."));
        int result = fileDialog.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileDialog.getSelectedFile();
            try {
                ByteStreams.copy(new ByteArrayInputStream(deckInput.getText().getBytes(Charset.forName("utf-8"))), new FileOutputStream(selectedFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void openDeck(JTextPane deck) {
        System.out.println("open...");
        JFileChooser fileDialog = new JFileChooser();
        fileDialog.setDialogType(JFileChooser.OPEN_DIALOG);
        fileDialog.setFileSelectionMode(JFileChooser.FILES_ONLY);
//        fileDialog.setDialogTitle("Select deck file");
        fileDialog.setCurrentDirectory(new File("."));
        int result = fileDialog.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileDialog.getSelectedFile();
            try {
                deck.setText(Files.asCharSource(selectedFile, Charset.forName("utf-8")).read());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void simulateOneGame(String deckList, int iterations, GoldfishSimulator.Start start) {
        Class<? extends DeckPilot> pilotClass = ((PilotItem) pilotSelect.getSelectedItem()).pilotClass;
        if (pilotClass == null) {
            JOptionPane.showMessageDialog(this, "Choose a deck pilot first!");
            return;
        }

        try {
            System.out.println("Simulate one game");
            Deck deck = Deck.parse(new StringReader(deckList));

            System.out.println("Deck loaded: " + deck.getMain().size() + " cards (" + deck.getSideboard().size() + " cards in sideboard)");
            System.out.println();

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

            System.out.println("... game ended");

            // show game logs
            JDialog dialog = new JDialog(this, "Game logs", true);
            JTextArea gameLogs = new JTextArea();
            gameLogs.setEditable(false);
            gameLogs.setFont(new Font("monospaced", Font.PLAIN, 14));
            gameLogs.setText(logs.toString());
            gameLogs.setMargin(new Insets(5, 5, 5, 5));

            dialog.add(scrollPane(gameLogs, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED));
            dialog.setSize(500, 800);

            dialog.setVisible(true);
        } catch (Exception e) {
//            StringWriter stack = new StringWriter();
//            e.printStackTrace(new PrintWriter(stack));
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static JScrollPane scrollPane(Component content, int hPolicy, int vPolicy) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setHorizontalScrollBarPolicy(hPolicy);
        scrollPane.setVerticalScrollBarPolicy(vPolicy);
        return scrollPane;
    }

    private void simulateGoldfish(String deckList, int iterations, GoldfishSimulator.Start start, JTable statsTable, JButton markAsRef) {
        Class<? extends DeckPilot> pilotClass = ((PilotItem) pilotSelect.getSelectedItem()).pilotClass;
        if (pilotClass == null) {
            JOptionPane.showMessageDialog(this, "Choose a deck pilot first!");
            return;
        }

        try {
            System.out.println("Simulate goldfish (" + iterations + " iterations)");
            Deck deck = Deck.parse(new StringReader(deckList));

            System.out.println("Deck loaded: " + deck.getMain().size() + " cards (" + deck.getSideboard().size() + " cards in sideboard)");
            System.out.println();

            // simulate games
            GoldfishSimulator simulator = GoldfishSimulator.builder()
                    .iterations(iterations)
                    .pilotClass(pilotClass)
                    .start(GoldfishSimulator.Start.BOTH)
                    .maxTurns(10) // TODO: configurable ?
                    .build();

            GoldfishSimulator.DeckStats stats = simulator.simulate(deck);

            System.out.println("... simulation ended");

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
                    rows.add(computeRow(stats, mulligansTaken, start, winTurns));
                }
            });

            // last row is global
            rows.add(computeRow(stats, -1, start, winTurns));

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
            // TODO: on mark as ref, re-render every table
            markAsRef.addActionListener(e -> reference = stats);
        } catch (Exception e) {
//            StringWriter stack = new StringWriter();
//            e.printStackTrace(new PrintWriter(stack));
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<StatCell> computeRow(GoldfishSimulator.DeckStats stats, int mulligans, GoldfishSimulator.Start start, List<Integer> winTurns) {
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
            Component cmp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            StatCell cell = (StatCell) model.getValueAt(row, column);
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
            // TODO
            return (int) (100d * (value - other.value));
        }
    }

    @Value
    private static class Percentage implements Comparable<Percentage> {
        final long count;
        final long total;

        public String toString() {
            //        return String.format("%.1f%% (%d/%d)", (100f * (float) count / (float) total), count, total);
            return String.format("%.1f%%", (100f * (float) count / (float) total));
        }

        public String details() {
            return count + " on " + total;
        }

        protected double value() {
            return 100d * (double) count / (double) total;
        }

        @Override
        public int compareTo(Percentage other) {
            // TODO
            return (int) (value() - other.value());
        }
    }

    @Value
    private static class StatCell {
        final GoldfishSimulator.DeckStats stats;
        final GoldfishSimulator.Start start;
        final int mulligans;
        final int winTurn;

        public int compareTo(GoldfishSimulator.DeckStats other) {
            return value(stats).compareTo(value(other));
        }

        public Comparable value(GoldfishSimulator.DeckStats stats) {
            Predicate<GoldfishSimulator.GameResult> withMulligans = mulligans < 0 ? Predicates.alwaysTrue() : result -> result.getMulligans() == mulligans;
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
                Predicate<GoldfishSimulator.GameResult> withMulligansAndStartAndWinTurn = withMulligansAndStart.and(result -> result.getEndTurn() == winTurn);
                long total = stats.count(withMulligansAndStart);
                long count = stats.count(withMulligansAndStartAndWinTurn);
                return new Percentage(count, total);
            }
        }

        public String toString() {
            if (start == null && mulligans >= 0) {
                return mulligans + " (" + value(stats) + ")";
            } else {
                return value(stats).toString();
            }
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
                int cc = value(stats).compareTo(value(reference));
                if (cc < -2) {
                    cmp.setForeground(BLUE);
                } else if (cc > 2) {
                    cmp.setForeground(RED);
                }
            }
        }
    }

    private static boolean moreThanOnePercent(long count, int total) {
        return count * 100 / total > 1;
    }

}

