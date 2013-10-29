/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.pos.event;

import java.math.BigDecimal;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.pos.PosTransaction;
import org.ofbiz.pos.component.ClientInput;
import org.ofbiz.pos.component.FacilityInput;
import org.ofbiz.pos.component.GiftCardInput;
import org.ofbiz.pos.component.QuoteInput;
import org.ofbiz.pos.screen.PosScreen;

public class NavagationEvents {
	public static final String module = NavagationEvents.class.getName();
	
	public static synchronized void showPosScreen(final PosScreen pos) {
		ManagerEvents.mgrLoggedIn = false;
		pos.showPage("pospanel");
	}
	
	public static synchronized void showPayScreen(final PosScreen pos) {
		ManagerEvents.mgrLoggedIn = false;
		final PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
		if (trans.isEmpty()) {
			pos.showDialog("dialog/error/noitems");
		} else {
			final FacilityInput flInput = pos.getFacilityInput();
			final QuoteInput qinput = pos.getQuoteInput();
			String facilityId = flInput.getSelectedFacility();
			String quoteId = qinput.getQuoteId().getText();
			if ("error".equals(facilityId) && UtilValidate.isNotEmpty(quoteId)) {
				facilityId = "quote_order";
				trans.reserveQuoteProducts(quoteId);
				final PosScreen newPos = pos.showPage("paypanel");
				newPos.getInput().setFunction("TOTAL");
				newPos.refresh();
				getClientInput(newPos);
				getGCInput(newPos);

			} else {
				trans.getProductsFacility(pos);
//				final String productId = flInput.getfacilityProductId();
//				//if (!UtilProperties.getPropertyValue("general.properties", "electroman.consignee.facility").equals(facilityId)) {
//					String consigneeFacility = UtilProperties.getPropertyValue("general.properties", "electroman.consignee.facility");
//					final String inventoryStatus = trans.checkProductAvailability(productId, facilityId);
//					if (UtilValidate.isEmpty(productId)) {
//						pos.showDialog("dialog/error/exception", "CART IS EMPTY");
//						pos.showPage("pospanel");
//					}else if ("error".equals(facilityId)) {
//						pos.showDialog("dialog/error/exception", "SELECT A STOCK LOCATION");
//						pos.showPage("pospanel");
//					} else if (("NOT_AVAILABLE".equals(inventoryStatus))&& (!consigneeFacility.equals(facilityId))) {
//						pos.showDialog("dialog/error/exception", "NOT ENOUGH STOCK IN THE SELECTED LOCATION.");
//						pos.showPage("pospanel");
//						
//					} else if (("NOT_AVAILABLE".equals(inventoryStatus))&& (consigneeFacility.equals(facilityId))) {
//						pos.showDialog("dialog/error/exception", "CONSIGNEE INVENTORY IS NOT ASSIGNED FOR THIS PRODUCT.");
//						pos.showPage("pospanel");
//					}
//						else {
//					final BigDecimal quantity = flInput.getReserveQuantity();
//						//trans.setCurrentProductFacility(facilityId); ||
//						if (UtilValidate.isNotEmpty(facilityId) && (!"error".equals(facilityId))) {
//							trans.setReservationDetails(facilityId, productId, quantity);
//							Debug.logInfo("@@@@@@@@@@@@@@@@@@@@@@@@@@reserving " + quantity + "from the "+facilityId, module);
//						}
						flInput.clearFlInput();
						facilityId = "error";
						final PosScreen newPos = pos.showPage("paypanel");
						newPos.getInput().setFunction("TOTAL");
						newPos.refresh();
						getClientInput(newPos);
						getGCInput(newPos);
						flInput.setFlInput(false);
					}
				}
			}
		//}
	//}
	
	public static synchronized void showPromoScreen(final PosScreen pos) {
		final PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
		ManagerEvents.mgrLoggedIn = false;
		final FacilityInput flInput = pos.getFacilityInput();
		final QuoteInput qinput = pos.getQuoteInput();
		flInput.clearFlInput();
		qinput.clearQuoteInput();
		flInput.setFlInput(false);
		qinput.setQuoteInput(true);
		pos.showPage("pospanel");
		pos.getInput().setFunction("QUOTE_ADD");
		String quoteId = qinput.getQuoteId().getText();
		trans.loadCartFromQuote(quoteId);
	}
	public static synchronized void showQuoteScreen(final PosScreen pos) {
		ManagerEvents.mgrLoggedIn = false;
		pos.showPage("quotepanel");
	}
	
	public static ClientInput getClientInput(final PosScreen pos) {
		final ClientInput clInput = new ClientInput(pos);
		clInput.setClInput(false);
		return clInput;
	}
	public static GiftCardInput getGCInput(PosScreen pos) {
    	GiftCardInput gcInput = new GiftCardInput(pos);
    	gcInput.setGCInput(false);
    	return gcInput;
    	
    }
	
}