package com.library.ui;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

/**
 * The application shell. One tab per feature area; each tab gets its own panel
 * class in this package as the features are built.
 *
 * <p>This class wires widgets to services and nothing else — no validation, no
 * SQL, no business rules.
 */
public class MainWindow extends JFrame {

    public MainWindow() {
        super("Library Management System");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Catalog", placeholder("Catalog"));
        tabs.addTab("Members", placeholder("Members"));
        tabs.addTab("Circulation", placeholder("Circulation"));
        add(tabs, BorderLayout.CENTER);
    }

    private static JPanel placeholder(String area) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(area + " — not built yet", SwingConstants.CENTER), BorderLayout.CENTER);
        return panel;
    }
}
