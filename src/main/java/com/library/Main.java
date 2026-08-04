package com.library;

import com.library.persistence.Database;
import com.library.ui.MainWindow;
import java.awt.GraphicsEnvironment;
import java.sql.SQLException;
import javax.swing.SwingUtilities;

/**
 * Application entry point: prepares the database, then opens the main window.
 */
public final class Main {

    public static void main(String[] args) {
        try {
            Database.initialize();
        } catch (SQLException e) {
            System.err.println("Could not open the library database: " + e.getMessage());
            System.exit(1);
        }

        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Database ready. No display available, so the window was not opened.");
            return;
        }

        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }

    private Main() {
    }
}
