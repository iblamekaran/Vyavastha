import api.ApiServer; 
import ui.DashboardFrame; 

import javax.swing.SwingUtilities; 

public class Main { 
    public static void main(String[] args) { 
        // Automatically launch the REST API server on port 8080 in background
        // so the React Web UI connects seamlessly
        try { 
            ApiServer server = new ApiServer(8080); 
            server.start(); 
        } catch (Exception e) { 
            System.out.println("Notice: ApiServer on port 8080 could not start (it may already be running): " + e.getMessage()); 
        } 

        SwingUtilities.invokeLater(() -> new DashboardFrame().setVisible(true)); 
    } 
}
