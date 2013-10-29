package org.ofbiz.pos.component;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.ofbiz.pos.screen.PosScreen;

import net.xoetrope.swing.XEdit;
import net.xoetrope.swing.XLabel;
import net.xoetrope.swing.XButton;
import net.xoetrope.swing.XPanel;

public class QuoteInput {
	public static final String module = FacilityInput.class.getName();
		protected JTextField quoteId;
		protected JLabel quoteLabel;
		protected JButton createOrder;
		protected JPanel quotePanel;
		
		public QuoteInput(final PosScreen pos) {
			quoteLabel = (XLabel) pos.findComponent("quoteIdLabel");
			quoteId = (XEdit) pos.findComponent("quoteId");
			createOrder = (XButton) pos.findComponent("createOrder");
			quotePanel = (XPanel) pos.findComponent("quotePanel");
		}
		public void setQuoteInput(boolean flag){
			quoteId.setVisible(flag);
			quoteLabel.setVisible(flag);
			createOrder.setVisible(flag);
			quotePanel.setVisible(flag);
		}
		public void clearQuoteInput () {
			quoteId.setText("");
		}
		public JTextField getQuoteId() {
			return quoteId;
		}
		public void setQuoteId(JTextField quoteId) {
			this.quoteId = quoteId;
		}
		
}