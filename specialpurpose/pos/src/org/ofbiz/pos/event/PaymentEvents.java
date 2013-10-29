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

import net.xoetrope.swing.XEdit;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.pos.PosTransaction;
import org.ofbiz.pos.component.ClientInput;
import org.ofbiz.pos.component.FacilityInput;
import org.ofbiz.pos.component.GiftCardInput;
import org.ofbiz.pos.component.Input;
import org.ofbiz.pos.component.Journal;
import org.ofbiz.pos.screen.PosScreen;
import org.ofbiz.base.util.UtilProperties;

import java.awt.Desktop;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;

public class PaymentEvents {


    public static final String module = PaymentEvents.class.getName();
    
    

    public static synchronized void payCash(PosScreen pos) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        trans.clearPayment("CASH");

        // all cash transactions are NO_PAYMENT; no need to check
        try {
            BigDecimal amount = processAmount(trans, pos, null);
            Debug.logInfo("Processing [Cash] Amount : " + amount, module);

            // add the payment
            trans.addPayment("CASH", amount, null, null);
        } catch (GeneralException e) {
            // errors handled
        }
        clearInputPaymentFunctions(pos);
        pos.refresh();
      getClientInput(pos);
    }
    
    public static ClientInput getClientInput(PosScreen pos) {
    	ClientInput clInput = new ClientInput(pos);
    	clInput.setClInput(true);
    	return clInput;
    }
    public static GiftCardInput getGCInput(PosScreen pos) {
    	GiftCardInput gcInput = new GiftCardInput(pos);
    	gcInput.setGCInput(true);
    	return gcInput;
    	
    }


    public static synchronized void payCheck(PosScreen pos) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        Input input = pos.getInput();
        String[] ckInfo = input.getFunction("CHECK");
//
////        // check for no/external payment processing
////        int paymentCheck = trans.checkPaymentMethodType("PERSONAL_CHECK");
////        if (paymentCheck == PosTransaction.NO_PAYMENT) {
////            trans.clearPayment("PERSONAL_CHECK");
////            processNoPayment(pos, "PERSONAL_CHECK");
////            return;
////        } else if (paymentCheck == PosTransaction.EXTERNAL_PAYMENT) {
////            if (ckInfo == null) {
////                input.setFunction("CHECK");
////                pos.getOutput().print(UtilProperties.getMessage(PosTransaction.resource,"PosRefNum",Locale.getDefault()));
////                return;
////            } else {
////                processExternalPayment(pos, "PERSONAL_CHECK", ckInfo[1]);
////                return;
////            }
////        }
////
////        // now for internal payment processing
////        pos.showDialog("dialog/error/notyetsupported");
    }

    public static synchronized void payGiftCard(PosScreen pos) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        Input input = pos.getInput();
        String[] gcInfo = input.getFunction("GIFTCARD");
    	String giftData = input.value();
        getGCInput(pos);
        GiftCardInput gcInput = new GiftCardInput(pos);
        ArrayList<String> giftCardInfo  = gcInput.getGCInputs();
        String gcRefNo = giftCardInfo.get(0);
        String gcAmount = giftCardInfo.get(1);
		if (UtilValidate.isEmpty(gcRefNo) || (UtilValidate.isEmpty(gcAmount))) {
       	 	return;
        }
		BigDecimal amount = new BigDecimal(gcAmount);
		if (giftCardInfo != null) {
			trans.addPayment("GIFT_CARD", amount, gcRefNo, null);
	        clearInputPaymentFunctions(pos);
	        pos.refresh();
	        gcInput.setGCInput(false);
	        //getClientInput(pos);
		}
		if (trans.getTotalDue().compareTo(BigDecimal.ZERO) > 0) {
            pos.getOutput().print("Gift Card Applied. Pending Amount : "+trans.getTotalDue());
		} else {
		      getClientInput(pos);
		}
        // check for no/external payment processing
        
        clearInputPaymentFunctions(pos);
    }


    private static synchronized void processNoPayment(PosScreen pos, String paymentMethodTypeId) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());

        try {
            BigDecimal amount = processAmount(trans, pos, null);
            Debug.logInfo("Processing [" + paymentMethodTypeId + "] Amount : " + amount, module);

            // add the payment
            trans.addPayment(paymentMethodTypeId, amount, null, null);
        } catch (GeneralException e) {
            // errors handled
        }
        clearInputPaymentFunctions(pos);
        pos.refresh();
       //        XEdit inputs = (XEdit) pos.findComponent("pos_input");
//        inputs.setVisible(false);

    }
    
    
    private static synchronized void processExternalPayment(PosScreen pos, String paymentMethodTypeId, String amountStr) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        Input input = pos.getInput();
        String refNum = input.value();
        if (refNum == null) {
            pos.getOutput().print(UtilProperties.getMessage(PosTransaction.resource,"PosRefNum",Locale.getDefault()));
            return;
        }
        input.clearInput();

        try {
            BigDecimal amount = processAmount(trans, pos, amountStr);
            Debug.logInfo("Processing [" + paymentMethodTypeId + "] Amount : " + amount, module);

            // add the payment
            trans.addPayment(paymentMethodTypeId, amount, refNum, null);
        } catch (GeneralException e) {
            // errors handled
        }
        clearInputPaymentFunctions(pos);
        pos.refresh();
        getClientInput(pos);

//        XEdit inputs = (XEdit) pos.findComponent("pos_input");
//        inputs.setVisible(false);

    }

    public static synchronized void clearPayment(PosScreen pos) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        Journal journal = pos.getJournal();
        String sku = journal.getSelectedSku();
        String idx = journal.getSelectedIdx();
        if (UtilValidate.isNotEmpty(idx) && UtilValidate.isEmpty(sku)) {
            int index = -1;
            try {
                index = Integer.parseInt(idx);
            } catch (Exception e) {
            }
            if (index > -1) {
                trans.clearPayment(index);
            }
        }
        clearInputPaymentFunctions(pos);
        pos.refresh();
    }

    public static synchronized void clearAllPayments(PosScreen pos) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        trans.clearPayments();
        clearInputPaymentFunctions(pos);
        pos.refresh();
    }

    public static synchronized void setRefNum(PosScreen pos) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        Journal journal = pos.getJournal();
        String sku = journal.getSelectedSku();
        String idx = journal.getSelectedIdx();

        if (UtilValidate.isNotEmpty(idx) && UtilValidate.isEmpty(sku)) {
            String refNum = pos.getInput().value();
            if (UtilValidate.isEmpty(refNum)) {
                pos.getOutput().print(UtilProperties.getMessage(PosTransaction.resource,"PosRefNum",Locale.getDefault()));
                pos.getInput().setFunction("REFNUM");
            } else {
                int index = -1;
                try {
                    index = Integer.parseInt(idx);
                } catch (Exception e) {
                }
                if (index > -1) {
                    trans.setPaymentRefNum(index, refNum, refNum);
                    pos.refresh();
                }
            }
        } else {
            pos.refresh();
        }
        clearInputPaymentFunctions(pos);
    }

    public static synchronized void processSale(PosScreen pos) {
        PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
        ClientInput clInput = new ClientInput(pos);
		final FacilityInput flInput = pos.getFacilityInput();
		String facilityId = flInput.getSelectedFacility();
		final BigDecimal quantity = flInput.getReserveQuantity();
        //ClientInput clinput = pos.getClientInput();
        //ClientInput clinput = new ClientInput(pos)
       // String[] clientInfo = input.getFunction("CLIENT_INFO");
         ArrayList<String> clientInfo  = clInput.getClientInputs();
         String custName = clientInfo.get(0);
         String custPhone = clientInfo.get(1);
         String salesRep = clientInfo.get(2);
		if (UtilValidate.isEmpty(custName) || (UtilValidate.isEmpty(custPhone))) 
         {
        	 pos.showDialog("dialog/error/exception", "PLEASE ENTER CUSTOMER NAME AND PHONE NUMBER");
        	 return;
         }
         trans.createClientProfile(custName, custPhone,salesRep, pos);
        Locale defaultLocale = Locale.getDefault();
        if (trans.isEmpty()) {
            PosScreen newPos = pos.showPage("pospanel");
            newPos.showDialog("dialog/error/noitems");
        } else if (trans.getTotalDue().compareTo(BigDecimal.ZERO) > 0) {
            pos.showDialog("dialog/error/exception", UtilProperties.getMessage("Xuilabels", "NOT_ENOUGH_FUNDS", defaultLocale));
            trans.clearPayment("CASH");
        } else {
            // manual locks (not secured; will be unlocked on clear)
            pos.setWaitCursor();
            PosScreen.currentScreen.getOutput().print(UtilProperties.getMessage(PosTransaction.resource, "PosProcessing", defaultLocale));
            pos.getInput().setLock(true);
            pos.getButtons().setLock(true);
            pos.refresh(false);

            // process the sale
            try {
                trans.processSale(pos.getOutput());
                pos.getInput().setFunction("PAID");
        		trans.reserveNonMarketingProducts();
                String result = trans.reserveMarkettingPackage();
                if ("error".equals(result)) {
                    pos.showDialog("dialog/error/exception", "NOT ENOUGH STOCK TO FULLFILL FURTHER ORDERS");
                }
                trans.setExchangeOfferInfo();
                trans.removeQuoteDetails();
                //PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
                String invoiceURL = "https://electromann:8443/accounting/control/invoice.pdf?invoiceId=";
            	String invoiceId = trans.getPOSInvoiceId();
            	// try {
            	// if(Desktop.isDesktopSupported())
            	// {
            	  // Desktop.getDesktop().browse(new URI(invoiceURL+invoiceId));
            	// }
            	// }catch(Exception ex) {
            		// Debug.logError(ex, module);
            	// }
            } catch (GeneralException e) {
                Debug.logError(e, e.getMessage(), module);
                pos.getInput().setLock(false);
                pos.getButtons().setLock(false);
                pos.showDialog("dialog/error/exception", e.getMessage());
            }
            clearInputPaymentFunctions(pos);
            pos.setNormalCursor();
        }
    }

    private static synchronized BigDecimal processAmount(PosTransaction trans, PosScreen pos, String amountStr) throws GeneralException {
        Input input = pos.getInput();

        if (input.isFunctionSet("TOTAL")) {
            String amtStr = amountStr != null ? amountStr : input.value();
            BigDecimal amount;
            if (UtilValidate.isNotEmpty(amtStr)) {
                try {
                    amount = new BigDecimal(amtStr);
                } catch (NumberFormatException e) {
                    Debug.logError("Invalid number for amount : " + amtStr, module);
                    pos.getOutput().print("Invalid Amount!");
                    input.clearInput();
                    throw new GeneralException();
                }
                //amount = amount.movePointLeft(2); // convert to decimal
                Debug.logInfo("Set amount / 100 : " + amount, module);
            } else {
                Debug.logInfo("Amount is empty; assumption is full amount : " + trans.getTotalDue(), module);
                amount = trans.getTotalDue();
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new GeneralException();
                }
            }
            return amount;
        } else {
            Debug.logInfo("TOTAL function NOT set", module);
            throw new GeneralException();
        }
    }

    // Removes all payment functions from the input function stack
    // Useful for clearing redundant data after a payment has been
    // processed or if an error occurred
    public static synchronized void clearInputPaymentFunctions(PosScreen pos) {
        String[] paymentFuncs = {"CHECK", "CHECKINFO", "CREDIT",
                                    "GIFTCARD", "MSRINFO", "REFNUM", "CREDITEXP",
                                    "TRACK2", "SECURITYCODE", "POSTALCODE" };
        Input input = pos.getInput();
        for (int i = 0; i < paymentFuncs.length; i++) {
            while (input.isFunctionSet(paymentFuncs[i])) {
                input.clearFunction(paymentFuncs[i]);
            }
        }
    }

	 public static synchronized void payByCreditCard(PosScreen pos) {
    	PosTransaction trans = PosTransaction.getCurrentTx(pos.getSession());
    	Input input = pos.getInput();
    	String[] totalInfo = input.getFunction("TOTAL");
    	String[] crtInfo = input.getFunction("CREDIT");
    	String msrData = input.value();
        String refNum = msrData.substring(2, 18);

    	int paymentCheck = PosTransaction.EXTERNAL_PAYMENT;
    	if (paymentCheck != PosTransaction.EXTERNAL_PAYMENT) {
    		processNoPayment(pos, "CREDIT_CARD");
    		return;
    	} else if (paymentCheck == PosTransaction.EXTERNAL_PAYMENT) {
    		BigDecimal amount = BigDecimal.ZERO;
    		try {
    			if (UtilValidate.isEmpty(refNum)) {
    				pos.getOutput().print(UtilProperties.getMessage(PosTransaction.resource,"PosCredNo",Locale.getDefault()));
    				return;
    			} else {
    				amount = processAmount(trans, pos, totalInfo[1]);
    				Debug.logInfo("Processing Credit Card Amount : " + amount, module);
    				trans.addPayment("CREDIT_CARD", amount, refNum, null);
    		        clearInputPaymentFunctions(pos);
    		        pos.refresh();
    		        getClientInput(pos);

    			}
    		} catch (GeneralException e) {
    			Debug.logError("Exception caught calling processAmount.", module);
    			Debug.logError(e.getMessage(), module);
    		}
    		return;
    	}

    }
}
