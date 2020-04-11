package org.mtgpeasant.perfectdeck.gui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameLogsDialog extends JDialog {
    private static Pattern TITLE_LINE = Pattern.compile("^===.*$", Pattern.MULTILINE);
    private static Pattern START_TURN = Pattern.compile("^> .*$", Pattern.MULTILINE);
    private static Pattern BEGIN_PHASE = Pattern.compile("^beg>", Pattern.MULTILINE);
    private static Pattern MAIN_PHASE = Pattern.compile("^m#[12]>", Pattern.MULTILINE);
    private static Pattern COMBAT_PHASE = Pattern.compile("^cmb>", Pattern.MULTILINE);
    private static Pattern END_PHASE = Pattern.compile("^end>", Pattern.MULTILINE);

    private static final AttributeSet TITLE_STYLE;
    private static final AttributeSet START_STYLE;
    private static final AttributeSet BEGIN_STYLE;
    private static final AttributeSet MAIN_STYLE;
    private static final AttributeSet COMBAT_STYLE;
    private static final AttributeSet END_STYLE;

    static {
        // create styles for editor
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet bold = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Bold, true);
        TITLE_STYLE = sc.addAttribute(bold, StyleConstants.Foreground, UI.BLACK);
        START_STYLE = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.GRAY);
        BEGIN_STYLE = sc.addAttribute(bold, StyleConstants.Foreground, UI.OLIVE);
        MAIN_STYLE = sc.addAttribute(bold, StyleConstants.Foreground, UI.BLUE);
        COMBAT_STYLE = sc.addAttribute(bold, StyleConstants.Foreground, UI.RED);
        END_STYLE = sc.addAttribute(bold, StyleConstants.Foreground, UI.OLIVE);
    }

    private final JTextPane gameLogs;

    public GameLogsDialog(Window owner) {
        super(owner, "Game logs");
        // TODO: colorize game logs
        gameLogs = new JTextPane();
        gameLogs.setAutoscrolls(true);
        gameLogs.setEditable(false);
        gameLogs.setFont(new Font("monospaced", Font.PLAIN, 14));
        gameLogs.setMargin(new Insets(5, 5, 5, 5));

        add(UI.createScrollPane(gameLogs, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED));
    }

    public void setLogs(String logs) {
        gameLogs.setText(logs);
        colorize();
    }

    private void colorize() {
//        StyledDocument doc = gameLogs.getStyledDocument();
//        doc.setCharacterAttributes(0, doc.getLength(), CARD_STYLE, false);

        apply(TITLE_LINE, TITLE_STYLE);
        apply(START_TURN, START_STYLE);
        apply(BEGIN_PHASE, BEGIN_STYLE);
        apply(MAIN_PHASE, MAIN_STYLE);
        apply(COMBAT_PHASE, COMBAT_STYLE);
        apply(END_PHASE, END_STYLE);
    }

    private void apply(Pattern pattern, AttributeSet style) {
        Matcher matcher = pattern.matcher(gameLogs.getText());
        StyledDocument doc = gameLogs.getStyledDocument();
        while (matcher.find()) {
            doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), style, false);
        }
    }

}
