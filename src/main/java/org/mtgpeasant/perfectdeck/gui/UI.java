package org.mtgpeasant.perfectdeck.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class UI {
    static final Color BLACK = new Color(0x111111);
    static final Color BLUE = new Color(0x0074D9);
    static final Color RED = new Color(0xFF4136);
    static final Color GRAY = new Color(0xAAAAAA);
    static final Color NAVY = new Color(0x001f3f);
    static final Color TEAL = new Color(0x39CCCC);
    static final Color OLIVE = new Color(0x3D9970);
    static final Color GREEN = new Color(0x2ECC40);
    static final Color LIME = new Color(0x01FF70);
    static final Color YELLOW = new Color(0xFFDC00);
    static final Color ORANGE = new Color(0xFF851B);
    static final Color SILVER = new Color(0xDDDDDD);
    static final Color MAROON = new Color(0x85144b);
    static final Color FUCHSIA = new Color(0xF012BE);
    static final Color PURPLE = new Color(0xB10DC9);
    static final Color AQUA = new Color(0x7FDBFF);

    static JScrollPane createScrollPane(Component content, int hPolicy, int vPolicy) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setHorizontalScrollBarPolicy(hPolicy);
        scrollPane.setVerticalScrollBarPolicy(vPolicy);
        return scrollPane;
    }

    static JTextField createIntegerInput(String text, int columns) {
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

    static JPanel createFlowPanel(int align, Component... components) {
        JPanel panel = new JPanel(new FlowLayout(align));
        for (Component c : components) {
            panel.add(c);
        }
        return panel;
    }
}
