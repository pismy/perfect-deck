package org.mtgpeasant.perfectdeck.gui;

import com.google.common.net.UrlEscapers;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.mtgpeasant.decks.PilotScanner;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;

public class PilotInfoDialog extends JDialog {

    public PilotInfoDialog(Window owner, PilotScanner.PilotMetadata description) {
        super(owner, "[" + description.getName() + "] Info");

        final JEditorPane htmlViewer = new JEditorPane("text/html", "");
        htmlViewer.setEditable(false);
        htmlViewer.setAutoscrolls(true);
        htmlViewer.setMargin(new Insets(5, 5, 5, 5));

        HTMLEditorKit kit = new HTMLEditorKit();
        htmlViewer.setEditorKit(kit);

        StyleSheet styleSheet = kit.getStyleSheet();
        styleSheet.addRule("body{font-family:Verdana,sans-serif; font-size:12pt; margin: 5px; color: #111111;}");
        styleSheet.addRule("h1{font-size:18pt;color:#3D9970;}");
        styleSheet.addRule("h2{font-size:15pt;color:#3D9970;}");
        styleSheet.addRule("h2{font-size:13pt;color:#3D9970;}");
        styleSheet.addRule("a{color:#0074D9;}");
        styleSheet.addRule("ul{margin:0 0 0 20px; padding:0}");

        htmlViewer.setText(toHtml(description));

        htmlViewer.addHyperlinkListener(e -> onHyperlink(e));

        add(UI.createScrollPane(htmlViewer, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED));
    }

    private void onHyperlink(HyperlinkEvent evt) {
        if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            // open in system browser
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(evt.getURL().toURI());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String toHtml(PilotScanner.PilotMetadata description) {
        StringBuilder html = new StringBuilder();
        // 1: description
        if (description.getDescription() != null) {
            Parser parser = Parser.builder().build();
            Node document = parser.parse(description.getDescription());
            HtmlRenderer renderer = HtmlRenderer.builder().build();
            html.append(renderer.render(document));
        } else {
            html.append("<h1>").append(description.getName()).append("</h1><br/>");
        }
        // 2: managed cards
        if (description.getManagedCards() != null && !description.getManagedCards().isEmpty()) {
            html.append("<h2>Managed Cards (" + description.getManagedCards().size() + ")</h2>");
            html.append("<ul>");
            description.getManagedCards().forEach(card -> html.append("<li><a href=\"" + "https://scryfall.com/search?q=" + UrlEscapers.urlFragmentEscaper().escape(card) + "\">" + card + "</li>"));
            html.append("</ul>");
        }
        return html.toString();
    }
}
