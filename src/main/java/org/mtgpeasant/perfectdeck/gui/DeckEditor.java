package org.mtgpeasant.perfectdeck.gui;

import org.mtgpeasant.perfectdeck.common.cards.Deck;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.io.StringReader;

/**
 * Deck list editor + bottom bar (summary)
 *
 * <pre>
 * +---------------------------------------+
 * | (deck editor)                         |
 * |                                       |
 * |                                       |
 * |                                       |
 * |                                       |
 * |                                       |
 * +---------------------------------------+
 * | (summary)                             |
 * +---------------------------------------+
 * </pre>
 * TODO: auto-suggest (on CRTL+space)
 */
public class DeckEditor extends JPanel {
    private static final Color USB = new Color(0xFF8888);
    private static final AttributeSet COMMENT_STYLE;
    private static final AttributeSet SIDEBOARD_STYLE;
    private static final AttributeSet CARD_STYLE;
    private static final AttributeSet UNKNOWN_CARD_STYLE;
    private static final AttributeSet UNKNOWN_SIDEBOARD_STYLE;

    static {
        // create styles for editor
        StyleContext sc = StyleContext.getDefaultStyleContext();
        COMMENT_STYLE = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, UI.OLIVE);
        SIDEBOARD_STYLE = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.GRAY);
        CARD_STYLE = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.BLACK);
        UNKNOWN_CARD_STYLE = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, UI.RED);
        UNKNOWN_SIDEBOARD_STYLE = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, USB);
    }

    private final GuiOptionsHandler handler;
    private final JTextPane editor;
    private final JLabel summary;

    public DeckEditor(GuiOptionsHandler handler) {
        this.handler = handler;

        summary = new JLabel("");
        summary.setForeground(Color.GRAY);

        editor = new JTextPane();
        editor.setText("// type in your deck here...\n");
        colorize();
        // TODO: timeout
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> colorize());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> colorize());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
//                SwingUtilities.invokeLater(() -> colorize(deckEditor));
            }
        });
        editor.setMinimumSize(new Dimension(400, 300));
//      editor.setCharacterAttributes();
        editor.setFont(new Font("monospaced", Font.BOLD, 14));
        editor.setAutoscrolls(true);

        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, UI.createScrollPane(editor, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED));
        add(BorderLayout.SOUTH, UI.createFlowPanel(FlowLayout.LEFT, summary));
    }

    /**
     * Sets the deck list
     */
    void setText(String deck) {
        editor.setText(deck);
    }

    /**
     * Gets the deck list
     */
    String getText() {
        return editor.getText();
    }

    private void colorize() {
        try {
            String text = editor.getText();
            java.util.List<Deck.ParsedDeck.Line> lines = Deck.parseLines(new StringReader(text), handler.getManagedCards());
            StyledDocument doc = editor.getStyledDocument();
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
            summary.setText("Main: " + main + (side == 0 ? "" : " / Side: " + side));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
