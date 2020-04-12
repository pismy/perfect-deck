package org.mtgpeasant.perfectdeck.gui;

import lombok.Value;
import org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.mtgpeasant.perfectdeck.goldfish.GoldfishSimulator.DeckStats.*;

public class GameStatsPanel extends JPanel {
    private static final double AVG_TRESH = .1d;
    private static final double PERCENT_TRESH = 2d;

    private final GuiOptionsHandler handler;
    private final JTable otpTable;
    private final JTable otdTable;
    private final JButton markAsRef;

    public GameStatsPanel(GuiOptionsHandler handler) {
        this.handler = handler;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        otpTable = new JTable();
        otpTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        otpTable.setDefaultRenderer(Object.class, new StatsTableRenderer());

        otdTable = new JTable();
        otdTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        otdTable.setDefaultRenderer(Object.class, new StatsTableRenderer());

        markAsRef = new JButton("mark as ref.");
        markAsRef.setToolTipText("Mark those stats as reference (all other stats will be compared to this)");

        JPanel statsActionsPanel = new JPanel();
        statsActionsPanel.add(markAsRef);

        add(Box.createVerticalStrut(5));
        add(UI.createFlowPanel(FlowLayout.LEFT, new JLabel("ON THE PLAY")));
        add(otpTable.getTableHeader());
        add(otpTable);

        add(Box.createVerticalStrut(5));
        add(UI.createFlowPanel(FlowLayout.LEFT, new JLabel("ON THE DRAW")));
        add(otdTable.getTableHeader());
        add(otdTable);

        add(Box.createVerticalStrut(5));
        add(statsActionsPanel);
    }

    /**
     * Refreshes the view with given game statistics
     */
    public void refresh(GoldfishSimulator.DeckStats stats) {
        // get (significant) win turns (columns)
        java.util.List<Integer> winTurns = stats.getWinTurns(result -> result.getOutcome() == GoldfishSimulator.GameResult.Outcome.WON)
                .stream()
                .filter(turn -> {
                    long count = stats.count(result -> result.getEndTurn() == turn);
                    return moreThanOnePercent(count, stats.getIterations());
                })
                .collect(Collectors.toList());

        // make table columns
        java.util.List<String> columns = new ArrayList<>();
        columns.add("mulligans");
        columns.add("avg win turn");
        winTurns.forEach(turn -> {
            columns.add("win T" + turn);
        });

        populateStats(otpTable, stats, winTurns, columns, GoldfishSimulator.Start.OTP);
        populateStats(otdTable, stats, winTurns, columns, GoldfishSimulator.Start.OTD);

        while (markAsRef.getActionListeners().length > 0) {
            markAsRef.removeActionListener(markAsRef.getActionListeners()[0]);
        }
        markAsRef.addActionListener(e -> {
            handler.setReference(stats);
            // re-render this table: other will be automatically refreshed
            otpTable.repaint();
            otdTable.repaint();
        });
    }

    private void populateStats(JTable table, GoldfishSimulator.DeckStats stats, java.util.List<Integer> winTurns, java.util.List<String> columns, GoldfishSimulator.Start start) {
        java.util.List<java.util.List<StatCell>> rows = new ArrayList<>();

        // one row per mulligans taken
        stats.getMulligans().forEach(mulligansTaken -> {
            if(stats.getPercentage(withStart(start), withMulligans(mulligansTaken, false)).getPercentage() > 1d) {
                rows.add(createStatsRow(stats, mulligansTaken, start, winTurns));
            }
        });

        // last row is global
        rows.add(createStatsRow(stats, -1, start, winTurns));

        // update table
        table.setModel(new AbstractTableModel() {
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
        for (int column = 0; column < table.getColumnCount(); column++) {
            TableColumn tableColumn = table.getColumnModel().getColumn(column);
            if (column <= 1) {
                tableColumn.setPreferredWidth(100);
            } else {
                tableColumn.setPreferredWidth(50);
            }
        }
    }

    private java.util.List<StatCell> createStatsRow(GoldfishSimulator.DeckStats stats, int mulligans, GoldfishSimulator.Start start, java.util.List<Integer> winTurns) {
        List<StatCell> row = new ArrayList<>(winTurns.size() + 1);
        // mulligans
        row.add(new MulligansCell(stats, start, mulligans));

        // first column: avg win turn
        row.add(new AvgKillCell(stats, start, mulligans));

        // one column per win turn
        winTurns.forEach(turn -> {
            row.add(new KillTurnCell(stats, start, mulligans, turn));
        });
        return row;
    }

    class StatsTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            TableModel model = table.getModel();
            StatCell cell = (StatCell) model.getValueAt(row, column);
            String text = cell.toString(handler.cumulated());
            Component cmp = super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
            cell.applyStyle(cmp, handler.getReference());
            return cmp;
        }
    }

    interface StatCell {
        String toString(boolean cumulated);

        void applyStyle(Component cmp, GoldfishSimulator.DeckStats reference);
    }

    @Value
    private static class MulligansCell implements StatCell {
        final GoldfishSimulator.DeckStats stats;
        final GoldfishSimulator.Start start;
        final int mulligans;

        public GoldfishSimulator.DeckStats.Percentage value(GoldfishSimulator.DeckStats stats, boolean cumulated) {
            return stats.getPercentage(withStart(start), withMulligans(mulligans, cumulated));
        }

        @Override
        public String toString(boolean cumulated) {
            if (mulligans < 0) {
                return "GLOBAL";
            }
            return mulligans + " (" + value(stats, cumulated) + ")";
        }

        public String toString() {
            return toString(false);
        }

        @Override
        public void applyStyle(Component cmp, GoldfishSimulator.DeckStats reference) {
            // background
            if (mulligans < 0) {
                // last row
                cmp.setBackground(UI.SILVER);
            } else {
                cmp.setBackground(Color.WHITE);
            }
            // font
            cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));
            // foreground color (if compared to another ref)
            cmp.setForeground(Color.BLACK);
            if (reference != null && reference != stats) {
                GoldfishSimulator.DeckStats.Percentage thisCmp = value(stats, true);
                GoldfishSimulator.DeckStats.Percentage otherCmp = value(reference, true);
                double diff = thisCmp.getPercentage() - otherCmp.getPercentage();
                if (diff >= PERCENT_TRESH) {
                    // better
                    cmp.setForeground(UI.OLIVE);
                } else if (diff <= -PERCENT_TRESH) {
                    // worse
                    cmp.setForeground(UI.RED);
                }
            }
        }
    }

    @Value
    private static class AvgKillCell implements StatCell {
        final GoldfishSimulator.DeckStats stats;
        final GoldfishSimulator.Start start;
        final int mulligans;

        public GoldfishSimulator.DeckStats.Average value(GoldfishSimulator.DeckStats stats, boolean cumulated) {
            return stats.getAverageWinTurn(withStart(start).and(withMulligans(mulligans, cumulated)));
        }

        @Override
        public String toString(boolean cumulated) {
            return value(stats, cumulated).toString();
        }

        public String toString() {
            return toString(false);
        }

        @Override
        public void applyStyle(Component cmp, GoldfishSimulator.DeckStats reference) {
            // background
            if (mulligans < 0) {
                // last row
                cmp.setBackground(UI.SILVER);
            } else {
                cmp.setBackground(Color.WHITE);
            }
            // font
            cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));
            // foreground color (if compared to another ref)
            cmp.setForeground(Color.BLACK);
            if (reference != null && reference != stats) {
                GoldfishSimulator.DeckStats.Average thisCmp = value(stats, true);
                GoldfishSimulator.DeckStats.Average otherCmp = value(reference, true);
                double diff = thisCmp.getAverage() - otherCmp.getAverage();
                if (diff <= -AVG_TRESH) {
                    // better
                    cmp.setForeground(UI.OLIVE);
                } else if (diff >= AVG_TRESH) {
                    // worse
                    cmp.setForeground(UI.RED);
                }
            }
        }
    }

    @Value
    private static class KillTurnCell implements StatCell {
        final GoldfishSimulator.DeckStats stats;
        final GoldfishSimulator.Start start;
        final int mulligans;
        final int winTurn;

        public GoldfishSimulator.DeckStats.Percentage value(GoldfishSimulator.DeckStats stats, boolean cumulated) {
            return stats.getPercentage(withStart(start).and(withMulligans(mulligans, cumulated)), withEndTurn(winTurn, cumulated));
        }

        @Override
        public String toString(boolean cumulated) {
            return value(stats, cumulated).toString();
        }

        public String toString() {
            return toString(false);
        }

        @Override
        public void applyStyle(Component cmp, GoldfishSimulator.DeckStats reference) {
            // background
            if (mulligans < 0) {
                // last row
                cmp.setBackground(UI.SILVER);
            } else {
                cmp.setBackground(Color.WHITE);
            }
            // foreground color (if compared to another ref)
            cmp.setForeground(Color.BLACK);
            if (reference != null && reference != stats) {
                Percentage thisCmp = value(stats, true);
                Percentage otherCmp = value(reference, true);
                double diff = thisCmp.getPercentage() - otherCmp.getPercentage();
                if (diff >= PERCENT_TRESH) {
                    // better
                    cmp.setForeground(UI.OLIVE);
                } else if (diff <= -PERCENT_TRESH) {
                    // worse
                    cmp.setForeground(UI.RED);
                }
            }
        }
    }

    private static boolean moreThanOnePercent(long count, int total) {
        return count * 100 / total > 1;
    }

}
