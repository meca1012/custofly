package com.example.window;

import com.vaadin.data.Property;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Field;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class MySubWindow extends Window {
    
	final TextField tf = new TextField();
	
	private String result = null;

	public MySubWindow(String value) {
        super("Edit value: "); // Set window caption
        center();
       
        // Some basic content for the window
        VerticalLayout content = new VerticalLayout();
        
        tf.setCaption("Test: ");
        tf.setValue(value);
        tf.setImmediate(true);
        content.addComponent(tf);
        
        content.addComponent(new Label("Just edit, it's OK!"));
        content.setMargin(true);
        setContent(content);
        
        // Disable the close button
        setClosable(false);

        // Trivial logic for closing the sub-window
        Button ok = new Button("OK");
        ok.addClickListener(new ClickListener() {  	
			@Override
			public void buttonClick(ClickEvent event) {
	        	setResult(tf.getValue());
	        	// Close the sub-window
	            close();
			} 
        });    
        content.addComponent(ok);
    }	
	
	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
		
	}
	
}