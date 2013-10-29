package org.ofbiz.pos.component;

import java.util.ArrayList;

import net.xoetrope.swing.XEdit;
import net.xoetrope.swing.XLabel;

import org.ofbiz.pos.screen.PosScreen;

public class GiftCardInput {

    protected XLabel gcRefNoLabel = null;
    protected XLabel gcAmountLabel = null;
    protected javax.swing.JTextField gcRefNoEdit = null;
    protected javax.swing.JTextField gcAmountEdit = null;

	public GiftCardInput(PosScreen page) {
		this.gcRefNoLabel = (XLabel) page.findComponent("gcRefNoLabel");
		this.gcAmountLabel = (XLabel) page.findComponent("gcAmountLabel");
        this.gcRefNoEdit = (XEdit) page.findComponent("gcRefNoEdit");
        this.gcAmountEdit = (XEdit) page.findComponent("gcAmountEdit");
	}
	
	public void setGCInput(boolean flag) {
			gcRefNoLabel.setVisible(flag);
			gcAmountLabel.setVisible(flag);
			gcRefNoEdit.setVisible(flag);
			gcAmountEdit.setVisible(flag);
	}
	public  ArrayList<String> getGCInputs() {
		String refNo = this.gcRefNoEdit.getText().trim();      
        String amount = this.gcAmountEdit.getText().trim();
		ArrayList<String> inputs = new ArrayList<String>();
		inputs.add(refNo);
		inputs.add(amount);
		gcRefNoEdit.setText("");
		gcAmountEdit.setText("");
		return inputs;		
	}
}
