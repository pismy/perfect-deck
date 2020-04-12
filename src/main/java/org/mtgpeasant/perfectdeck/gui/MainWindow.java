package org.mtgpeasant.perfectdeck.gui;

import org.mtgpeasant.decks.PilotScanner;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class MainWindow extends JFrame implements GuiOptionsHandler {
    private GoldfishSimulator.DeckStats reference;

    // stateful fields
    private final JComboBox pilotSelect;
    private final JCheckBox cumulatedStats;
    private final JLabel pilotInfoHyperlink;

    public static void main(String[] args) {
        MainWindow window = new MainWindow();
        window.setMinimumSize(new Dimension(500, 480));
        // TODO: save settings
        window.setSize(new Dimension(600, 800));
        window.setLocation(100, 100);
        window.setVisible(true);
    }

    MainWindow() {
        setTitle("Perfect Deck");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // north panel: deck pilot combo selector
        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel("Deck Pilot: ");
        pilotInfoHyperlink = new JLabel("XX cards");
        pilotInfoHyperlink.setVisible(false);
        pilotInfoHyperlink.setToolTipText("Click to see pilot info");
        pilotInfoHyperlink.setForeground(UI.TEAL);
        pilotInfoHyperlink.setFont(pilotInfoHyperlink.getFont().deriveFont(12));
        pilotInfoHyperlink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // show pilot info dialog
                JDialog dialog = new PilotInfoDialog(SwingUtilities.getWindowAncestor(MainWindow.this), getCurrentPilot());
                dialog.setSize(450, 600);
                dialog.setLocation(200, 200);
                dialog.setVisible(true);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                pilotInfoHyperlink.setForeground(UI.OLIVE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                pilotInfoHyperlink.setForeground(UI.TEAL);
            }
        });

        pilotSelect = new JComboBox();
        pilotSelect.addItem(new PilotScanner.PilotMetadata("Choose...", null, null, null));
        try {
            PilotScanner.scan().forEach(pd -> pilotSelect.addItem(pd));
        } catch (IOException e) {
            e.printStackTrace();
        }
        pilotSelect.addActionListener(e -> onPilotSelected());

        northPanel.add(label);
        northPanel.add(pilotSelect);
        northPanel.add(Box.createHorizontalStrut(10));
        northPanel.add(pilotInfoHyperlink);
        northPanel.add(Box.createHorizontalStrut(20));
        cumulatedStats = new JCheckBox("cumulated stats", false);
        northPanel.add(cumulatedStats);
        cumulatedStats.setToolTipText("Toggles cumulated stats");
        cumulatedStats.addActionListener(e -> repaint());

        // center: deck tabbed panef
        JTabbedPane deckTabs = new JTabbedPane();
        DeckTab deckTab = new DeckTab(this);
//        SwingUtilities.invokeLater(() -> deckTab.setText("// type in your deck here...\n"));
//        deckTab.setText("// type in your deck here...\n");
        deckTabs.addTab("Main", deckTab);
        // TODO: add a + icon

        getContentPane().add(BorderLayout.NORTH, northPanel);
        getContentPane().add(BorderLayout.CENTER, deckTabs);
    }

    private void onPilotSelected() {
        // reset known cards
        pilotInfoHyperlink.setVisible(false);
        if (getManagedCards() != null) {
            pilotInfoHyperlink.setVisible(true);
            pilotInfoHyperlink.setText(getManagedCards().size() + " cards");
        }
        // TODO: parse and colorize every open deck
    }

    @Override
    public GoldfishSimulator.DeckStats getReference() {
        return reference;
    }

    @Override
    public void setReference(GoldfishSimulator.DeckStats reference) {
        this.reference = reference;
    }

    @Override
    public Class<? extends DeckPilot> getPilotClass() {
        return getCurrentPilot().getPilotClass();
    }

    @Override
    public Cards getManagedCards() {
        return getCurrentPilot().getManagedCards();
    }

    @Override
    public boolean cumulated() {
        return cumulatedStats.isSelected();
    }

    PilotScanner.PilotMetadata getCurrentPilot() {
        return (PilotScanner.PilotMetadata) pilotSelect.getSelectedItem();
    }

}

