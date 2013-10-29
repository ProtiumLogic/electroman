package org.ofbiz.pos.component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.xoetrope.swing.XEdit;
import net.xoetrope.swing.XLabel;
import net.xoetrope.swing.XRadioButton;
import net.xoetrope.swing.XPanel;


import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.pos.screen.PosScreen;

public class FacilityInput {
	public static final String module = FacilityInput.class.getName();

	
	protected XRadioButton showRoom, displayLocation, backStore, mainWareHouse, consignee;
	protected JTextField showRoomStock, displayLocationStock, backStoreStock, mainWareHouseStock, consigneeStock;
	protected JLabel facilityName, stockLevel;
	protected ArrayList<String> facilityList = new ArrayList<String>();
	protected String facilityProductId = null;
	protected BigDecimal reserveQuantity = null;
	protected JPanel facilityPanel;
	
	public FacilityInput(final PosScreen pos) {
		facilityName = (XLabel) pos.findComponent("facilityLabel");
		stockLevel = (XLabel) pos.findComponent("ATPLabel");
		showRoom = (XRadioButton) pos.findComponent("showRoom");
		displayLocation = (XRadioButton) pos.findComponent("displayLocation");
		backStore = (XRadioButton) pos.findComponent("backStore");
		mainWareHouse = (XRadioButton) pos.findComponent("mainWareHouse");
		consignee = (XRadioButton) pos.findComponent("consignee");
		showRoomStock = (XEdit) pos.findComponent("showRoomStock");
		displayLocationStock = (XEdit) pos.findComponent("displayLocationStock");
		backStoreStock = (XEdit) pos.findComponent("backStoreStock");
		mainWareHouseStock = (XEdit) pos.findComponent("mainWareHouseStock");
		consigneeStock = (XEdit) pos.findComponent("consigneeStock");
		facilityPanel = (XPanel)pos.findComponent("facilityPanel");
		
//		this.nextItem = (XButton) pos.findComponent("menuNextItem");
	}
	
	public void setFlInput(final boolean flag) {
		facilityName.setVisible(flag);
		stockLevel.setVisible(flag);
		showRoom.setVisible(flag);
		displayLocation.setVisible(flag);
		backStore.setVisible(flag);
		mainWareHouse.setVisible(flag);
		consignee.setVisible(flag);
		showRoomStock.setVisible(flag);
		displayLocationStock.setVisible(flag);
		backStoreStock.setVisible(flag);
		mainWareHouseStock.setVisible(flag);
		consigneeStock.setVisible(false);
		facilityPanel.setVisible(flag);
//		nextItem.setVisible(flag);
	}
	
	public void clearFlInput() {
		showRoom.setSelected(false);
		displayLocation.setSelected(false);
		backStore.setSelected(false);
		mainWareHouse.setSelected(false);
		consignee.setSelected(false);
		showRoomStock.setText("");
		displayLocationStock.setText("");
		backStoreStock.setText("");
		mainWareHouseStock.setText("");
		consigneeStock.setText("");
	}
	
	public ArrayList<String> getFacilityList() {
		facilityList.add(UtilProperties.getPropertyValue("general.properties", "electroman.showroom.facility"));
		facilityList.add(UtilProperties.getPropertyValue("general.properties", "electroman.displaylocation.facility"));
		facilityList.add(UtilProperties.getPropertyValue("general.properties", "electroman.backdoor.facility"));
		facilityList.add(UtilProperties.getPropertyValue("general.properties", "electroman.mainwarehouse.facility"));
		facilityList.add(UtilProperties.getPropertyValue("general.properties", "electroman.consignee.facility"));
		
		return facilityList;
	}
	
	public void setStockLevel(final Map result) {
		if (UtilValidate.isNotEmpty(result)) {
			showRoomStock.setText((String) result.get(UtilProperties.getPropertyValue("general.properties", "electroman.showroom.facility")));
			displayLocationStock.setText((String) result.get(UtilProperties.getPropertyValue("general.properties", "electroman.displaylocation.facility")));
			backStoreStock.setText((String) result.get(UtilProperties.getPropertyValue("general.properties", "electroman.backdoor.facility")));
			mainWareHouseStock.setText((String) result.get(UtilProperties.getPropertyValue("general.properties", "electroman.mainwarehouse.facility")));
			consigneeStock.setText((String) result.get(UtilProperties.getPropertyValue("general.properties", "electroman.consignee.facility")));
		}
	}
	
	public String getSelectedFacility() {
		if (showRoom.isSelected()) {
			return UtilProperties.getPropertyValue("general.properties", "electroman.showroom.facility");
		} else if (displayLocation.isSelected()) {
			return UtilProperties.getPropertyValue("general.properties", "electroman.displaylocation.facility");
		} else if (backStore.isSelected()) {
			return UtilProperties.getPropertyValue("general.properties", "electroman.backdoor.facility");
		} else if (mainWareHouse.isSelected()) {
			return UtilProperties.getPropertyValue("general.properties", "electroman.mainwarehouse.facility");
		} else if (consignee.isSelected()) {
			return UtilProperties.getPropertyValue("general.properties", "electroman.consignee.facility");
		} else {
			return "error";
		}
	}
	
	public void setfacilityProductId(final String productId) {
		facilityProductId = productId;
	}
	
	public String getfacilityProductId() {
		return facilityProductId;
	}
	
	public BigDecimal getReserveQuantity() {
		return reserveQuantity;
	}
	
	public void setReserveQuantity(final BigDecimal reserveQuantity) {
		this.reserveQuantity = reserveQuantity;
	}
	
}