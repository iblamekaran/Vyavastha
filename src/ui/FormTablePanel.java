package ui; 

import exception.AllocationException; 
import exception.ValidationException; 

import javax.swing.BorderFactory; 
import javax.swing.JButton; 
import javax.swing.JComponent; 
import javax.swing.JLabel; 
import javax.swing.JOptionPane; 
import javax.swing.JPanel; 
import javax.swing.JScrollPane; 
import javax.swing.JTable; 
import javax.swing.table.DefaultTableModel; 
import java.awt.BorderLayout; 
import java.awt.Component; 
import java.awt.FlowLayout; 
import java.awt.GridLayout; 

public abstract class FormTablePanel extends JPanel { 
    protected final DefaultTableModel model; 
    protected final JTable table; 
    private final JPanel form = new JPanel(new GridLayout(0, 2, 6, 6)); 

    protected FormTablePanel(String[] columns) { 
        super(new BorderLayout(8, 8)); 
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); 
        model = new DefaultTableModel(columns, 0) { 
            @Override 
            public boolean isCellEditable(int row, int col) { return false; } 
        }; 
        table = new JTable(model); 
        JButton addButton = new JButton("Add"); 
        JButton deleteButton = new JButton("Delete selected"); 
        JButton refreshButton = new JButton("Refresh"); 

        addButton.addActionListener(e -> run(this::onAdd)); 
        deleteButton.addActionListener(e -> { 
            int row = table.getSelectedRow(); 
            if (row < 0) { 
                JOptionPane.showMessageDialog(this, "Select a row first."); 
                return; 
            } 
            run(() -> onDelete(row)); 
        }); 
        refreshButton.addActionListener(e -> run(this::loadData)); 

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT)); 
        buttons.add(addButton); 
        buttons.add(deleteButton); 
        buttons.add(refreshButton); 

        JPanel formWrap = new JPanel(new FlowLayout(FlowLayout.LEFT)); 
        formWrap.add(form); 
        JPanel north = new JPanel(new BorderLayout()); 
        north.add(formWrap, BorderLayout.CENTER); 
        north.add(buttons, BorderLayout.SOUTH); 
        add(north, BorderLayout.NORTH); 
        add(new JScrollPane(table), BorderLayout.CENTER); 
    } 

    protected void addField(String label, JComponent input) { 
        form.add(new JLabel(label)); 
        form.add(input); 
    } 

    // each panel fills these in 
    protected abstract void onAdd(); 
    protected abstract void onDelete(int tableRow); 
    protected abstract void loadData(); 

    // runs an action and shows any error as a dialog instead of crashing 
    protected void run(Runnable action) { 
        try { 
            action.run(); 
        } catch (RuntimeException ex) { 
            report(this, ex); 
        } 
    } 

    public static void report(Component parent, RuntimeException ex) { 
        if (ex instanceof ValidationException || ex instanceof AllocationException) { 
            JOptionPane.showMessageDialog(parent, ex.getMessage(), "Please check", 
                JOptionPane.WARNING_MESSAGE); 
        } else { 
            JOptionPane.showMessageDialog(parent, ex.getMessage(), "Error", 
                JOptionPane.ERROR_MESSAGE); 
        } 
    } 
}
