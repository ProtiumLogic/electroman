package org.ofbiz.pos.component;

import java.util.ArrayList;

import javax.swing.JLabel;

import net.xoetrope.swing.XEdit;
import net.xoetrope.swing.XLabel;

import org.ofbiz.pos.screen.PosScreen;

public class ClientInput {
    protected XLabel nameLabel = null;
    protected XLabel phoneLabel = null;
    protected XLabel salesRepLabel = null;
    protected javax.swing.JTextField custName = null;
    protected javax.swing.JTextField custPhone = null;
    protected javax.swing.JTextField salesRepId = null;

	public ClientInput(PosScreen page) {
		this.nameLabel = (XLabel) page.findComponent("nameLabel");
		this.phoneLabel = (XLabel) page.findComponent("phoneLabel");
		this.salesRepLabel = (XLabel) page.findComponent("salesRepLabel");
        this.custName = (XEdit) page.findComponent("nameEdit");
        this.custPhone = (XEdit) page.findComponent("phoneEdit");
        this.salesRepId = (XEdit) page.findComponent("salesRepEdit");
	}
	
	public void setClInput(boolean flag) {
			nameLabel.setVisible(flag);
			phoneLabel.setVisible(flag);
			salesRepLabel.setVisible(flag);
			custName.setVisible(flag);
			custPhone.setVisible(flag);
			salesRepId.setVisible(flag);
	}
	public  ArrayList<String> getClientInputs() {
		String name = this.custName.getText().trim();      
        String phone = this.custPhone.getText().trim();
        String salesrepid = this.salesRepId.getText().trim();
		ArrayList<String> inputs = new ArrayList<String>();
		inputs.add(name);
		inputs.add(phone);
		inputs.add(salesrepid);
		custName.setText("");
		custPhone.setText("");
		salesRepId.setText("");
		return inputs;		
	}

}
