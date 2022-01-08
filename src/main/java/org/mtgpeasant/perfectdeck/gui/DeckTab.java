package org.mtgpeasant.perfectdeck.gui;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.mtgpeasant.perfectdeck.common.cards.Deck;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.charset.Charset;

/**
 * Deck list editor + bottom bar (summary)
 *
 * <pre>
 * +---------------------------------------+
 * | toolbar: (load) (save) (copy)         |
 * +---------------------------------------+
 * | (deck editor)                         |
 * |                                       |
 * |                                       |
 * |                                       |
 * |                                       |
 * |                                       |
 * +---------------------------------------+
 * | actions: observe / stats              |
 * +---------------------------------------+
 * | (stats)                               |
 * | OTP ...                               |
 * | OTD ...                               |
 * |                                       |
 * |           (mark ref)                  |
 * +---------------------------------------+
 * </pre>
 * TODO: auto-suggest (on CRTL+space)
 */
public class DeckTab extends JPanel {
    private static int currentTabIndex = 0;
    private static File currentDir = new File(".");

    private final GuiOptionsHandler handler;
    private final DeckEditor deckEditor;
    private final GameStatsPanel statsPanel;

    public DeckTab(GuiOptionsHandler handler) {
        this.handler = handler;

        // deck (text area)
        deckEditor = new DeckEditor(handler);

        // center panel contains toolbar (north) / deck area (center)
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        centerPanel.add(BorderLayout.NORTH, createDeckToolbar());
        centerPanel.add(BorderLayout.CENTER, deckEditor);
        centerPanel.add(BorderLayout.SOUTH, createDeckActionsBar());

        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, centerPanel);
        statsPanel = new GameStatsPanel(handler);
        // hidden at first...
        statsPanel.setVisible(false);
        add(BorderLayout.SOUTH, statsPanel);
    }

    Component createDeckToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setMargin(new Insets(5, 5, 5, 5));

        JButton load = new JButton();
        load.addActionListener(e -> onLoadDeck());
//        open.setIcon(new ImageIcon("Open24"));
        load.setText("load");
        toolBar.add(load);

        JButton save = new JButton();
        save.addActionListener(e -> onSaveDeck());
//        save.setIcon(new ImageIcon("Save24"));
        save.setText("save");
        toolBar.add(save);

        toolBar.add(new JToolBar.Separator());
        JButton duplicate = new JButton();
        duplicate.addActionListener(e -> cloneTab());
//        duplicate.setIcon(new ImageIcon("New24"));
        duplicate.setText("duplicate");
        duplicate.setToolTipText("Copies this deck into a new tab");
        toolBar.add(duplicate);

        return toolBar;
    }

    private void cloneTab() {
        JTabbedPane tabbedPane = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, this);
//            JTabbedPane tabbedPane = (JTabbedPane) pane.getParent();
        String title = "Alt. " + currentTabIndex++;
        Component newTab = new DeckTab(handler);
        ((DeckTab) newTab).setText(deckEditor.getText());
        tabbedPane.addTab(title, newTab);

        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabPanel.setOpaque(false);
        tabPanel.add(new JLabel(title));
        tabPanel.add(Box.createHorizontalStrut(5));
        JLabel closeBtn = new JLabel("✖"); // ❎❌✖✕
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
                closeBtn.setForeground(UI.RED);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setForeground(Color.BLACK);
            }
        });
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, tabPanel);
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }

    Component createDeckActionsBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        panel.setBackground(Color.LIGHT_GRAY);
//        JToolBar panel = new JToolBar();
//        panel.setMargin(new Insets(5, 5, 5, 5));
//        panel.setFloatable(false);

        JLabel observe = new JLabel("observe");
        observe.setForeground(UI.BLUE);
        panel.add(observe);

        // TODO: settings
        JTextField observeInput = UI.createIntegerInput("2", 2);
        panel.add(observeInput);
        JLabel games = new JLabel("games");
        games.setForeground(UI.BLUE);
        panel.add(games);
//        panel.set
        JButton oneGameBut = new JButton("Go!");
        oneGameBut.setToolTipText("Simulate some games (with logs)");
        oneGameBut.addActionListener(e -> onObserveGames(Integer.parseInt(observeInput.getText())));
        oneGameBut.setBackground(UI.BLUE);
        oneGameBut.setForeground(Color.WHITE);
        panel.add(oneGameBut);

        panel.add(Box.createHorizontalStrut(30));

        JLabel stats_on = new JLabel("stats on");
        stats_on.setForeground(UI.RED);
        panel.add(stats_on);

        // TODO: settings
        JTextField iterationsInput = UI.createIntegerInput("50000", 6);
        panel.add(iterationsInput);

        JLabel games2 = new JLabel("games");
        games2.setForeground(UI.RED);
        panel.add(games2);

        JButton goldfishBut = new JButton("Go !");
        goldfishBut.setToolTipText("Starts a goldfish simulation and computes statistics");
        goldfishBut.addActionListener(e -> onComputeStats(Integer.parseInt(iterationsInput.getText())));
        goldfishBut.setBackground(UI.RED);
        goldfishBut.setForeground(Color.WHITE);
        panel.add(goldfishBut);
        return panel;
    }

    /**
     * Sets the deck list
     */
    void setText(String deck) {
        deckEditor.setText(deck);
    }

    /**
     * Gets the deck list
     */
    String getText() {
        return deckEditor.getText();
    }

    private void onSaveDeck() {
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
                ByteStreams.copy(new ByteArrayInputStream(deckEditor.getText().getBytes(Charset.forName("utf-8"))), new FileOutputStream(selectedFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void onLoadDeck() {
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
                deckEditor.setText(Files.asCharSource(selectedFile, Charset.forName("utf-8")).read());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void onObserveGames(int iterations) {
        Class<? extends DeckPilot> pilotClass = handler.getPilotClass();
        if (pilotClass == null) {
            JOptionPane.showMessageDialog(this, "Choose a deck pilot first!");
            return;
        }

        try {
            Deck deck = Deck.parse(new StringReader(deckEditor.getText()));

            // simulate games
            StringWriter logs = new StringWriter();
            GoldfishSimulator simulator = GoldfishSimulator.builder()
                    .iterations(iterations)
                    .pilotClass(pilotClass)
                    .start(GoldfishSimulator.Start.BOTH)
                    .maxTurns(10) // TODO: configurable ?
                    .out(new PrintWriter(logs))
                    .build();

            simulator.simulate(deck);

            // show game logs
            JDialog dialog = new GameLogsDialog(SwingUtilities.getWindowAncestor(this));
            ((GameLogsDialog) dialog).setLogs(logs.toString());
            dialog.setSize(800, 800);
            dialog.setLocation(200, 200);
            dialog.setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void onComputeStats(int iterations) {
        Class<? extends DeckPilot> pilotClass = handler.getPilotClass();
        if (pilotClass == null) {
            JOptionPane.showMessageDialog(this, "Choose a deck pilot first!");
            return;
        }

        try {
            Deck deck = Deck.parse(new StringReader(deckEditor.getText()));

            // simulate games
            GoldfishSimulator simulator = GoldfishSimulator.builder()
                    .iterations(iterations)
                    .pilotClass(pilotClass)
                    .start(GoldfishSimulator.Start.BOTH)
                    .maxTurns(10) // TODO: configurable ?
                    .build();

            // TODO: progress bar
            GoldfishSimulator.DeckStats stats = simulator.simulate(deck);
            statsPanel.refresh(stats);
            statsPanel.setVisible(true);
        } catch (Exception e) {
//            StringWriter stack = new StringWriter();
//            e.printStackTrace(new PrintWriter(stack));
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
