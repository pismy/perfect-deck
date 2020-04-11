package org.mtgpeasant.perfectdeck.gui;

import com.google.common.base.Joiner;
import lombok.Value;
import org.mtgpeasant.decks.burn.BurnDeckPilot;
import org.mtgpeasant.decks.infect.InfectDeckPilot;
import org.mtgpeasant.decks.reanimator.ReanimatorDeckPilot;
import org.mtgpeasant.decks.stompy.StompyDeckPilot;
import org.mtgpeasant.perfectdeck.common.cards.Cards;
import org.mtgpeasant.perfectdeck.goldfish.DeckPilot;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainWindow extends JFrame implements GuiOptionsHandler {
    private GoldfishSimulator.DeckStats reference;

    // stateful fields
    private final JComboBox pilotSelect;
    private final JCheckBox cumulatedStats;
    private final JLabel managedCardsHyperlink;
    private Cards managedCards = null;

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
        managedCardsHyperlink = new JLabel("XX cards");
        managedCardsHyperlink.setVisible(false);
        managedCardsHyperlink.setToolTipText("Click to see managed cards");
        managedCardsHyperlink.setForeground(UI.TEAL);
        managedCardsHyperlink.setFont(managedCardsHyperlink.getFont().deriveFont(12));
        managedCardsHyperlink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (managedCards != null) {
                    // TODO: selectable
                    JOptionPane.showMessageDialog(MainWindow.this, Joiner.on('\n').join(managedCards), "Managed Cards", JOptionPane.PLAIN_MESSAGE);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                managedCardsHyperlink.setForeground(UI.OLIVE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                managedCardsHyperlink.setForeground(UI.TEAL);
            }
        });

        pilotSelect = new JComboBox();
        pilotSelect.addItem(new PilotItem("Choose...", null));
        // TODO: auto discover all deck pilot classes
        pilotSelect.addItem(new PilotItem("Burn", BurnDeckPilot.class));
        pilotSelect.addItem(new PilotItem("Reanimator", ReanimatorDeckPilot.class));
        pilotSelect.addItem(new PilotItem("Stompy", StompyDeckPilot.class));
        pilotSelect.addItem(new PilotItem("Infect", InfectDeckPilot.class));
        pilotSelect.addActionListener(e -> onPilotSelected());

        northPanel.add(label);
        northPanel.add(pilotSelect);
        northPanel.add(Box.createHorizontalStrut(10));
        northPanel.add(managedCardsHyperlink);
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
        managedCards = null;
        managedCardsHyperlink.setVisible(false);
        Class<? extends DeckPilot> pilotClass = getPilotClass();
        if (pilotClass != null) {
            managedCards = DeckPilot.loadManagedCards(pilotClass);
            if (managedCards != null) {
                managedCardsHyperlink.setVisible(true);
                managedCardsHyperlink.setText(managedCards.size() + " cards");
            }
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
        PilotItem item = (PilotItem) pilotSelect.getSelectedItem();
        return item.getPilotClass();
    }

    @Override
    public Cards managedCards() {
        return managedCards;
    }

    @Override
    public boolean cumulated() {
        return cumulatedStats.isSelected();
    }
}

