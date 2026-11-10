package ui; 

import javax.swing.JFrame; 
import javax.swing.JTabbedPane; 

public class DashboardFrame extends JFrame { 
    public DashboardFrame() { 
        super("Vyavastha - Exam Seating & Invigilation Management"); 
        setDefaultCloseOperation(EXIT_ON_CLOSE); 
        setSize(950, 620); 
        setLocationRelativeTo(null); 
        JTabbedPane tabs = new JTabbedPane(); 
        tabs.addTab("Students", new StudentPanel()); 
        tabs.addTab("Rooms", new RoomPanel()); 
        tabs.addTab("Exams", new ExamPanel()); 
        tabs.addTab("Invigilators", new InvigilatorPanel()); 
        tabs.addTab("Allocation", new AllocationPanel()); 
        tabs.addTab("Reports", new ReportPanel()); 
        add(tabs); 
    } 
}
