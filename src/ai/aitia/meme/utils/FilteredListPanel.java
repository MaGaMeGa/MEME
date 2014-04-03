package ai.aitia.meme.utils;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

public abstract class FilteredListPanel extends JPanel {
	
	JScrollPane elementListPane;
	JList<String> elementList = new JList<String>();
	JTextField elementFilterField = new JTextField();

	public FilteredListPanel(Object ... params) 
	{
		final ArrayList<String> elements;
		final DefaultListModel<String> listModel = new DefaultListModel<String>();

		
		elementList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		elementList.setModel(listModel);
		
		elements = formatElements(params);
		for(String element : elements)listModel.addElement(element);

		setLayout(new BorderLayout());
		elementFilterField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				listModel.clear();
				for (String element : elements)
				{
		            if(element.toUpperCase().contains(elementFilterField.getText().toUpperCase()))listModel.addElement(element);
		        }
			}
		});
		add(new JLabel("Please select an element:"), BorderLayout.LINE_START);
		add(elementFilterField, BorderLayout.PAGE_START);
        
        elementList = new JList<String>(listModel);
        elementListPane = new JScrollPane(elementList);
        add(elementListPane, BorderLayout.CENTER);
        
	}
	
	public Object getSelectedType() {
		return elementList.getSelectedValue();
	}
	
	protected abstract ArrayList<String> formatElements(Object ... params);
}
