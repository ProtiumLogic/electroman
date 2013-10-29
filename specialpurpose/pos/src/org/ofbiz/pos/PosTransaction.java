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
package org.ofbiz.pos;

import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.xoetrope.xui.data.XModel;
import net.xoetrope.xui.helper.SwingWorker;

import org.ofbiz.accounting.payment.PaymentGatewayServices;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.Log4jLoggerWriter;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.LifoSet;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.guiapp.xui.XuiSession;
import org.ofbiz.order.shoppingcart.CartItemModifyException;
import org.ofbiz.order.shoppingcart.CheckOutHelper;
import org.ofbiz.order.shoppingcart.ItemNotFoundException;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.ofbiz.order.shoppinglist.ShoppingListEvents;
import org.ofbiz.party.contact.ContactMechWorker;
import org.ofbiz.pos.component.FacilityInput;
import org.ofbiz.pos.component.Journal;
import org.ofbiz.pos.component.Output;
import org.ofbiz.pos.device.DeviceLoader;
import org.ofbiz.pos.device.impl.Receipt;
import org.ofbiz.pos.screen.ClientProfile;
import org.ofbiz.pos.screen.LoadSale;
import org.ofbiz.pos.screen.PosScreen;
import org.ofbiz.pos.screen.SaveSale;
import org.ofbiz.product.config.ProductConfigWrapper;
import org.ofbiz.product.config.ProductConfigWrapper.ConfigOption;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

@SuppressWarnings("serial")
public class PosTransaction implements Serializable {
	
	public static final int scale = UtilNumber.getBigDecimalScale("order.decimals");
	public static final int rounding = UtilNumber.getBigDecimalRoundingMode("order.rounding");
	public static final BigDecimal ZERO = (BigDecimal.ZERO).setScale(scale, rounding);
	
	public static final String resource = "PosUiLabels";
	public static final String module = PosTransaction.class.getName();
	public static final int NO_PAYMENT = 0;
	public static final int INTERNAL_PAYMENT = 1;
	public static final int EXTERNAL_PAYMENT = 2;
	
	private static PrintWriter defaultPrintWriter = new Log4jLoggerWriter(Debug.getLogger(module));
	private static PosTransaction currentTx = null;
	private static LifoSet<PosTransaction> savedTx = new LifoSet<PosTransaction>();
	
	protected XuiSession session = null;
	protected ShoppingCart cart = null;
	protected CheckOutHelper ch = null;
	protected PrintWriter trace = null;
	protected GenericValue txLog = null;
	
	protected String productStoreId = null;
	protected String transactionId = null;
	protected String facilityId = null;
	protected String terminalId = null;
	protected String currency = null;
	protected String orderId = null;
	protected String partyId = null;
	protected Locale locale = null;
	protected boolean isOpen = false;
	protected int drawerIdx = 0;
	protected HashMap<String, String> facilityProduct = new HashMap<String, String>();
	protected HashMap<String, BigDecimal> productQuantity = new HashMap<String, BigDecimal>();
	
	private GenericValue shipAddress = null;
	private final Map<String, Integer> skuDiscounts = FastMap.newInstance();
	private int cartDiscount = -1;
	
	public PosTransaction(final XuiSession session) {
		this.session = session;
		terminalId = session.getId();
		partyId = "_NA_";
		trace = defaultPrintWriter;
		
		productStoreId = (String) session.getAttribute("productStoreId");
		facilityId = (String) session.getAttribute("facilityId");
		currency = (String) session.getAttribute("currency");
//        this.locale = (Locale) session.getAttribute("locale"); This is legacy code and may come (demo) from ProductStore.defaultLocaleString defined in demoRetail and is incompatible with how localisation is handled in the POS
		locale = Locale.getDefault();
		
		cart = new ShoppingCart(session.getDelegator(), productStoreId, locale, currency);
		ch = new CheckOutHelper(session.getDispatcher(), session.getDelegator(), cart);
		cart.setChannelType("POS_SALES_CHANNEL");
		cart.setTransactionId(transactionId);
		//cart.setFacilityId(facilityId);
		cart.setTerminalId(terminalId);
//		if (session.getUserLogin() != null) {
//			cart.addAdditionalPartyRole(session.getUserLogin().getString("partyId"), "SALES_REP");
//		}
		
		// setup the TX log
		transactionId = session.getDelegator().getNextSeqId("PosTerminalLog");
		txLog = session.getDelegator().makeValue("PosTerminalLog");
		txLog.set("posTerminalLogId", transactionId);
		txLog.set("posTerminalId", terminalId);
		txLog.set("transactionId", transactionId);
		txLog.set("userLoginId", session.getUserId());
		txLog.set("statusId", "POSTX_ACTIVE");
		txLog.set("logStartDateTime", UtilDateTime.nowTimestamp());
		try {
			txLog.create();
		} catch (final GenericEntityException e) {
			Debug.logError(e, "Unable to create TX log - not fatal", module);
		}
		
		currentTx = this;
		trace("transaction created");
	}
	
	public XuiSession getSession() {
		return session;
	}
	
	public String getUserId() {
		return session.getUserId();
	}
	
	public String getPartyId() {
		return partyId;
	}
	
	public void setPartyId(final String partyId) {
		this.partyId = partyId;
		cart.setPlacingCustomerPartyId(partyId);
		try {
			cart.setUserLogin(session.getUserLogin(), session.getDispatcher());
		} catch (final CartItemModifyException e) {
			Debug.logError(e, module);
		}
	}
	
	public int getDrawerNumber() {
		return drawerIdx + 1;
	}
	
	public void popDrawer() {
		DeviceLoader.drawer[drawerIdx].openDrawer();
	}
	
	public String getTransactionId() {
		return transactionId;
	}
	
	public String getTerminalId() {
		return terminalId;
	}
	
	public String getFacilityId() {
		return facilityId;
	}
	
	public String getTerminalLogId() {
		return txLog.getString("posTerminalLogId");
	}
	
	public boolean isOpen() {
		if (!isOpen) {
			final GenericValue terminalState = getTerminalState();
			if (terminalState != null) {
				isOpen = true;
			} else {
				isOpen = false;
			}
		}
		return isOpen;
	}
	
	public boolean isEmpty() {
		return (UtilValidate.isEmpty(cart));
	}
	
	public List<GenericValue> lookupItem(final String sku) throws GeneralException {
		return ProductWorker.findProductsById(session.getDelegator(), sku, null);
	}
	
	public String getOrderId() {
		return orderId;
	}
	
	public BigDecimal getTaxTotal() {
		return cart.getTotalSalesTax();
	}
	
	public BigDecimal getGrandTotal() {
		return cart.getGrandTotal();
	}
	
	public int getNumberOfPayments() {
		return cart.selectedPayments();
	}
	
	public BigDecimal getPaymentTotal() {
		return cart.getPaymentTotal();
	}
	
	public BigDecimal getTotalDue() {
		final BigDecimal grandTotal = getGrandTotal();
		final BigDecimal paymentAmt = getPaymentTotal();
		return grandTotal.subtract(paymentAmt);
	}
	
	public int size() {
		return cart.size();
	}
	
	public void setCurrentProductFacility(final String facilityId) {
		if (UtilValidate.isNotEmpty(facilityId)) {
			//cart.setFacilityId(facilityId);
		} else {
			//cart.setFacilityId(this.facilityId);
		}
	}
	
	public String setReservationDetails(final String facilityId, final String productId, final BigDecimal quantity) {
		facilityProduct.put(productId, facilityId);
		productQuantity.put(productId, quantity);
		return "success";
	}
	
	public void performReservation () {
		Iterator it = facilityProduct.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        String productId = (String) pairs.getKey();
	        String facilityId = (String) pairs.getValue();
	        BigDecimal quantity = productQuantity.get(pairs.getKey());
	        reserveFromMultipleFacility(facilityId,productId,quantity);
	        it.remove();
	    }
		
	}

	public String reserveFromMultipleFacility(final String facilityId, final String productId, final BigDecimal quantity) {
		String inventoryItemId = null;
		try {
			String consigneeFacility = UtilProperties.getPropertyValue("general.properties", "electroman.consignee.facility");
			final List<GenericValue> inventoryItemList = session.getDelegator().findByAnd("InventoryItem", UtilMisc.toMap("productId", productId, "facilityId", facilityId));
			for (final GenericValue inventoryItem : inventoryItemList) {
				if (!consigneeFacility.equals(facilityId)) {
					if (inventoryItem.getBigDecimal("availableToPromiseTotal").compareTo(quantity) != -1) {
						inventoryItemId = inventoryItem.getString("inventoryItemId"); 
						break;
					}
				} else {
						 inventoryItemId = inventoryItem.getString("inventoryItemId"); 
						 break;
				}
			}
			final List<GenericValue> invItemDetailList = session.getDelegator().findByAnd("InventoryItemDetail", UtilMisc.toMap("inventoryItemId",inventoryItemId));
			for (GenericValue invItem : invItemDetailList) {
				invItem.put("availableToPromiseDiff", invItem.getBigDecimal("availableToPromiseDiff").subtract(quantity));
				invItem.put("quantityOnHandDiff", invItem.getBigDecimal("quantityOnHandDiff").subtract(quantity));
				//invItem.put("accountingQuantityDiff", invItem.getBigDecimal("accountingQuantityDiff").subtract(quantity));
				invItem.store();
				break;
			}
			
		} catch (final GenericEntityException e) {
			e.printStackTrace();
		}
		return inventoryItemId;
		
	}
	
	public boolean isMarketingPackage(final String productId) {
		boolean flag = false;
		GenericValue productItem;
		try {
			productItem = session.getDelegator().findOne("Product", UtilMisc.toMap("productId", productId), false);
			if (UtilValidate.isNotEmpty(productItem)) {
				if (productItem.getString("productTypeId").equals("MARKETING_PKG_AUTO") ||productItem.getString("productTypeId").equals("MARKETING_PKG")) {
					flag = true;
				}
			}
		} catch (final GenericEntityException e) {
			e.printStackTrace();
		}
		return flag;
	}
	
	public ArrayList<MarketingCartItemInfo> getMarketingPackageDetails(final String productId) {
		final ArrayList<MarketingCartItemInfo> marketingItems = new ArrayList<MarketingCartItemInfo>();
		
		List<GenericValue> productAssocList = null;
		GenericValue productItem;
		try {
			productItem = session.getDelegator().findOne("Product", UtilMisc.toMap("productId", productId), false);
			if (UtilValidate.isNotEmpty(productItem)) {
				if (productItem.getString("productTypeId").equals("MARKETING_PKG_AUTO") || productItem.getString("productTypeId").equals("MARKETING_PKG")) {
					productAssocList = session.getDelegator().findByAnd("ProductAssoc", UtilMisc.toMap("productId", productId, "productAssocTypeId", "MANUF_COMPONENT"));
				}
			}
			if (UtilValidate.isNotEmpty(productAssocList)) {
				for (final GenericValue association : productAssocList) {
					final GenericValue assocItem = session.getDelegator().findOne("Product", UtilMisc.toMap("productId", association.getString("productIdTo")), false);
					final MarketingCartItemInfo bundleItem = new MarketingCartItemInfo(assocItem.getString("productId"), assocItem.getString("productName"), association.getString("quantity"), "0.000");
					marketingItems.add(bundleItem);
				}
			}
			
		} catch (final GenericEntityException e) {
			e.printStackTrace();
		}
		return marketingItems;
	}
	public String  setExchangeOfferInfo() {
		final String orderId = cart.getOrderId();
		List<GenericValue> orderItemList;
		List<GenericValue> quoteItemList;
		try {
			orderItemList = session.getDelegator().findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId));

			if (UtilValidate.isNotEmpty(orderItemList)) {
				for (final GenericValue orderItem : orderItemList) {
					if (UtilValidate.isNotEmpty(orderItem.getString("quoteId"))) {
						String quoteId = orderItem.getString("quoteId");
						quoteItemList = session.getDelegator().findByAnd("QuoteItem", UtilMisc.toMap("quoteId", quoteId,"quoteItemSeqId",orderItem.getString("orderItemSeqId")));
						if (UtilValidate.isNotEmpty(quoteItemList)) {
						GenericValue quoteItem = EntityUtil.getFirst(quoteItemList);
						String isExchange = quoteItem.getString("isExchange");
						orderItem.set("isExchange", isExchange);
						orderItem.store();
						}
					}
				}
			} 
		}catch (GenericEntityException e) {
				Debug.logError(e, module);
			}

		
		return "success";
	}

	public String  removeQuoteDetails() {
		final String orderId = cart.getOrderId();
		List<GenericValue> orderItemList;
		try {
			orderItemList = session.getDelegator().findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId));
			String quoteId = null;
			if (UtilValidate.isNotEmpty(orderItemList)) {
				for (final GenericValue orderItem : orderItemList) {
					if (UtilValidate.isNotEmpty(orderItem.getString("quoteId"))) {
						quoteId = orderItem.getString("quoteId");
						orderItem.set("quoteId", null);
						orderItem.set("quoteItemSeqId", null);
						orderItem.store();
					}
				}
				if (UtilValidate.isNotEmpty(quoteId)) {
						List<GenericValue> quoteItemList = session.getDelegator().findByAnd("QuoteItem", UtilMisc.toMap("quoteId", quoteId));
						if (UtilValidate.isNotEmpty(quoteItemList)) {
							session.getDelegator().removeAll(quoteItemList);
						}
						List<GenericValue> quoteRoleList = session.getDelegator().findByAnd("QuoteRole", UtilMisc.toMap("quoteId", quoteId));
						if (UtilValidate.isNotEmpty(quoteRoleList)) {
							session.getDelegator().removeAll(quoteRoleList);
						}
						GenericValue quoteInfo = session.getDelegator().findByPrimaryKey("Quote", UtilMisc.toMap("quoteId", quoteId));
						if (UtilValidate.isNotEmpty(quoteInfo)) {
							quoteInfo.remove();
						}
						
					
				}
			}
			
		} catch (GenericEntityException e) {
			Debug.logError(e, module);
		}
		return "success";
	}
	public String getProductsFacility(PosScreen pos) {
		int size = cart.size();
		FacilityInput finput = pos.getFacilityInput();
		String facilityId = finput.getSelectedFacility();
		final LocalDispatcher dispatcher = session.getDispatcher();

		String productId = null;
		if (size>=1) {
			ShoppingCartItem item = cart.findCartItem(0);
			productId = (String) item.getProductId();
			BigDecimal quantity = item.getQuantity();
			String consigneeFacility = UtilProperties.getPropertyValue("general.properties", "electroman.consignee.facility");
			final String inventoryStatus = checkProductAvailability(productId, facilityId);
			if (UtilValidate.isEmpty(productId)) {
				try {
					cart.removeCartItem(item, dispatcher);
				} catch (CartItemModifyException e) {
					e.printStackTrace();
				}
				pos.showDialog("dialog/error/exception", "CART IS EMPTY");
				pos.showPage("pospanel");
			}else if ("error".equals(facilityId)) {
				try {
					cart.removeCartItem(item, dispatcher);
				} catch (CartItemModifyException e) {
					e.printStackTrace();
				}
				pos.showDialog("dialog/error/exception", "SELECT A STOCK LOCATION");
				pos.showPage("pospanel");
			} else if (("NOT_AVAILABLE".equals(inventoryStatus))&& (!consigneeFacility.equals(facilityId))) {
				try {
					cart.removeCartItem(item, dispatcher);
				} catch (CartItemModifyException e) {
					e.printStackTrace();
				}
				pos.showDialog("dialog/error/exception", "NOT ENOUGH STOCK IN THE SELECTED LOCATION.");
				pos.showPage("pospanel");
			} else if (("NOT_AVAILABLE".equals(inventoryStatus))&& (consigneeFacility.equals(facilityId))) {
				try {
					cart.removeCartItem(item, dispatcher);
				} catch (CartItemModifyException e) {
					e.printStackTrace();
				}
				pos.showDialog("dialog/error/exception", "CONSIGNEE INVENTORY IS NOT ASSIGNED FOR THIS PRODUCT.");
				pos.showPage("pospanel");
			}
			else {
				if (UtilValidate.isNotEmpty(facilityId) && (!"error".equals(facilityId))) {
					setReservationDetails(facilityId, productId, quantity);
				}
				Debug.logInfo("@@@@@@@@@@@@@@@@@@@@@@@@@@reserving " + quantity + "from the "+facilityId, module);
			}			
		}
		return "success";
		
	}
	public String  reserveNonMarketingProducts() {
		final String orderId = cart.getOrderId();
		List<GenericValue> orderItemList;
		List<GenericValue> orderItemShipList = new ArrayList<GenericValue>();
		List<GenericValue> orderItemIssueList = new ArrayList<GenericValue>();
		List<GenericValue> quoteOrderItems = new ArrayList<GenericValue>();
		List<GenericValue> reservOrderItems = new ArrayList<GenericValue>();

		try {
			orderItemList = session.getDelegator().findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId));
			if (UtilValidate.isNotEmpty(orderItemList)) {
				for (final GenericValue orderItem : orderItemList) {	
					String productId = orderItem.getString("productId");
					BigDecimal quantity = orderItem.getBigDecimal("quantity");
					String facilityId = facilityProduct.get(productId);
					Timestamp currentDate = UtilDateTime.nowTimestamp();
					String inventoryItemId = reserveFromMultipleFacility(facilityId, productId, quantity);
					if (UtilValidate.isNotEmpty(inventoryItemId)) {
						GenericValue orderItemInvRes = session.getDelegator().makeValue("OrderReservationSummary");
						orderItemInvRes.set("orderId", orderId);
						orderItemInvRes.set("orderItemSeqId", orderItem.getString("orderItemSeqId"));
						orderItemInvRes.set("productId", orderItem.getString("productId"));
						orderItemInvRes.set("facilityId", facilityProduct.get(orderItem.getString("productId")));
						orderItemInvRes.create();
						GenericValue orderItemShipGrpInvRes = session.getDelegator().makeValue("OrderItemShipGrpInvRes");
						GenericValue orderItemIssuance = session.getDelegator().makeValue("ItemIssuance");
						orderItemIssuance.set("itemIssuanceId", session.getDelegator().getNextSeqId("ItemIssuance"));
						orderItemIssuance.set("orderId", orderId);
						orderItemIssuance.set("orderId", orderId);
						orderItemIssuance.set("shipGroupSeqId","00001");
						orderItemIssuance.set("orderItemSeqId", orderItem.getString("orderItemSeqId"));
						orderItemIssuance.set("quantity", quantity);
						orderItemIssuance.set("inventoryItemId",inventoryItemId);
						orderItemIssueList.add(orderItemIssuance);
	                    orderItemShipGrpInvRes.set("orderId", orderId);
	                    orderItemShipGrpInvRes.set("shipGroupSeqId","00001");
	                    orderItemShipGrpInvRes.set("orderItemSeqId", orderItem.getString("orderItemSeqId"));
	                    orderItemShipGrpInvRes.set("quantity", quantity);
	                    orderItemShipGrpInvRes.set("quantityNotAvailable", BigDecimal.ZERO);
	                    orderItemShipGrpInvRes.set("inventoryItemId",inventoryItemId);
	                    orderItemShipGrpInvRes.set("promisedDatetime",currentDate);
	                    orderItemShipGrpInvRes.set("reservedDatetime",currentDate);
	                    orderItemShipGrpInvRes.set("createdDatetime",currentDate);
	                    orderItemShipGrpInvRes.set("priority","2");
	                    orderItemShipList.add(orderItemShipGrpInvRes);
					}
					//session.getDelegator().storeAll(orderItemShipList);
					session.getDelegator().storeAll(orderItemIssueList);

				}
			}
		} catch (GenericEntityException e) {
			Debug.logError(e, module);
		}
		return "success";
	}
	
	public String issueMarkettingPackage (String inventoryItemId,String productId,String facilityId) {
		Debug.logInfo("################ issueMarkettingPackage  - " +inventoryItemId, module);

		final String orderId = cart.getOrderId();
		List<GenericValue> orderItemList;
		List<GenericValue> orderItemShipList = new ArrayList<GenericValue>();
		List<GenericValue> orderItemIssueList = new ArrayList<GenericValue>();
		List<GenericValue> quoteOrderItems = new ArrayList<GenericValue>();
		List<GenericValue> reservOrderItems = new ArrayList<GenericValue>();

		try {
			orderItemList = session.getDelegator().findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId));
			if (UtilValidate.isNotEmpty(orderItemList)) {
				for (final GenericValue orderItem : orderItemList) {	
					BigDecimal quantity = orderItem.getBigDecimal("quantity");
					Timestamp currentDate = UtilDateTime.nowTimestamp();
					if (UtilValidate.isNotEmpty(inventoryItemId)) {
						
						GenericValue orderItemShipGrpInvRes = session.getDelegator().makeValue("OrderItemShipGrpInvRes");
						GenericValue orderItemIssuance = session.getDelegator().makeValue("ItemIssuance");
						orderItemIssuance.set("itemIssuanceId", session.getDelegator().getNextSeqId("ItemIssuance"));
						orderItemIssuance.set("orderId", orderId);
						orderItemIssuance.set("orderId", orderId);
						orderItemIssuance.set("shipGroupSeqId","00001");
						orderItemIssuance.set("orderItemSeqId", orderItem.getString("orderItemSeqId"));
						orderItemIssuance.set("quantity", quantity);
						orderItemIssuance.set("inventoryItemId",inventoryItemId);
						orderItemIssueList.add(orderItemIssuance);
	                    orderItemShipGrpInvRes.set("orderId", orderId);
	                    orderItemShipGrpInvRes.set("shipGroupSeqId","00001");
	                    orderItemShipGrpInvRes.set("orderItemSeqId", orderItem.getString("orderItemSeqId"));
	                    orderItemShipGrpInvRes.set("quantity", quantity);
	                    orderItemShipGrpInvRes.set("quantityNotAvailable", BigDecimal.ZERO);
	                    orderItemShipGrpInvRes.set("inventoryItemId",inventoryItemId);
	                    orderItemShipGrpInvRes.set("promisedDatetime",currentDate);
	                    orderItemShipGrpInvRes.set("reservedDatetime",currentDate);
	                    orderItemShipGrpInvRes.set("createdDatetime",currentDate);
	                    orderItemShipGrpInvRes.set("priority","2");
	                    orderItemShipList.add(orderItemShipGrpInvRes);
					}
					//session.getDelegator().storeAll(orderItemShipList);
					session.getDelegator().storeAll(orderItemIssueList);

				}
			}
		} catch (GenericEntityException e) {
			Debug.logError(e, module);
		}
		return "success";	
	}
	public String reserveMarkettingPackage() {
		String prodFacility = null;
		final List<String> productIdList = new ArrayList<String>();
		final Map<String, BigDecimal> productItems = new HashMap<String, BigDecimal>();
		final String orderId = cart.getOrderId();
		List<GenericValue> orderItemList;
		List<GenericValue> productAssocList = null;
		boolean validInventory = false;
		String result = "error";
		try {
			orderItemList = session.getDelegator().findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId));
			if (UtilValidate.isNotEmpty(orderItemList)) {
				for (final GenericValue orderItem : orderItemList) {
					productItems.put(orderItem.getString("productId"), orderItem.getBigDecimal("quantity"));
					final GenericValue productItem = session.getDelegator().findOne("Product", UtilMisc.toMap("productId", orderItem.getString("productId")), false);
					 prodFacility = facilityProduct.get(orderItem.getString("productId"));
					if (UtilValidate.isNotEmpty(productItem)) {
						if (productItem.getString("productTypeId").equals("MARKETING_PKG_AUTO")||productItem.getString("productTypeId").equals("MARKETING_PKG")) {
							productIdList.add((String) productItem.get("productId"));
							GenericValue orderItemInvRes = session.getDelegator().makeValue("OrderReservationSummary");
							orderItemInvRes.set("orderId", orderId);
							orderItemInvRes.set("orderItemSeqId", orderItem.getString("orderItemSeqId"));
							orderItemInvRes.set("productId", orderItem.getString("productId"));
							orderItemInvRes.set("facilityId", prodFacility);
							orderItemInvRes.create();
						}
					}
				}
			}
			if (UtilValidate.isNotEmpty(productIdList)) {
				for (final String productId : productIdList) {
					productAssocList = session.getDelegator().findByAnd("ProductAssoc", UtilMisc.toMap("productId", productId, "productAssocTypeId", "MANUF_COMPONENT"));
					if (UtilValidate.isNotEmpty(productAssocList)) {
						String inventoryItemId = null;
						for (final GenericValue association : productAssocList) {
							BigDecimal reserveQuantity = productItems.get(association.getString("productId"));
							BigDecimal invQuantity = association.getBigDecimal("quantity").multiply(reserveQuantity);
							final List<GenericValue> inventoryItemList = session.getDelegator().findByAnd("InventoryItem", UtilMisc.toMap("productId", association.getString("productIdTo"), "facilityId", prodFacility));
							for (final GenericValue inventoryItem : inventoryItemList) {
								if (inventoryItem.getBigDecimal("availableToPromiseTotal").compareTo(invQuantity) >0) {
									inventoryItemId = inventoryItem.getString("inventoryItemId");
									break;
								}
							}
							issueMarkettingPackage(inventoryItemId,association.getString("productIdTo"),prodFacility);
							final List<GenericValue> invItemDetailList = session.getDelegator().findByAnd("InventoryItemDetail", UtilMisc.toMap("inventoryItemId",inventoryItemId));
							for (GenericValue invItem : invItemDetailList) {
								invItem.put("availableToPromiseDiff", invItem.getBigDecimal("availableToPromiseDiff").subtract(invQuantity));
								invItem.put("quantityOnHandDiff", invItem.getBigDecimal("quantityOnHandDiff").subtract(invQuantity));
								//invItem.put("accountingQuantityDiff", invItem.getBigDecimal("accountingQuantityDiff").subtract(invQuantity));
								invItem.store();
								validInventory = true;
								break;
							}
						}
					}
				}
			} 

		} catch (final GenericEntityException e) {
			e.printStackTrace();
		}
		if (validInventory) {
			result = "success";
		}
		return result;
	}
	
	public String getPOSInvoiceId() {
		String invoiceId = null;
		List<GenericValue> orderInvoiceIdList = new ArrayList<GenericValue>();
		final String orderId = cart.getOrderId();
		final LocalDispatcher dispatcher = session.getDispatcher();
		try {
			orderInvoiceIdList = session.getDelegator().findByAnd("OrderItemBilling", UtilMisc.toMap("orderId", orderId));
			if (UtilValidate.isNotEmpty(orderInvoiceIdList)) {
				final GenericValue orderInvoiceId = EntityUtil.getFirst(orderInvoiceIdList);
				Debug.logInfo("invoice info" + orderInvoiceId, module);
				invoiceId = orderInvoiceId.getString("invoiceId");
			} else {
				Debug.logInfo("invoice info is empty", module);
			}
		} catch (final GenericEntityException e) {
			Debug.logError(module, e.getMessage());
		}
		Map inputs = UtilMisc.toMap("invoiceId", invoiceId);
		try {
			dispatcher.runSync("printPickingTicket",inputs);
		} catch (GenericServiceException e) {
			e.printStackTrace();
		}
		
		return invoiceId;//catalog to main rebate%
	} 
	
	public Map<String, Object> getItemInfo(final int index) {
		final ShoppingCartItem item = cart.findCartItem(index);
		final Map<String, Object> itemInfo = FastMap.newInstance();
		itemInfo.put("productId", item.getProductId());
		final String description = item.getDescription();
		if (UtilValidate.isEmpty(description)) {
			itemInfo.put("description", item.getName());
		} else {
			itemInfo.put("description", description);
		}
		itemInfo.put("quantity", UtilFormatOut.formatQuantity(item.getQuantity()));
		itemInfo.put("subtotal", UtilFormatOut.formatPrice(item.getItemSubTotal()));
		itemInfo.put("isTaxable", item.taxApplies() ? "T" : " ");
		
		itemInfo.put("discount", "");
		itemInfo.put("adjustments", "");
		if (item.getOtherAdjustments().compareTo(BigDecimal.ZERO) != 0) {
			itemInfo.put("itemDiscount", UtilFormatOut.padString(
					UtilProperties.getMessage(resource, "PosItemDiscount", locale), Receipt.pridLength[0] + 1, true, ' '));
			itemInfo.put("adjustments", UtilFormatOut.formatPrice(item.getOtherAdjustments()));
		}
		
		if (isAggregatedItem(item.getProductId())) {
			ProductConfigWrapper pcw = null;
			pcw = item.getConfigWrapper();
			itemInfo.put("basePrice", UtilFormatOut.formatPrice(pcw.getDefaultPrice()));
		} else {
			itemInfo.put("basePrice", UtilFormatOut.formatPrice(item.getBasePrice()));
		}
		return itemInfo;
	}
	
	public List<Map<String, Object>> getItemConfigInfo(final int index) {
		final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		// I think I need to initialize the list in a special way
		// to use foreach in receipt.java
		
		final ShoppingCartItem item = cart.findCartItem(index);
		if (isAggregatedItem(item.getProductId())) {
			ProductConfigWrapper pcw = null;
			pcw = item.getConfigWrapper();
			final List<ConfigOption> selected = pcw.getSelectedOptions();
			for (final ConfigOption configoption : selected) {
				final Map<String, Object> itemInfo = FastMap.newInstance();
				if (configoption.isSelected() && !configoption.isDefault()) {
					itemInfo.put("productId", "");
					itemInfo.put("sku", "");
					itemInfo.put("configDescription", configoption.getDescription(locale));
					itemInfo.put("configQuantity", UtilFormatOut.formatQuantity(item.getQuantity()));
					itemInfo.put("configBasePrice", UtilFormatOut.formatPrice(configoption.getOffsetPrice()));
					//itemInfo.put("isTaxable", item.taxApplies() ? "T" : " ");
					list.add(itemInfo);
				}
			}
		}
		return list;
	}
	
	public Map<String, Object> getPaymentInfo(final int index) {
		final ShoppingCart.CartPaymentInfo inf = cart.getPaymentInfo(index);
		final GenericValue infValue = inf.getValueObject(session.getDelegator());
		GenericValue paymentPref = null;
		try {
			final Map<String, Object> fields = FastMap.newInstance();
			fields.put("paymentMethodTypeId", inf.paymentMethodTypeId);
			if (inf.paymentMethodId != null) {
				fields.put("paymentMethodId", inf.paymentMethodId);
			}
			fields.put("maxAmount", inf.amount);
			fields.put("orderId", getOrderId());
			
			final List<GenericValue> paymentPrefs = session.getDelegator().findByAnd("OrderPaymentPreference", fields);
			if (UtilValidate.isNotEmpty(paymentPrefs)) {
				//Debug.logInfo("Found some prefs - " + paymentPrefs.size(), module);
				if (paymentPrefs.size() > 1) {
					Debug.logError("Multiple OrderPaymentPreferences found for the same payment method!", module);
				} else {
					paymentPref = EntityUtil.getFirst(paymentPrefs);
					//Debug.logInfo("Got the first pref - " + paymentPref, module);
				}
			} else {
				Debug.logError("No OrderPaymentPreference found - " + fields, module);
			}
		} catch (final GenericEntityException e) {
			Debug.logError(e, module);
		}
		//Debug.logInfo("PaymentPref - " + paymentPref, module);
		
		final Map<String, Object> payInfo = FastMap.newInstance();
		
		// locate the auth info
		GenericValue authTrans = null;
		if (paymentPref != null) {
			authTrans = PaymentGatewayServices.getAuthTransaction(paymentPref);
			if (authTrans != null) {
				payInfo.putAll(authTrans);
				
				final String authInfoString = "Ref: " + authTrans.getString("referenceNum") + " Auth: " + authTrans.getString("gatewayCode");
				payInfo.put("authInfoString", authInfoString);
			} else {
				Debug.logError("No Authorization transaction found for payment preference - " + paymentPref, module);
			}
		} else {
			Debug.logError("Payment preference is empty!", module);
			return payInfo;
		}
		//Debug.logInfo("AuthTrans - " + authTrans, module);
		
		if ("PaymentMethodType".equals(infValue.getEntityName())) {
			payInfo.put("description", infValue.get("description", locale));
			payInfo.put("payInfo", infValue.get("description", locale));
			payInfo.put("amount", UtilFormatOut.formatPrice(inf.amount));
		} else {
			final String paymentMethodTypeId = infValue.getString("paymentMethodTypeId");
			GenericValue pmt = null;
			try {
				pmt = infValue.getRelatedOne("PaymentMethodType");
			} catch (final GenericEntityException e) {
				Debug.logError(e, module);
			}
			if (pmt != null) {
				payInfo.put("description", pmt.get("description", locale));
				payInfo.put("amount", UtilFormatOut.formatPrice(inf.amount));
			}
			
			if ("CREDIT_CARD".equals(paymentMethodTypeId)) {
				GenericValue cc = null;
				try {
					cc = infValue.getRelatedOne("CreditCard");
				} catch (final GenericEntityException e) {
					Debug.logError(e, module);
				}
				String nameOnCard = cc.getString("firstNameOnCard") + " " + cc.getString("lastNameOnCard");
				nameOnCard = nameOnCard.trim();
				payInfo.put("nameOnCard", nameOnCard);
				
				final String cardNum = cc.getString("cardNumber");
				final String cardStr = UtilFormatOut.formatPrintableCreditCard(cardNum);
				
				final String expDate = cc.getString("expireDate");
				final String infoString = cardStr + " " + expDate;
				payInfo.put("payInfo", infoString);
				payInfo.putAll(cc);
				payInfo.put("cardNumber", cardStr);  // masked cardNumber
				
			} else if ("GIFT_CARD".equals(paymentMethodTypeId)) {
				/*
				GenericValue gc = null;
				try {
				    gc = infValue.getRelatedOne("GiftCard"); //FIXME is this really useful ? (Maybe later...)
				} catch (GenericEntityException e) {
				    Debug.logError(e, module);
				}
				 */
			}
		}
		
		return payInfo;
	}
	
	public BigDecimal getItemQuantity(final String productId) {
		trace("request item quantity", productId);
		final ShoppingCartItem item = cart.findCartItem(productId, null, null, null, BigDecimal.ZERO);
		if (item != null) {
			return item.getQuantity();
		} else {
			trace("item not found", productId);
			return BigDecimal.ZERO;
		}
	}
	
	public boolean isAggregatedItem(final String productId) {
		trace("is Aggregated Item", productId);
		try {
			final Delegator delegator = cart.getDelegator();
			GenericValue product = null;
			product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
			if (UtilValidate.isNotEmpty(product) && "AGGREGATED".equals(product.getString("productTypeId"))) {
				return true;
			}
		} catch (final GenericEntityException e) {
			trace("item lookup error", e);
			Debug.logError(e, module);
		} catch (final Exception e) {
			trace("general exception", e);
			Debug.logError(e, module);
		}
		return false;
	}
	
	public ProductConfigWrapper getProductConfigWrapper(final String productId) {
		//Get a PCW for a new product
		trace("get Product Config Wrapper", productId);
		
		ProductConfigWrapper pcw = null;
		try {
			final Delegator delegator = cart.getDelegator();
			pcw = new ProductConfigWrapper(delegator, session.getDispatcher(),
					productId, null, null, null, null, null, null);
			trace("Product Config Wrapper", pcw.toString());
		} catch (final ItemNotFoundException e) {
			trace("item not found", e);
			//throw e;
		} catch (final CartItemModifyException e) {
			trace("add item error", e);
			//throw e;
		} catch (final GenericEntityException e) {
			trace("item lookup error", e);
			Debug.logError(e, module);
		} catch (final Exception e) {
			trace("general exception", e);
			Debug.logError(e, module);
		}
		return pcw;
	}
	
	public ProductConfigWrapper getProductConfigWrapper(final String productId, final String cartIndex) {
		// Get a PCW for a pre-configured product
		trace("get Product Config Wrapper", productId + "/" + cartIndex);
		ProductConfigWrapper pcw = null;
		try {
			final int index = Integer.parseInt(cartIndex);
			final ShoppingCartItem product = cart.findCartItem(index);
			pcw = product.getConfigWrapper();
		} catch (final Exception e) {
			trace("general exception", e);
			Debug.logError(e, module);
		}
		return pcw;
	}
	
	public void addItem(final String productId, final BigDecimal quantity) throws CartItemModifyException, ItemNotFoundException {
		trace("add item", productId + "/" + quantity);
		
		try {
			final Delegator delegator = cart.getDelegator();
			GenericValue product = null;
			ProductConfigWrapper pcw = null;
			product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
			if (UtilValidate.isNotEmpty(product) && "AGGREGATED".equals(product.getString("productTypeId"))) {
				// if it's an aggregated item, load the configwrapper and set to defaults
				pcw = new ProductConfigWrapper(delegator, session.getDispatcher(), productId, null, null, null, null, null, null);
				pcw.setDefaultConfig();
			}
			//cart.addOrIncreaseItem(productId, null, quantity, null, null, null, null, null, null, null, null, null, null, null, null, session.getDispatcher());
			cart.addOrIncreaseItem(productId, null, quantity, null, null, null, null, null, null, null, null, pcw, null, null, null, session.getDispatcher());

		} catch (final ItemNotFoundException e) {
			trace("item not found", e);
			throw e;
		} catch (final CartItemModifyException e) {
			trace("add item error", e);
			throw e;
		} catch (final GenericEntityException e) {
			trace("item lookup error", e);
			Debug.logError(e, module);
		} catch (final Exception e) {
			trace("general exception", e);
			Debug.logError(e, module);
		}
	}
	public BigDecimal cartItemQuantity () {
		final ShoppingCartItem carts = cart.findCartItem(0);
		BigDecimal itemQuantity = carts.getQuantity();
		Debug.logInfo("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"+carts.getQuantity(),module);
		Debug.logInfo("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"+carts.getProductId(),module);
		return itemQuantity;
	}
	public String reserveQuoteProducts (String quoteId) {
		try {
			List<GenericValue> quoteItems = session.getDelegator().findByAnd("QuoteItem", UtilMisc.toMap("quoteId", quoteId));
			for (GenericValue quoteItem: quoteItems) {
				reserveFromMultipleFacility(quoteItem.getString("facilityId"), quoteItem.getString("productId"), (quoteItem.getBigDecimal("quantity")));
			}
		} catch (GenericEntityException e) {
			e.printStackTrace();
		}
		
		return "success";
	}
	public String checkProductAvailability(final String productId, final String facilityId) {
		String productStatus = null;
		final LocalDispatcher dispatcher = session.getDispatcher();
		boolean beganTransaction = false;
		Map<String, Object> inventoryProductStatus = new HashMap();
		try {
			beganTransaction = TransactionUtil.begin();
			String consigneeFacility = UtilProperties.getPropertyValue("general.properties", "electroman.consignee.facility");
			inventoryProductStatus = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", productId, "facilityId", facilityId));
			BigDecimal ATP = (BigDecimal) inventoryProductStatus.get("availableToPromiseTotal");
			Map<String, Object> inventoryResult = new HashMap();
			if (isMarketingPackage(productId)) {
				inventoryResult = dispatcher.runSync("getMktgPackagesAvailable", UtilMisc.toMap("productId", productId, "facilityId", facilityId));
				ATP = (BigDecimal) inventoryResult.get("availableToPromiseTotal");
			}	
			if (!consigneeFacility.equals(facilityId)) {
				if (ATP.compareTo(BigDecimal.ZERO) > 0) {
					productStatus = "AVAILABLE";
				} 
				else {
					productStatus = "NOT_AVAILABLE";
				}
			} else {
				try {
					final List<GenericValue> inventoryItemList = session.getDelegator().findByAnd("InventoryItem", UtilMisc.toMap("productId", productId, "facilityId", facilityId));
					if (UtilValidate.isEmpty(inventoryItemList)) {
						productStatus = "NOT_AVAILABLE";
					} else {
						productStatus = "AVAILABLE";
					} 
				} catch (GenericEntityException e) {
					Debug.logInfo("Inventory find operation failed"+e.getMessage(), module);
				}
			}
		} catch (final GenericServiceException e) {
			Debug.logError("Facility operation failed", module);
		} catch (final GenericTransactionException e) {
			e.printStackTrace();
		} finally {
			try {
				TransactionUtil.commit(beganTransaction);
			} catch (final GenericTransactionException e1) {
				Debug.logError(e1, "Unable to commit transaction", module);
			}
		}
		return productStatus;
	}
	
	public Map getFacilityInfo(final String productId, final ArrayList<String> facilities) {
		final HashMap inventoryStatus = new HashMap();
		final LocalDispatcher dispatcher = session.getDispatcher();
		boolean beganTransaction = false;
		Map<String, Object> inventoryResult = new HashMap();
		try {
			beganTransaction = TransactionUtil.begin();
			for (final String facilityId : facilities) {
				if (isMarketingPackage(productId)) {
					inventoryResult = dispatcher.runSync("getMktgPackagesAvailable", UtilMisc.toMap("productId", productId, "facilityId", facilityId));
				} else {
					inventoryResult = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", productId, "facilityId", facilityId));
				}
				if (ServiceUtil.isSuccess(inventoryResult)) {
					final BigDecimal i = (BigDecimal) inventoryResult.get("availableToPromiseTotal");
					final String availableToPromiseTotal = i.toPlainString();
					inventoryStatus.put(facilityId, availableToPromiseTotal);
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			try {
				TransactionUtil.rollback(beganTransaction, e.getMessage(), e);
			} catch (final GenericTransactionException e1) {
				e1.printStackTrace();
			} finally {
				try {
					TransactionUtil.commit(beganTransaction);
				} catch (final GenericTransactionException e1) {
					Debug.logError(e, "Unable to commit transaction", module);
				}
			}
			
		}
		return inventoryStatus;
	}
	
	public void addItem(final String productId, final ProductConfigWrapper pcw)
			throws ItemNotFoundException, CartItemModifyException {
		trace("add item with ProductConfigWrapper", productId);
		try {
			cart.addOrIncreaseItem(productId, null, BigDecimal.ONE, null, null, null, null, null, null, null, null, pcw, null, null, null, session.getDispatcher());
		} catch (final ItemNotFoundException e) {
			trace("item not found", e);
			throw e;
		} catch (final CartItemModifyException e) {
			trace("add item error", e);
			throw e;
		} catch (final Exception e) {
			trace("general exception", e);
			Debug.logError(e, module);
		}
	}
	
	public void modifyConfig(final String productId, final ProductConfigWrapper pcw, final String cartIndex)
			throws CartItemModifyException, ItemNotFoundException {
		trace("modify item config", cartIndex);
		try {
			final int cartIndexInt = Integer.parseInt(cartIndex);
			final ShoppingCartItem cartItem = cart.findCartItem(cartIndexInt);
			final BigDecimal quantity = cartItem.getQuantity();
			cart.removeCartItem(cartIndexInt, session.getDispatcher());
			cart.addOrIncreaseItem(productId, null, quantity, null, null, null, null, null, null, null, null, pcw, null, null, null, session.getDispatcher());
		} catch (final CartItemModifyException e) {
			Debug.logError(e, module);
			trace("void or add item error", productId, e);
			throw e;
		} catch (final ItemNotFoundException e) {
			trace("item not found", e);
			throw e;
		} catch (final Exception e) {
			trace("general exception", e);
			Debug.logError(e, module);
		}
		return;
	}
	
	public void modifyQty(final String productId, final BigDecimal quantity) throws CartItemModifyException {
		trace("modify item quantity", productId + "/" + quantity);
		final ShoppingCartItem item = cart.findCartItem(productId, null, null, null, BigDecimal.ZERO);
		if (item != null) {
			try {
				item.setQuantity(quantity, session.getDispatcher(), cart, true);
			} catch (final CartItemModifyException e) {
				Debug.logError(e, module);
				trace("modify item error", e);
				throw e;
			}
		} else {
			trace("item not found", productId);
		}
	}
	
	public void modifyPrice(final String productId, final BigDecimal price) {
		trace("modify item price", productId + "/" + price);
		final ShoppingCartItem item = cart.findCartItem(productId, null, null, null, BigDecimal.ZERO);
		if (item != null) {
			item.setBasePrice(price);
		} else {
			trace("item not found", productId);
		}
	}
	
	public void addDiscount(final String productId, final BigDecimal discount, final boolean percent) {
		final GenericValue adjustment = session.getDelegator().makeValue("OrderAdjustment");
		adjustment.set("orderAdjustmentTypeId", "DISCOUNT_ADJUSTMENT");
		if (percent) {
			adjustment.set("sourcePercentage", discount.movePointRight(2));
		} else {
			adjustment.set("amount", discount);
		}
		
		if (productId != null) {
			trace("add item adjustment");
			final ShoppingCartItem item = cart.findCartItem(productId, null, null, null, BigDecimal.ZERO);
			final Integer itemAdj = skuDiscounts.get(productId);
			if (itemAdj != null) {
				item.removeAdjustment(itemAdj.intValue());
			}
			final int idx = item.addAdjustment(adjustment);
			skuDiscounts.put(productId, idx);
		} else {
			trace("add sale adjustment");
			if (cartDiscount > -1) {
				cart.removeAdjustment(cartDiscount);
			}
			cartDiscount = cart.addAdjustment(adjustment);
		}
	}
	
	public void clearDiscounts() {
		if (cartDiscount > -1) {
			cart.removeAdjustment(cartDiscount);
			cartDiscount = -1;
		}
		for (final String productId : skuDiscounts.keySet()) {
			final ShoppingCartItem item = cart.findCartItem(productId, null, null, null, BigDecimal.ZERO);
			final Integer itemAdj = skuDiscounts.remove(productId);
			if (itemAdj != null) {
				item.removeAdjustment(itemAdj.intValue());
			}
		}
	}
	
	public BigDecimal GetTotalDiscount() {
		return cart.getOrderOtherAdjustmentTotal();
	}
	
	public void revertReservation(final String productId, final BigDecimal quantity) {
		
	}
	
	public void voidItem(final String productId) throws CartItemModifyException {
		trace("void item", productId);
		final ShoppingCartItem item = cart.findCartItem(productId, null, null, null, BigDecimal.ZERO);
		if (item != null) {
			try {
				final int itemIdx = cart.getItemIndex(item);
				Debug.logInfo("#####################"+item.getProductId()+item.getQuantity(), module);
				Debug.logInfo("#####################"+facilityProduct.get(item.getProductId()), module);

				cart.removeCartItem(itemIdx, session.getDispatcher());
			} catch (final CartItemModifyException e) {
				Debug.logError(e, module);
				trace("void item error", productId, e);
				throw e;
			}
		} else {
			trace("item not found", productId);
		}
	}
	
	public void voidSale(final PosScreen pos) {
		trace("void sale");
		txLog.set("statusId", "POSTX_VOIDED");
		txLog.set("itemCount", new Long(cart.size()));
		txLog.set("logEndDateTime", UtilDateTime.nowTimestamp());
		try {
			txLog.store();
		} catch (final GenericEntityException e) {
			Debug.logError(e, "Unable to store TX log - not fatal", module);
		}
		cart.clear();
		pos.getPromoStatusBar().clear();
		currentTx = null;
	}
	
	public void closeTx() {
		trace("transaction closed");
		txLog.set("statusId", "POSTX_CLOSED");
		txLog.set("itemCount", new Long(cart.size()));
		txLog.set("logEndDateTime", UtilDateTime.nowTimestamp());
		try {
			txLog.store();
		} catch (final GenericEntityException e) {
			Debug.logError(e, "Unable to store TX log - not fatal", module);
		}
		cart.clear();
		currentTx = null;
	}
	
	public void paidInOut(final String type) {
		trace("paid " + type);
		txLog.set("statusId", "POSTX_PAID_" + type);
		txLog.set("logEndDateTime", UtilDateTime.nowTimestamp());
		try {
			txLog.store();
		} catch (final GenericEntityException e) {
			Debug.logError(e, "Unable to store TX log - not fatal", module);
		}
		currentTx = null;
	}
	
	public void calcTax() {
		try {
			ch.calcAndAddTax(getStoreOrgAddress());
		} catch (final GeneralException e) {
			Debug.logError(e, module);
		}
	}
	
	public void clearTax() {
		cart.removeAdjustmentByType("SALES_TAX");
	}
	
	public int checkPaymentMethodType(final String paymentMethodTypeId) {
		final Map<String, String> fields = UtilMisc.toMap("paymentMethodTypeId", paymentMethodTypeId, "productStoreId", productStoreId);
		List<GenericValue> values = null;
		try {
			values = session.getDelegator().findByAndCache("ProductStorePaymentSetting", fields);
		} catch (final GenericEntityException e) {
			Debug.logError(e, module);
		}
		
		final String externalCode = "PRDS_PAY_EXTERNAL";
		if (UtilValidate.isEmpty(values)) {
			return NO_PAYMENT;
		} else {
			boolean isExternal = true;
			final Iterator<GenericValue> i = values.iterator();
			while (i.hasNext() && isExternal) {
				final GenericValue v = i.next();
				//Debug.logInfo("Testing [" + paymentMethodTypeId + "] - " + v, module);
				if (!externalCode.equals(v.getString("paymentServiceTypeEnumId"))) {
					isExternal = false;
				}
			}
			
			if (isExternal) {
				return EXTERNAL_PAYMENT;
			} else {
				return INTERNAL_PAYMENT;
			}
		}
	}
	
	public BigDecimal addPayment(final String id, final BigDecimal amount) {
		return this.addPayment(id, amount, null, null);
	}
	
	public BigDecimal addPayment(final String id, final BigDecimal amount, final String refNum, final String authCode) {
		trace("added payment", id + "/" + amount);
		if ("CASH".equals(id)) {
			// clear cash payments first; so there is only one
			cart.clearPayment(id);
		}
		cart.addPaymentAmount(id, amount, refNum, authCode, true, true, false);
		return getTotalDue();
	}
	
	public void setPaymentRefNum(final int paymentIndex, final String refNum, final String authCode) {
		trace("setting payment index reference number", paymentIndex + " / " + refNum + " / " + authCode);
		final ShoppingCart.CartPaymentInfo inf = cart.getPaymentInfo(paymentIndex);
		inf.refNum[0] = refNum;
		inf.refNum[1] = authCode;
	}
	
	/* CVV2 code should be entered when a card can't be swiped */
	public void setPaymentSecurityCode(final String paymentId, final String refNum, final String securityCode) {
		trace("setting payment security code", paymentId);
		final int paymentIndex = cart.getPaymentInfoIndex(paymentId, refNum);
		final ShoppingCart.CartPaymentInfo inf = cart.getPaymentInfo(paymentIndex);
		inf.securityCode = securityCode;
		inf.isSwiped = false;
	}
	
	/* Track2 data should be sent to processor when a card is swiped. */
	public void setPaymentTrack2(final String paymentId, final String refNum, final String securityCode) {
		trace("setting payment security code", paymentId);
		final int paymentIndex = cart.getPaymentInfoIndex(paymentId, refNum);
		final ShoppingCart.CartPaymentInfo inf = cart.getPaymentInfo(paymentIndex);
		inf.securityCode = securityCode;
		inf.isSwiped = true;
	}
	
	/* Postal code should be entered when a card can't be swiped */
	public void setPaymentPostalCode(final String paymentId, final String refNum, final String postalCode) {
		trace("setting payment security code", paymentId);
		final int paymentIndex = cart.getPaymentInfoIndex(paymentId, refNum);
		final ShoppingCart.CartPaymentInfo inf = cart.getPaymentInfo(paymentIndex);
		inf.postalCode = postalCode;
	}
	
	public void clearPayments() {
		trace("all payments cleared from sale");
		cart.clearPayments();
	}
	
	public void clearPayment(final int index) {
		trace("removing payment", "" + index);
		cart.clearPayment(index);
	}
	
	public void clearPayment(final String id) {
		trace("removing payment", id);
		cart.clearPayment(id);
	}
	
	public int selectedPayments() {
		return cart.selectedPayments();
	}
	
	public void setTxAsReturn(final String returnId) {
		trace("returned sale");
		txLog.set("statusId", "POSTX_RETURNED");
		txLog.set("returnId", returnId);
		txLog.set("logEndDateTime", UtilDateTime.nowTimestamp());
		try {
			txLog.store();
		} catch (final GenericEntityException e) {
			Debug.logError(e, "Unable to store TX log - not fatal", module);
		}
		cart.clear();
		currentTx = null;
	}
	
	public BigDecimal processSale(final Output output) throws GeneralException {
		trace("process sale");
		final BigDecimal grandTotal = getGrandTotal();
		final BigDecimal paymentAmt = getPaymentTotal();
		if (grandTotal.compareTo(paymentAmt) > 0) {
			throw new IllegalStateException();
		}
		
		// attach the party ID to the cart
		Debug.logInfo("#########################"+partyId, module);

		cart.setOrderPartyId(partyId);
		
		cart.setFacilityId("");
		// Set the shipping type
		cart.setShipmentMethodTypeId("NO_SHIPPING");
		// cart.setCarrierPartyId();
		
		// validate payment methods
		output.print(UtilProperties.getMessage(resource, "PosValidating", locale));
		final Map<String, Object> valRes = ch.validatePaymentMethods();
		if (valRes != null && ServiceUtil.isError(valRes)) {
			throw new GeneralException(ServiceUtil.getErrorMessage(valRes));
		}
		
		// store the "order"
		if (UtilValidate.isEmpty(orderId)) {  // if order does not exist
			output.print(UtilProperties.getMessage(resource, "PosSaving", locale));
			final Map<String, Object> orderRes = ch.createOrder(session.getUserLogin());
			//Debug.logInfo("Create Order Resp : " + orderRes, module);
			
			if (orderRes != null && ServiceUtil.isError(orderRes)) {
				throw new GeneralException(ServiceUtil.getErrorMessage(orderRes));
			} else if (orderRes != null) {
				orderId = (String) orderRes.get("orderId");
			}
		} else { // if the order has already been created
			final Map<?, ?> changeMap = UtilMisc.toMap("itemReasonMap",
					UtilMisc.toMap("reasonEnumId", "EnumIdHere"), // TODO: where does this come from?
					"itemCommentMap", UtilMisc.toMap("changeComments", "change Comments here")); //TODO
			
			final Map<String, Object> svcCtx = FastMap.newInstance();
			svcCtx.put("userLogin", session.getUserLogin());
			svcCtx.put("orderId", orderId);
			svcCtx.put("shoppingCart", cart);
			svcCtx.put("locale", locale);
			svcCtx.put("changeMap", changeMap);
			
			Map<String, Object> svcRes = null;
			try {
				final LocalDispatcher dispatcher = session.getDispatcher();
				svcRes = dispatcher.runSync("saveUpdatedCartToOrder", svcCtx);
			} catch (final GenericServiceException e) {
				Debug.logError(e, module);
				//pos.showDialog("dialog/error/exception", e.getMessage());
				throw new GeneralException(ServiceUtil.getErrorMessage(svcRes));
			}
		}
		
		// process the payment(s)
		output.print(UtilProperties.getMessage(resource, "PosProcessing", locale));
		Map<String, Object> payRes = null;
		try {
			payRes = ch.processPayment(ProductStoreWorker.getProductStore(productStoreId, session.getDelegator()), session.getUserLogin(), true);
		} catch (final GeneralException e) {
			Debug.logError(e, module);
			throw e;
		}
		
		if (payRes != null && ServiceUtil.isError(payRes)) {
			throw new GeneralException(ServiceUtil.getErrorMessage(payRes));
		}
		
		// get the change due
		final BigDecimal change = grandTotal.subtract(paymentAmt);
		
		// notify the change due
		output.print(UtilProperties.getMessage(resource, "PosChange", locale) + " " + UtilFormatOut.formatPrice(getTotalDue().negate()));
		
		// threaded drawer/receipt printing
		final PosTransaction currentTrans = this;
		final SwingWorker worker = new SwingWorker() {
			@Override
			public Object construct() {
				// open the drawer
				currentTrans.popDrawer();
				
				// print the receipt
				DeviceLoader.receipt.printReceipt(currentTrans, true);
				
				return null;
			}
		};
		worker.start();
		
		// save the TX Log
		txLog.set("statusId", "POSTX_SOLD");
		txLog.set("orderId", orderId);
		txLog.set("itemCount", new Long(cart.size()));
		txLog.set("logEndDateTime", UtilDateTime.nowTimestamp());
		try {
			txLog.store();
		} catch (final GenericEntityException e) {
			Debug.logError(e, "Unable to store TX log - not fatal", module);
		}
		
		// clear the tx
		currentTx = null;
		partyId = "_NA_";
		
		return change;
	}
	
	private synchronized GenericValue getStoreOrgAddress() {
		if (shipAddress == null) {
			// locate the store's physical address - use this for tax
			final GenericValue facility = (GenericValue) session.getAttribute("facility");
			if (facility == null) {
				return null;
			}
			
			final Delegator delegator = session.getDelegator();
			final GenericValue facilityContactMech = ContactMechWorker.getFacilityContactMechByPurpose(delegator, facilityId, UtilMisc.toList("SHIP_ORIG_LOCATION", "PRIMARY_LOCATION"));
			if (facilityContactMech != null) {
				try {
					shipAddress = session.getDelegator().findByPrimaryKey("PostalAddress",
							UtilMisc.toMap("contactMechId", facilityContactMech.getString("contactMechId")));
				} catch (final GenericEntityException e) {
					Debug.logError(e, module);
				}
			}
		}
		return shipAddress;
	}
	
	public void saveTx() {
		savedTx.push(this);
		currentTx = null;
		trace("transaction saved");
	}
	
	public void appendItemDataModel(final XModel model) {
		if (cart != null) {
			final Iterator<?> i = cart.iterator();
			while (i.hasNext()) {
				final ShoppingCartItem item = (ShoppingCartItem) i.next();
				final BigDecimal quantity = item.getQuantity();
				final BigDecimal unitPrice = item.getBasePrice();
				final BigDecimal subTotal = unitPrice.multiply(quantity);
				final BigDecimal adjustment = item.getOtherAdjustments();
				
				final XModel line = Journal.appendNode(model, "tr", "" + cart.getItemIndex(item), "");
				Journal.appendNode(line, "td", "sku", item.getProductId());
				Journal.appendNode(line, "td", "desc", item.getName());
				Journal.appendNode(line, "td", "qty", UtilFormatOut.formatQuantity(quantity));
				Journal.appendNode(line, "td", "price", UtilFormatOut.formatPrice(subTotal));
				Journal.appendNode(line, "td", "index", Integer.toString(cart.getItemIndex(item)));
				
				if (isAggregatedItem(item.getProductId())) {
					// put alterations here
					ProductConfigWrapper pcw = null;
					// product = delegator.findByPrimaryKeyCache("Product", UtilMisc.toMap("productId", productId));
					// pcw = new ProductConfigWrapper(delegator, session.getDispatcher(), productId, null, null, null, null, null, null);
					pcw = item.getConfigWrapper();
					final List<ConfigOption> selected = pcw.getSelectedOptions();
					for (final ConfigOption configoption : selected) {
						if (configoption.isSelected()) {
							final XModel option = Journal.appendNode(model, "tr", "" + cart.getItemIndex(item), "");
							Journal.appendNode(option, "td", "sku", "");
							Journal.appendNode(option, "td", "desc", configoption.getDescription());
							Journal.appendNode(option, "td", "qty", "");
							Journal.appendNode(option, "td", "price", UtilFormatOut.formatPrice(configoption.getPrice()));
							Journal.appendNode(option, "td", "index", Integer.toString(cart.getItemIndex(item)));
						}
					}
				}
				if (isMarketingPackage(item.getProductId())) {
					final ArrayList<MarketingCartItemInfo> bundleInfo = getMarketingPackageDetails(item.getProductId());
					for (final MarketingCartItemInfo bundleItem : bundleInfo) {
						final XModel option = Journal.appendNode(model, "tr", "" + cart.getItemIndex(item), "");
						Debug.logInfo("############################ is a marketting product" + bundleItem.productSku, module);
						Journal.appendNode(option, "td", "sku", bundleItem.productSku);
						Journal.appendNode(option, "td", "desc", bundleItem.productName);
						Journal.appendNode(option, "td", "qty", bundleItem.productQty);
						Journal.appendNode(option, "td", "price", bundleItem.productPrice);
						Journal.appendNode(option, "td", "index", Integer.toString(cart.getItemIndex(item)));
					}
				}
				
				if (adjustment.compareTo(BigDecimal.ZERO) != 0) {
					// append the promo info
					final XModel promo = Journal.appendNode(model, "tr", "itemadjustment", "");
					Journal.appendNode(promo, "td", "sku", "");
					Journal.appendNode(promo, "td", "desc", UtilProperties.getMessage(resource, "PosItemDiscount", locale));
					Journal.appendNode(promo, "td", "qty", "");
					Journal.appendNode(promo, "td", "price", UtilFormatOut.formatPrice(adjustment));
				}
			}
		}
	}
	
	public void appendTotalDataModel(final XModel model) {
		if (cart != null) {
			final BigDecimal taxAmount = cart.getTotalSalesTax();
			final BigDecimal total = cart.getGrandTotal();
			final List<GenericValue> adjustments = cart.getAdjustments();
			BigDecimal itemsAdjustmentsAmount = BigDecimal.ZERO;
			
			final Iterator<?> i = cart.iterator();
			while (i.hasNext()) {
				final ShoppingCartItem item = (ShoppingCartItem) i.next();
				final BigDecimal adjustment = item.getOtherAdjustments();
				if (adjustment.compareTo(BigDecimal.ZERO) != 0) {
					itemsAdjustmentsAmount = itemsAdjustmentsAmount.add(adjustment);
				}
			}
			
			for (final GenericValue orderAdjustment : adjustments) {
				BigDecimal amount = orderAdjustment.getBigDecimal("amount");
				final BigDecimal sourcePercentage = orderAdjustment.getBigDecimal("sourcePercentage");
				final XModel adjustmentLine = Journal.appendNode(model, "tr", "adjustment", "");
				Journal.appendNode(adjustmentLine, "td", "sku", "");
				Journal.appendNode(adjustmentLine, "td", "desc",
						UtilProperties.getMessage(resource, "PosSalesDiscount", locale));
				if (UtilValidate.isNotEmpty(amount)) {
					Journal.appendNode(adjustmentLine, "td", "qty", "");
					Journal.appendNode(adjustmentLine, "td", "price", UtilFormatOut.formatPrice(amount));
				} else if (UtilValidate.isNotEmpty(sourcePercentage)) {
					final BigDecimal percentage = sourcePercentage.movePointLeft(2).negate(); // sourcePercentage is negative and must be show as a positive value (it's a discount not an amount)
					Journal.appendNode(adjustmentLine, "td", "qty", UtilFormatOut.formatPercentage(percentage));
					amount = cart.getItemTotal().add(itemsAdjustmentsAmount).multiply(percentage); // itemsAdjustmentsAmount is negative
					Journal.appendNode(adjustmentLine, "td", "price", UtilFormatOut.formatPrice(amount.negate())); // amount must be shown as a negative value
				}
				Journal.appendNode(adjustmentLine, "td", "index", "-1");
			}
			
			final XModel taxLine = Journal.appendNode(model, "tr", "tax", "");
			Journal.appendNode(taxLine, "td", "sku", "");
			
			Journal.appendNode(taxLine, "td", "desc", UtilProperties.getMessage(resource, "PosSalesTax", locale));
			Journal.appendNode(taxLine, "td", "qty", "");
			Journal.appendNode(taxLine, "td", "price", UtilFormatOut.formatPrice(taxAmount));
			Journal.appendNode(taxLine, "td", "index", "-1");
			
			final XModel totalLine = Journal.appendNode(model, "tr", "total", "");
			Journal.appendNode(totalLine, "td", "sku", "");
			Journal.appendNode(totalLine, "td", "desc", UtilProperties.getMessage(resource, "PosGrandTotal", locale));
			Journal.appendNode(totalLine, "td", "qty", "");
			Journal.appendNode(totalLine, "td", "price", UtilFormatOut.formatPrice(total));
			Journal.appendNode(totalLine, "td", "index", "-1");
		}
	}
	
	public void appendPaymentDataModel(final XModel model) {
		if (cart != null) {
			final int paymentInfoSize = cart.selectedPayments();
			for (int i = 0; i < paymentInfoSize; i++) {
				final ShoppingCart.CartPaymentInfo inf = cart.getPaymentInfo(i);
				final GenericValue paymentInfoObj = inf.getValueObject(session.getDelegator());
				
				GenericValue paymentMethodType = null;
				GenericValue paymentMethod = null;
				if ("PaymentMethod".equals(paymentInfoObj.getEntityName())) {
					paymentMethod = paymentInfoObj;
					try {
						paymentMethodType = paymentMethod.getRelatedOne("PaymentMethodType");
					} catch (final GenericEntityException e) {
						Debug.logError(e, module);
					}
				} else {
					paymentMethodType = paymentInfoObj;
				}
				
				final Object desc = paymentMethodType != null ? paymentMethodType.get("description", locale) : "??";
				final String descString = desc.toString();
				BigDecimal amount = BigDecimal.ZERO;
				if (inf.amount == null) {
					amount = cart.getGrandTotal().subtract(cart.getPaymentTotal());
				} else {
					amount = inf.amount;
				}
				
				final XModel paymentLine = Journal.appendNode(model, "tr", Integer.toString(i), "");
				Journal.appendNode(paymentLine, "td", "sku", "");
				Journal.appendNode(paymentLine, "td", "desc", descString);
				Journal.appendNode(paymentLine, "td", "qty", "-");
				Journal.appendNode(paymentLine, "td", "price", UtilFormatOut.formatPrice(amount.negate()));
				Journal.appendNode(paymentLine, "td", "index", Integer.toString(i));
			}
		}
	}
	
	public void appendChangeDataModel(final XModel model) {
		if (cart != null) {
			final BigDecimal changeDue = getTotalDue().negate();
			if (changeDue.compareTo(BigDecimal.ZERO) >= 0) {
				final XModel changeLine = Journal.appendNode(model, "tr", "", "");
				Journal.appendNode(changeLine, "td", "sku", "");
				Journal.appendNode(changeLine, "td", "desc", "Change");
				Journal.appendNode(changeLine, "td", "qty", "-");
				Journal.appendNode(changeLine, "td", "price", UtilFormatOut.formatPrice(changeDue));
			}
		}
	}
	
	public String makeCreditCardVo(final String cardNumber, final String expDate, final String firstName, final String lastName) {
		final LocalDispatcher dispatcher = session.getDispatcher();
		final String expMonth = expDate.substring(0, 2);
		String expYear = expDate.substring(2);
		// two digit year check -- may want to re-think this
		if (expYear.length() == 2) {
			expYear = "20" + expYear;
		}
		
		final Map<String, Object> svcCtx = FastMap.newInstance();
		svcCtx.put("userLogin", session.getUserLogin());
		svcCtx.put("partyId", partyId);
		svcCtx.put("cardNumber", cardNumber);
		svcCtx.put("firstNameOnCard", firstName == null ? "" : firstName);
		svcCtx.put("lastNameOnCard", lastName == null ? "" : lastName);
		svcCtx.put("expMonth", expMonth);
		svcCtx.put("expYear", expYear);
		svcCtx.put("cardType", UtilValidate.getCardType(cardNumber));
		
		//Debug.logInfo("Create CC : " + svcCtx, module);
		Map<String, Object> svcRes = null;
		try {
			svcRes = dispatcher.runSync("createCreditCard", svcCtx);
		} catch (final GenericServiceException e) {
			Debug.logError(e, module);
			return null;
		}
		if (ServiceUtil.isError(svcRes)) {
			Debug.logError(ServiceUtil.getErrorMessage(svcRes) + " - " + svcRes, module);
			return null;
		} else {
			return (String) svcRes.get("paymentMethodId");
		}
	}
	
	public GenericValue getTerminalState() {
		final Delegator delegator = session.getDelegator();
		List<GenericValue> states = null;
		try {
			states = delegator.findByAnd("PosTerminalState", UtilMisc.toMap("posTerminalId", getTerminalId()));
		} catch (final GenericEntityException e) {
			Debug.logError(e, module);
		}
		states = EntityUtil.filterByDate(states, UtilDateTime.nowTimestamp(), "openedDate", "closedDate", true);
		return EntityUtil.getFirst(states);
	}
	
	public void setPrintWriter(final PrintWriter writer) {
		trace = writer;
	}
	
	private void trace(final String s) {
		trace(s, null, null);
	}
	
	private void trace(final String s, final Throwable t) {
		trace(s, null, t);
	}
	
	private void trace(final String s1, final String s2) {
		trace(s1, s2, null);
	}
	
	private void trace(final String s1, final String s2, final Throwable t) {
		if (trace != null) {
			String msg = s1;
			if (UtilValidate.isNotEmpty(s2)) {
				msg = msg + "(" + s2 + ")";
			}
			if (t != null) {
				msg = msg + " : " + t.getMessage();
			}
			
			// print the trace line
			trace.println("[POS @ " + terminalId + " TX:" + transactionId + "] - " + msg);
			trace.flush();
		}
	}
	
	public static synchronized PosTransaction getCurrentTx(final XuiSession session) {
		if (currentTx == null) {
			if (session.getUserLogin() != null) {
				currentTx = new PosTransaction(session);
			}
		}
		return currentTx;
	}
	
	public void loadSale(final PosScreen pos) {
		trace("Load a sale");
		final List<GenericValue> shoppingLists = createShoppingLists();
		if (!shoppingLists.isEmpty()) {
			final Map<String, String> salesMap = createSalesMap(shoppingLists);
			if (!salesMap.isEmpty()) {
				final LoadSale loadSale = new LoadSale(salesMap, this, pos);
				loadSale.openDlg();
			}
			else {
				pos.showDialog("dialog/error/nosales");
			}
		} else {
			pos.showDialog("dialog/error/nosales");
		}
	}
	
	public void loadOrder(final PosScreen pos) {
		final List<GenericValue> orders = findOrders();
		if (!orders.isEmpty()) {
			final LoadSale loadSale = new LoadSale(createOrderHash(orders), this, pos);
			loadSale.openDlg();
		} else {
			pos.showDialog("dialog/error/nosales");
		}
	}
	
	private List<GenericValue> findOrders() {
		final LocalDispatcher dispatcher = session.getDispatcher();
		
		final Map<String, Object> svcCtx = FastMap.newInstance();
		svcCtx.put("userLogin", session.getUserLogin());
		svcCtx.put("partyId", partyId);
		final List<String> orderStatusIds = FastList.newInstance();
		orderStatusIds.add("ORDER_CREATED");
		svcCtx.put("orderStatusId", orderStatusIds);
		svcCtx.put("viewIndex", 1);
		svcCtx.put("viewSize", 25);
		svcCtx.put("showAll", "Y");
		
		Map<String, Object> svcRes = null;
		try {
			svcRes = dispatcher.runSync("findOrders", svcCtx);
		} catch (final GenericServiceException e) {
			Debug.logError(e, module);
		}
		
		if (svcRes == null) {
			Debug.logInfo(UtilProperties.getMessage("EcommerceUiLabels", "EcommerceNoShoppingListsCreate", locale), module);
		} else if (ServiceUtil.isError(svcRes)) {
			Debug.logError(ServiceUtil.getErrorMessage(svcRes) + " - " + svcRes, module);
		} else {
			final Integer orderListSize = (Integer) svcRes.get("orderListSize");
			if (orderListSize > 0) {
				final List<GenericValue> orderList = UtilGenerics.checkList(svcRes.get("orderList"), GenericValue.class);
				return orderList;
			}
		}
		return null;
	}
	
	/*    public void configureItem(String cartIndex, PosScreen pos) {
	     trace("configure item", cartIndex);
	     try {
	        int index = Integer.parseInt(cartIndex);
	        ShoppingCartItem product = cart.findCartItem(index);
	        Delegator delegator = cart.getDelegator();
	        ProductConfigWrapper pcw = null;
	        pcw = product.getConfigWrapper();
	        if (pcw != null) {
	            ConfigureItem configItem = new ConfigureItem(cartIndex, pcw, this, pos);
	            configItem.openDlg();
	        }
	        else {
	            pos.showDialog("dialog/error/itemnotconfigurable");
	        }
	    } catch (Exception e) {
	        trace("general exception", e);
	        Debug.logError(e, module);
	    }
	}  */
	
	public List<GenericValue> createShoppingLists() {
		List<GenericValue> shoppingLists = null;
		final Delegator delegator = session.getDelegator();
		try {
			shoppingLists = delegator.findList("ShoppingList", null, null, null, null, false);
		} catch (final GenericEntityException e) {
			Debug.logError(e, module);
			return null;
		}
		
		if (shoppingLists == null) {
			Debug.logInfo(UtilProperties.getMessage("EcommerceUiLabels", "EcommerceNoShoppingListsCreate", locale), module);
		}
		return shoppingLists;
	}
	
	public Map<String, String> createSalesMap(final List<GenericValue> shoppingLists) {
		final Map<String, String> salesMap = FastMap.newInstance();
		for (final GenericValue shoppingList : shoppingLists) {
			List<GenericValue> items = null;
			try {
				items = shoppingList.getRelated("ShoppingListItem", UtilMisc.toList("shoppingListItemSeqId"));
			} catch (final GenericEntityException e) {
				Debug.logError(e, module);
			}
			if (UtilValidate.isNotEmpty(items)) {
				final String listName = shoppingList.getString("listName");
				final String shoppingListId = shoppingList.getString("shoppingListId");
				salesMap.put(shoppingListId, listName);
			}
		}
		return salesMap;
	}
	
	public Map<String, String> createOrderHash(final List<GenericValue> orders) {
		final Map<String, String> hash = FastMap.newInstance();
		for (final GenericValue order : orders) {
			final String orderName = order.getString("orderName");
			final String orderId = order.getString("orderId");
			if (orderName != null) {
				hash.put(orderId, orderName);
			}
		}
		return hash;
	}
	
	public boolean addListToCart(final String shoppingListId, final PosScreen pos, final boolean append) {
		trace("Add list to cart");
		final Delegator delegator = session.getDelegator();
		final LocalDispatcher dispatcher = session.getDispatcher();
		final String includeChild = null; // Perhaps will be used later ...
		final String prodCatalogId = null;
		
		try {
			ShoppingListEvents.addListToCart(delegator, dispatcher, cart, prodCatalogId, shoppingListId, (includeChild != null), true, append);
		} catch (final IllegalArgumentException e) {
			Debug.logError(e, module);
			pos.showDialog("dialog/error/exception", e.getMessage());
			return false;
		}
		return true;
	}
	public boolean validateQuote (String quoteId) {
		boolean validQuote = false;
		try {
			GenericValue quote = session.getDelegator().findByPrimaryKey("Quote", UtilMisc.toMap("quoteId", quoteId));
			if (UtilValidate.isNotEmpty(quote)) {
				Timestamp quoteThrudate = quote.getTimestamp("validThruDate");
				if (UtilDateTime.nowTimestamp().after(quoteThrudate)) {
					
				}
				validQuote = true;
			}
		} catch (GenericEntityException e) {
			Debug.logError(e, module);
		}
		
	return validQuote;	
	}
	public boolean checkQuoteExpireDate (String quoteId) {
		boolean validQuote = false;
		try {
			GenericValue quote = session.getDelegator().findByPrimaryKey("Quote", UtilMisc.toMap("quoteId", quoteId));
			if (UtilValidate.isNotEmpty(quote)) {
				Timestamp quoteThrudate = quote.getTimestamp("validThruDate");
				if (UtilDateTime.nowTimestamp().before(quoteThrudate)) {
					validQuote = true;
				}
			}
		} catch (GenericEntityException e) {
			Debug.logError(e, module);
		}
		
	return validQuote;	
	}
	public void loadCartFromQuote(String quoteId) {
		final LocalDispatcher dispatcher = session.getDispatcher();
		final Map<String, Object> svcCtx = FastMap.newInstance();
		svcCtx.put("userLogin", session.getUserLogin());
		svcCtx.put("quoteId", quoteId);
		svcCtx.put("applyQuoteAdjustments", "true");
		Map<String, Object> outMap = null;
		try {
			outMap = dispatcher.runSync("loadCartFromQuote", svcCtx);
			cart.setFacilityId("MyRetailStore");
			cart.setChannelType("POS_SALES_CHANNEL");
			cart.setTransactionId(transactionId);
			cart.setTerminalId(terminalId);
			cart.setProductStoreId(productStoreId);
			Debug.logInfo("#########################"+cart.getProductStoreId(), module);
			Debug.logInfo("#########################"+cart.getProductStoreId(), module);
			cart = (ShoppingCart) outMap.get("shoppingCart");
			ch = new CheckOutHelper(session.getDispatcher(), session.getDelegator(), cart);
			if (session.getUserLogin() != null) {
				cart.setOrderPartyId(session.getUserLogin().getString("partyId"));
				this.partyId = session.getUserLogin().getString("partyId");
			}
			Debug.logInfo("#########################"+this.productStoreId, module);
			Debug.logInfo("#########################"+cart.getFacilityId(), module);
			Debug.logInfo("#########################"+cart.getTotalQuantity(), module);

		} catch (GenericServiceException e) {
			e.printStackTrace();
		}
	}
	public boolean restoreOrder(final String orderId, final PosScreen pos, final boolean append) {
		trace("Restore an order");
		
		final LocalDispatcher dispatcher = session.getDispatcher();
		
		final Map<String, Object> svcCtx = FastMap.newInstance();
		svcCtx.put("userLogin", session.getUserLogin());
		svcCtx.put("orderId", orderId);
		svcCtx.put("skipInventoryChecks", Boolean.TRUE);
		svcCtx.put("skipProductChecks", Boolean.TRUE);
		
		Map<String, Object> svcRes = null;
		try {
			svcRes = dispatcher.runSync("loadCartFromOrder", svcCtx);
		} catch (final GenericServiceException e) {
			Debug.logError(e, module);
			pos.showDialog("dialog/error/exception", e.getMessage());
		}
		
		if (svcRes == null) {
			Debug.logInfo(UtilProperties.getMessage("EcommerceUiLabels", "EcommerceNoShoppingListsCreate", locale), module);
		} else if (ServiceUtil.isError(svcRes)) {
			Debug.logError(ServiceUtil.getErrorMessage(svcRes) + " - " + svcRes, module);
		} else {
			final ShoppingCart restoredCart = (ShoppingCart) svcRes.get("shoppingCart");
			if (append) {
				// TODO: add stuff to append items
				cart = restoredCart;
				this.orderId = orderId;
			} else {
				cart = restoredCart;
				this.orderId = orderId;
			}
			ch = new CheckOutHelper(session.getDispatcher(), session.getDelegator(), cart);
//			if (session.getUserLogin() != null) {
//				cart.addAdditionalPartyRole(session.getUserLogin().getString("partyId"), "SALES_REP");
//			}
			cart.setFacilityId(facilityId);
			cart.setTerminalId(terminalId);
			cart.setOrderId(orderId);
			return true;
		}
		return false;
	}
	
	public boolean clearList(final String shoppingListId, final PosScreen pos) {
		final Delegator delegator = session.getDelegator();
		try {
			ShoppingListEvents.clearListInfo(delegator, shoppingListId);
		} catch (final GenericEntityException e) {
			Debug.logError(e, module);
			pos.showDialog("dialog/error/exception", e.getMessage());
			return false;
		}
		return true;
	}
	
	public void clientProfile(final PosScreen pos) {
		final ClientProfile clientProfile = new ClientProfile(this, pos);
		clientProfile.openDlg();
	}
	
	public void saveSale(final PosScreen pos) {
		final SaveSale saveSale = new SaveSale(this, pos);
		saveSale.openDlg();
	}
	
	public void saveOrder(final String shoppingListName, final PosScreen pos) {
		trace("Save an order");
		if (cart.size() == 0) {
			pos.showDialog("dialog/error/exception", UtilProperties.getMessage("OrderErrorUiLabels", "OrderUnableToCreateNewShoppingList", locale));
			return;
		}
		if (!UtilValidate.isEmpty(shoppingListName)) {
			// attach the party ID to the cart
			cart.setOrderPartyId(partyId);
			cart.setOrderName(shoppingListName);
			//cart.setExternalId(shoppingListName);
			//cart.setInternalCode("Internal Code");
			//ch.setCheckOutOptions(null, null, null, null, null, "shipping instructions", null, null, null, "InternalId", null, null, null);
			final Map<String, Object> orderRes = ch.createOrder(session.getUserLogin());
			
			if (orderRes != null && ServiceUtil.isError(orderRes)) {
				Debug.logError(ServiceUtil.getErrorMessage(orderRes), module);
				//throw new GeneralException(ServiceUtil.getErrorMessage(orderRes));
			} else if (orderRes != null) {
				orderId = (String) orderRes.get("orderId");
			}
		}
	}
	
	public void saveSale(final String shoppingListName, final PosScreen pos) {
		trace("Save a sale");
		if (cart.size() == 0) {
			pos.showDialog("dialog/error/exception", UtilProperties.getMessage("OrderErrorUiLabels", "OrderUnableToCreateNewShoppingList", locale));
			return;
		}
		final Delegator delegator = session.getDelegator();
		final LocalDispatcher dispatcher = session.getDispatcher();
		final GenericValue userLogin = session.getUserLogin();
		String shoppingListId = null;
		
		if (!UtilValidate.isEmpty(shoppingListName)) {
			// create a new shopping list with partyId = user connected (POS clerk, etc.) and not buyer (_NA_ in POS)
			final Map<String, Object> serviceCtx = UtilMisc.toMap("userLogin", session.getUserLogin(), "partyId", session.getUserPartyId(),
					"productStoreId", productStoreId, "listName", shoppingListName);
			
			serviceCtx.put("shoppingListTypeId", "SLT_SPEC_PURP");
			Map<String, Object> newListResult = null;
			try {
				
				newListResult = dispatcher.runSync("createShoppingList", serviceCtx);
			} catch (final GenericServiceException e) {
				Debug.logError(e, "Problem while creating new ShoppingList", module);
				pos.showDialog("dialog/error/exception", UtilProperties.getMessage("OrderErrorUiLabels", "OrderUnableToCreateNewShoppingList", locale));
				return;
			}
			
			// check for errors
			if (ServiceUtil.isError(newListResult)) {
				final String error = ServiceUtil.getErrorMessage(newListResult);
				Debug.logError(error, module);
				pos.showDialog("dialog/error/exception", error);
				return;
			}
			
			// get the new list id
			if (newListResult != null) {
				shoppingListId = (String) newListResult.get("shoppingListId");
			} else {
				Debug.logError("Problem while creating new ShoppingList", module);
				pos.showDialog("dialog/error/exception", UtilProperties.getMessage("OrderErrorUiLabels", "OrderUnableToCreateNewShoppingList", locale));
				return;
			}
		}
		
		final String selectedCartItems[] = new String[cart.size()];
		for (int i = 0; i < cart.size(); i++) {
			final Integer integer = i;
			selectedCartItems[i] = integer.toString();
		}
		
		try {
			ShoppingListEvents.addBulkFromCart(delegator, dispatcher, cart, userLogin, shoppingListId, null, selectedCartItems, true, true);
		} catch (final IllegalArgumentException e) {
			Debug.logError(e, "Problem while creating new ShoppingList", module);
			pos.showDialog("dialog/error/exception", UtilProperties.getMessage("OrderErrorUiLabels", "OrderUnableToCreateNewShoppingList", locale));
		}
	}
	
	public String addProductPromoCode(final String code) {
		trace("Add a promo code");
		final LocalDispatcher dispatcher = session.getDispatcher();
		final String result = cart.addProductPromoCode(code, dispatcher);
		calcTax();
		return result;
	}
	
	private List<Map<String, String>> searchContactMechs(final Delegator delegator, final PosScreen pos, final List<Map<String, String>> partyList, final String valueToCompare, final String contactMechType) {
		final ListIterator<Map<String, String>> partyListIt = partyList.listIterator();
		while (partyListIt.hasNext()) {
			final Map<String, String> party = partyListIt.next();
			final String partyId = party.get("partyId");
			final List<Map<String, Object>> partyContactMechValueMaps = ContactMechWorker.getPartyContactMechValueMaps(delegator, partyId, false, contactMechType);
			Integer nb = 0;
			for (final Map<String, Object> partyContactMechValueMap : partyContactMechValueMaps) {
				nb++;
				String keyType = null;
				String key = null;
				if ("TELECOM_NUMBER".equals(contactMechType)) {
					keyType = "telecomNumber";
					key = "contactNumber";
				} else if ("EMAIL_ADDRESS".equals(contactMechType)) {
					keyType = "contactMech";
					key = "infoString";
				}
				final Map<String, Object> keyTypeMap = UtilGenerics.checkMap(partyContactMechValueMap.get(keyType));
				final String keyTypeValue = ((String) keyTypeMap.get(key)).trim();
				if (valueToCompare.equals(keyTypeValue) || UtilValidate.isEmpty(valueToCompare)) {
					if (nb == 1) {
						party.put(key, keyTypeValue);
						partyListIt.set(party);
					} else {
						final Map<String, String> partyClone = FastMap.newInstance();
						partyClone.putAll(party);
						partyClone.put(key, keyTypeValue);
						partyListIt.add(partyClone);
					}
				}
			}
		}
		return partyList;
	}
	
	public List<Map<String, String>> searchClientProfile(final String name, final String email, final String phone, final String card, final PosScreen pos, final Boolean equalsName) {
		final Delegator delegator = session.getDelegator();
		
		List<GenericValue> partyList = null;
		List<Map<String, String>> resultList = null;
		
		// create the dynamic view entity
		final DynamicViewEntity dynamicView = new DynamicViewEntity();
		
		// Person (name + card)
		dynamicView.addMemberEntity("PT", "Party");
		dynamicView.addAlias("PT", "partyId");
		dynamicView.addAlias("PT", "statusId");
		dynamicView.addAlias("PT", "partyTypeId");
		dynamicView.addMemberEntity("PE", "Person");
		dynamicView.addAlias("PE", "partyId");
		dynamicView.addAlias("PE", "lastName");
		dynamicView.addAlias("PE", "cardId");
		dynamicView.addViewLink("PT", "PE", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId"));
		
		if (UtilValidate.isNotEmpty(email)) {
			// ContactMech (email)
			dynamicView.addMemberEntity("PM", "PartyContactMechPurpose");
			dynamicView.addAlias("PM", "contactMechId");
			dynamicView.addAlias("PM", "contactMechPurposeTypeId");
			dynamicView.addAlias("PM", "thruDate");
			dynamicView.addMemberEntity("CM", "ContactMech");
			dynamicView.addAlias("CM", "infoString");
			dynamicView.addViewLink("PT", "PM", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId"));
			dynamicView.addViewLink("PM", "CM", Boolean.FALSE, ModelKeyMap.makeKeyMapList("contactMechId"));
		} else if (UtilValidate.isNotEmpty(phone)) {
			dynamicView.addMemberEntity("PM", "PartyContactMechPurpose");
			dynamicView.addAlias("PM", "contactMechId");
			dynamicView.addAlias("PM", "thruDate");
			dynamicView.addAlias("PM", "contactMechPurposeTypeId");
			dynamicView.addMemberEntity("TN", "TelecomNumber");
			dynamicView.addAlias("TN", "contactNumber");
			dynamicView.addViewLink("PT", "PM", Boolean.FALSE, ModelKeyMap.makeKeyMapList("partyId"));
			dynamicView.addViewLink("PM", "TN", Boolean.FALSE, ModelKeyMap.makeKeyMapList("contactMechId"));
		}
		
		// define the main condition & expression list
		final List<EntityCondition> andExprs = FastList.newInstance();
		EntityCondition mainCond = null;
		
		final List<String> orderBy = FastList.newInstance();
		final List<String> fieldsToSelect = FastList.newInstance();
		// fields we need to select; will be used to set distinct
		fieldsToSelect.add("partyId");
		fieldsToSelect.add("lastName");
		fieldsToSelect.add("cardId");
		if (UtilValidate.isNotEmpty(email)) {
			fieldsToSelect.add("infoString");
		} else if (UtilValidate.isNotEmpty(phone)) {
			fieldsToSelect.add("contactNumber");
		}
		
		// NOTE: _must_ explicitly allow null as it is not included in a not equal in many databases... odd but true
		// This allows to get all clients when any informations has been entered
		andExprs.add(EntityCondition.makeCondition(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, null), EntityOperator.OR, EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "PARTY_DISABLED")));
		andExprs.add(EntityCondition.makeCondition("partyTypeId", EntityOperator.EQUALS, "PERSON")); // Only persons for now...
		if (UtilValidate.isNotEmpty(name)) {
			if (equalsName) {
				andExprs.add(EntityCondition.makeCondition("lastName", EntityOperator.EQUALS, name));  // Plain name
			} else {
				// andExprs.add(EntityCondition.makeCondition("lastName", EntityOperator.LIKE, "%"+name+"%")); // Less restrictive
				andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("lastName"), EntityOperator.LIKE, EntityFunction.UPPER("%" + name + "%"))); // Even less restrictive
			}
		}
		if (UtilValidate.isNotEmpty(card)) {
			andExprs.add(EntityCondition.makeCondition("cardId", EntityOperator.EQUALS, card));
		}
		if (UtilValidate.isNotEmpty(email)) {
			andExprs.add(EntityCondition.makeCondition("infoString", EntityOperator.EQUALS, email));
			andExprs.add(EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.EQUALS, "PRIMARY_EMAIL"));
			andExprs.add(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null));
		} else if (UtilValidate.isNotEmpty(phone)) {
			andExprs.add(EntityCondition.makeCondition("contactNumber", EntityOperator.EQUALS, phone));
			andExprs.add(EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.EQUALS, "PHONE_HOME"));
			andExprs.add(EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null));
		}
		
		mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);
		orderBy.add("lastName");
		
		Debug.logInfo("In searchClientProfile mainCond=" + mainCond, module);
		
		final Integer maxRows = Integer.MAX_VALUE;
		// attempt to start a transaction
		boolean beganTransaction = false;
		try {
			beganTransaction = TransactionUtil.begin();
			
			try {
				// set distinct on so we only get one row per person
				final EntityFindOptions findOpts = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, -1, maxRows, true);
				// using list iterator
				final EntityListIterator pli = delegator.findListIteratorByCondition(dynamicView, mainCond, null, fieldsToSelect, orderBy, findOpts);
				
				// get the partial list for this page
				partyList = pli.getPartialList(1, maxRows);
				
				// close the list iterator
				pli.close();
			} catch (final GenericEntityException e) {
				Debug.logError(e, module);
				pos.showDialog("dialog/error/exception", e.getMessage());
			}
		} catch (final GenericTransactionException e) {
			Debug.logError(e, module);
			try {
				TransactionUtil.rollback(beganTransaction, e.getMessage(), e);
			} catch (final GenericTransactionException e2) {
				Debug.logError(e2, "Unable to rollback transaction", module);
				pos.showDialog("dialog/error/exception", e2.getMessage());
			}
			pos.showDialog("dialog/error/exception", e.getMessage());
		} finally {
			try {
				TransactionUtil.commit(beganTransaction);
			} catch (final GenericTransactionException e) {
				Debug.logError(e, "Unable to commit transaction", module);
				pos.showDialog("dialog/error/exception", e.getMessage());
			}
		}
		
		if (partyList != null) {
			resultList = FastList.newInstance();
			for (final GenericValue party : partyList) {
				final Map<String, String> partyMap = FastMap.newInstance();
				partyMap.put("partyId", party.getString("partyId"));
				partyMap.put("lastName", party.getString("lastName"));
				partyMap.put("cardId", party.getString("cardId"));
				if (UtilValidate.isNotEmpty(email)) {
					partyMap.put("infoString", party.getString("infoString"));
					partyMap.put("contactNumber", "");
				} else if (UtilValidate.isNotEmpty(phone)) {
					partyMap.put("infoString", "");
					partyMap.put("contactNumber", party.getString("contactNumber"));
				} else { // both empty
					partyMap.put("infoString", "");
					partyMap.put("contactNumber", "");
					
				}
				resultList.add(partyMap);
			}
			
			if (UtilValidate.isNotEmpty(email)) {
				resultList = searchContactMechs(delegator, pos, resultList, phone, "TELECOM_NUMBER");
			} else if (UtilValidate.isNotEmpty(phone)) {
				resultList = searchContactMechs(delegator, pos, resultList, "", "EMAIL_ADDRESS"); // "" is more clear than email which is by definition here is empty
			} else { // both empty
				resultList = searchContactMechs(delegator, pos, resultList, "", "TELECOM_NUMBER"); // "" is more clear than phone which is by definition here is empty
				resultList = searchContactMechs(delegator, pos, resultList, "", "EMAIL_ADDRESS");
			}
		} else {
			resultList = FastList.newInstance();
		}
		return resultList;
	}
	
	public String createClientProfile(final String name, final String phone, final String salesRep, final PosScreen pos) {
		
		// We suppose here that a cardId (card number) can only belongs to one person (it's used as owned PromoCode)
		// We use the 1st party's login (it may change and be multiple since it depends on email and card)
		// We suppose only one email address (should be ok anyway because of the contactMechPurposeTypeId == "PRIMARY_EMAIL")
		// we suppose only one phone number (should be ok anyway because of the contactMechPurposeTypeId == "PHONE_HOME")
		final LocalDispatcher dispatcher = session.getDispatcher();
		final GenericValue userLogin = session.getUserLogin();
		GenericValue partyUserLogin = null;
		String result = null;
		String emailContact = null;
		String phoneContact = null;
		final String email = null;
		
		final Map<String, Object> svcCtx = FastMap.newInstance();
		Map<String, Object> svcRes = null;
		if (UtilValidate.isNotEmpty(salesRep)) {
			cart.addAdditionalPartyRole(salesRep, "SALES_REP");
		}
		// Create
		svcCtx.put("userLogin", userLogin);
		svcCtx.put("lastName", name);
		svcCtx.put("firstName", ""); // Needed by service createPersonAndUserLogin
		if (UtilValidate.isNotEmpty(email) && UtilValidate.isNotEmpty(phone)) {
			svcCtx.put("userLoginId", email);
			svcCtx.put("currentPassword", phone);
			svcCtx.put("currentPasswordVerify", phone);
			try {
				svcRes = dispatcher.runSync("createPersonAndUserLogin", svcCtx);
			} catch (final GenericServiceException e) {
				Debug.logError(e, module);
				pos.showDialog("dialog/error/exception", e.getMessage());
				return null;
			}
			if (ServiceUtil.isError(svcRes)) {
				pos.showDialog("dialog/error/exceptionLargeSmallFont", ServiceUtil.getErrorMessage(svcRes)); // exceptionLargeSmallFont used to show duplicate key error message for card
				return null;
			}
			partyId = (String) svcRes.get("partyId");
			partyUserLogin = (GenericValue) svcRes.get("newUserLogin");
		} else {
			// createPerson
			trace("createPerson");
			try {
				svcRes = dispatcher.runSync("createPerson", svcCtx);
			} catch (final GenericServiceException e) {
				Debug.logError(e, module);
				pos.showDialog("dialog/error/exception", e.getMessage());
				return result;
			}
			if (ServiceUtil.isError(svcRes)) {
				pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
				return result;
			}
			partyId = (String) svcRes.get("partyId");
			partyUserLogin = userLogin;
		}
		
		if (UtilValidate.isNotEmpty(email)) {
			// createPartyEmailAddress
			trace("createPartyEmailAddress");
			svcCtx.clear();
			svcCtx.put("userLogin", partyUserLogin);
			svcCtx.put("emailAddress", email);
			svcCtx.put("partyId", partyId);
			svcCtx.put("contactMechPurposeTypeId", "PRIMARY_EMAIL");
			try {
				svcRes = dispatcher.runSync("createPartyEmailAddress", svcCtx);
				emailContact = (String) svcRes.get("contactMechId");
			} catch (final GenericServiceException e) {
				Debug.logError(e, module);
				pos.showDialog("dialog/error/exception", e.getMessage());
				return null;
			}
			if (ServiceUtil.isError(svcRes)) {
				pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
				return null;
			}
		}
		
		if (UtilValidate.isNotEmpty(phone)) {
			if (phone.length() < 5) {
				pos.showDialog("dialog/error/exception", UtilProperties.getMessage(PosTransaction.resource, "PosPhoneField5Required", locale));
			} else {
				// createPartyTelecomNumber
				trace("createPartyTelecomNumber");
				svcCtx.clear();
				svcCtx.put("userLogin", partyUserLogin);
				svcCtx.put("contactNumber", phone);
				svcCtx.put("partyId", partyId);
				svcCtx.put("contactMechPurposeTypeId", "PHONE_HOME");
				try {
					svcRes = dispatcher.runSync("createPartyTelecomNumber", svcCtx);
					phoneContact = (String) svcRes.get("contactMechId");
				} catch (final GenericServiceException e) {
					Debug.logError(e, module);
					pos.showDialog("dialog/error/exception", e.getMessage());
					return null;
				}
				if (ServiceUtil.isError(svcRes)) {
					pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
					return null;
				}
			}
		}
		Debug.logInfo("contactMechs" + emailContact + phoneContact, module);
		if (UtilValidate.isNotEmpty(emailContact)) {
			cart.addContactMech("PRIMARY_EMAIL", emailContact);
		}
		if (UtilValidate.isNotEmpty(phoneContact)) {
			cart.addContactMech("PHONE_HOME", phoneContact);
		}
		
		result = partyId;
		return null;
		
	}
	
	public String editClientProfile(final String name, final String email, final String phone, final String card, final PosScreen pos, final String editType, String partyId) {
		// We suppose here that a cardId (card number) can only belongs to one person (it's used as owned PromoCode)
		// We use the 1st party's login (it may change and be multiple since it depends on email and card)
		// We suppose only one email address (should be ok anyway because of the contactMechPurposeTypeId == "PRIMARY_EMAIL")
		// we suppose only one phone number (should be ok anyway because of the contactMechPurposeTypeId == "PHONE_HOME")
		final LocalDispatcher dispatcher = session.getDispatcher();
		GenericValue userLogin = session.getUserLogin();
		GenericValue partyUserLogin = null;
		String result = null;
		
		final Map<String, Object> svcCtx = FastMap.newInstance();
		Map<String, Object> svcRes = null;
		
		// Create
		if ("create".equals(editType)) {
			trace("Create a client profile");
			// createPersonAndUserLogin
			trace("createPersonAndUserLogin");
			if (UtilValidate.isNotEmpty(card)) {
				svcCtx.put("cardId", card);
			}
			svcCtx.put("userLogin", userLogin);
			svcCtx.put("lastName", name);
			svcCtx.put("firstName", ""); // Needed by service createPersonAndUserLogin
			if (UtilValidate.isNotEmpty(email) && UtilValidate.isNotEmpty(phone)) {
				svcCtx.put("userLoginId", email);
				svcCtx.put("currentPassword", phone);
				svcCtx.put("currentPasswordVerify", phone);
				try {
					svcRes = dispatcher.runSync("createPersonAndUserLogin", svcCtx);
				} catch (final GenericServiceException e) {
					Debug.logError(e, module);
					pos.showDialog("dialog/error/exception", e.getMessage());
					return null;
				}
				if (ServiceUtil.isError(svcRes)) {
					pos.showDialog("dialog/error/exceptionLargeSmallFont", ServiceUtil.getErrorMessage(svcRes)); // exceptionLargeSmallFont used to show duplicate key error message for card
					return null;
				}
				partyId = (String) svcRes.get("partyId");
				partyUserLogin = (GenericValue) svcRes.get("newUserLogin");
			} else {
				// createPerson
				trace("createPerson");
				try {
					svcRes = dispatcher.runSync("createPerson", svcCtx);
				} catch (final GenericServiceException e) {
					Debug.logError(e, module);
					pos.showDialog("dialog/error/exception", e.getMessage());
					return result;
				}
				if (ServiceUtil.isError(svcRes)) {
					pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
					return result;
				}
				partyId = (String) svcRes.get("partyId");
				partyUserLogin = userLogin;
			}
			
			if (UtilValidate.isNotEmpty(email)) {
				// createPartyEmailAddress
				trace("createPartyEmailAddress");
				svcCtx.clear();
				svcCtx.put("userLogin", partyUserLogin);
				svcCtx.put("emailAddress", email);
				svcCtx.put("partyId", partyId);
				svcCtx.put("contactMechPurposeTypeId", "PRIMARY_EMAIL");
				try {
					svcRes = dispatcher.runSync("createPartyEmailAddress", svcCtx);
				} catch (final GenericServiceException e) {
					Debug.logError(e, module);
					pos.showDialog("dialog/error/exception", e.getMessage());
					return null;
				}
				if (ServiceUtil.isError(svcRes)) {
					pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
					return null;
				}
			}
			
			if (UtilValidate.isNotEmpty(phone)) {
				if (phone.length() < 5) {
					pos.showDialog("dialog/error/exception", UtilProperties.getMessage(PosTransaction.resource, "PosPhoneField5Required", locale));
				} else {
					// createPartyTelecomNumber
					trace("createPartyTelecomNumber");
					svcCtx.clear();
					svcCtx.put("userLogin", partyUserLogin);
					svcCtx.put("contactNumber", phone);
					svcCtx.put("partyId", partyId);
					svcCtx.put("contactMechPurposeTypeId", "PHONE_HOME");
					try {
						svcRes = dispatcher.runSync("createPartyTelecomNumber", svcCtx);
					} catch (final GenericServiceException e) {
						Debug.logError(e, module);
						pos.showDialog("dialog/error/exception", e.getMessage());
						return null;
					}
					if (ServiceUtil.isError(svcRes)) {
						pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
						return null;
					}
				}
			}
			
			result = partyId;
			
			// Update
		} else if (UtilValidate.isNotEmpty(partyId)) {
			trace("Update a client profile");
			GenericValue person = null;
			
			try {
				person = session.getDelegator().findByPrimaryKey("Person", UtilMisc.toMap("partyId", partyId));
			} catch (final GenericEntityException e) {
				Debug.logError(e, module);
				pos.showDialog("dialog/error/exception", e.getMessage());
				return null;
			}
			
			Boolean newLogin = true;
			try {
				final List<GenericValue> userLogins = session.getDelegator().findByAnd("UserLogin", UtilMisc.toMap("partyId", partyId));
				if (UtilValidate.isNotEmpty(userLogins)) {
					userLogin = userLogins.get(0);
					newLogin = false;
				}
			} catch (final GenericEntityException e) {
				Debug.logError(e, module);
				pos.showDialog("dialog/error/exception", e.getMessage());
				return null;
			}
			
			if (!person.getString("lastName").equals(name)
					|| UtilValidate.isNotEmpty(card) && !card.equals(person.getString("cardId"))) {
				// Update name and possibly card (cardId)
				svcCtx.put("userLogin", userLogin);
				svcCtx.put("partyId", partyId);
				svcCtx.put("firstName", ""); // Needed by service updatePerson
				svcCtx.put("lastName", name);
				if (UtilValidate.isNotEmpty(card)) {
					svcCtx.put("cardId", card);
				}
				try {
					// updatePerson
					trace("updatePerson");
					svcRes = dispatcher.runSync("updatePerson", svcCtx);
				} catch (final GenericServiceException e) {
					Debug.logError(e, module);
					pos.showDialog("dialog/error/exception", e.getMessage());
					return null;
				}
				if (ServiceUtil.isError(svcRes)) {
					pos.showDialog("dialog/error/exceptionLargeSmallFont", ServiceUtil.getErrorMessage(svcRes));
					return null;
				}
			}
			
			if (UtilValidate.isNotEmpty(phone)) {
				// Create or update phone
				if (phone.length() < 5) {
					pos.showDialog("dialog/error/exception", UtilProperties.getMessage(PosTransaction.resource, "PosPhoneField5Required", locale));
				} else {
					String contactNumber = null;
					String contactMechId = null;
					svcCtx.clear();
					svcCtx.put("partyId", partyId);
					svcCtx.put("thruDate", null); // last one
					try {
						final List<GenericValue> PartyTelecomNumbers = session.getDelegator().findByAnd("PartyAndTelecomNumber", svcCtx);
						if (UtilValidate.isNotEmpty(PartyTelecomNumbers)) {
							final GenericValue PartyTelecomNumber = PartyTelecomNumbers.get(0); // There is  only one phone number (contactMechPurposeTypeId == "PHONE_HOME")
							contactNumber = PartyTelecomNumber.getString("contactNumber");
							contactMechId = PartyTelecomNumber.getString("contactMechId");
						}
					} catch (final GenericEntityException e) {
						Debug.logError(e, module);
						pos.showDialog("dialog/error/exception", e.getMessage());
						return null;
					}
					
					// Create or update phone
					trace("createUpdatePartyTelecomNumber");
					svcCtx.remove("thruDate");
					svcCtx.put("userLogin", userLogin);
					svcCtx.put("contactNumber", phone);
					svcCtx.put("contactMechPurposeTypeId", "PHONE_HOME");
					if (UtilValidate.isNotEmpty(contactMechId)) {
						svcCtx.put("contactMechId", contactMechId);
					}
					try {
						svcRes = dispatcher.runSync("createUpdatePartyTelecomNumber", svcCtx);
					} catch (final GenericServiceException e) {
						Debug.logError(e, module);
						pos.showDialog("dialog/error/exception", e.getMessage());
						return null;
					}
					if (ServiceUtil.isError(svcRes)) {
						pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
						return null;
					}
					
					// Handle login aspect where phone is taken as pwd
					if (UtilValidate.isNotEmpty(contactNumber) && !phone.equals(contactNumber)) {
						if (!newLogin) { // to create a new login we need also an email address
							// Update password, we need to temporary set password.accept.encrypted.and.plain to "true"
							// This is done only for the properties loaded for the session in memory (we don't persist the value)
							trace("updatePassword");
							String passwordAcceptEncryptedAndPlain = null;
							try {
								passwordAcceptEncryptedAndPlain = UtilProperties.getPropertyValue("security.properties", "password.accept.encrypted.and.plain");
								UtilProperties.setPropertyValueInMemory("security.properties", "password.accept.encrypted.and.plain", "true");
								svcRes = dispatcher.runSync("updatePassword",
										UtilMisc.toMap("userLogin", userLogin,
												"userLoginId", userLogin.getString("userLoginId"),
												"currentPassword", userLogin.getString("currentPassword"),
												"newPassword", phone,
												"newPasswordVerify", phone));
							} catch (final GenericServiceException e) {
								Debug.logError(e, "Error calling updatePassword service", module);
								pos.showDialog("dialog/error/exception", e.getMessage());
								UtilProperties.setPropertyValueInMemory("security.properties", "password.accept.encrypted.and.plain", passwordAcceptEncryptedAndPlain);
								return null;
							} finally {
								// Put back passwordAcceptEncryptedAndPlain value in memory
								UtilProperties.setPropertyValueInMemory("security.properties", "password.accept.encrypted.and.plain", passwordAcceptEncryptedAndPlain);
							}
							if (ServiceUtil.isError(svcRes)) {
								pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
								return null;
							}
						}
					}
				}
			}
			
			if (UtilValidate.isNotEmpty(email)) {
				// Update email
				svcCtx.clear();
				svcCtx.put("partyId", partyId);
				svcCtx.put("thruDate", null); // last one
				svcCtx.put("contactMechTypeId", "EMAIL_ADDRESS");
				String contactMechId = null;
				String infoString = null;
				try {
					final List<GenericValue> PartyEmails = session.getDelegator().findByAnd("PartyAndContactMech", svcCtx);
					if (UtilValidate.isNotEmpty(PartyEmails)) {
						final GenericValue PartyEmail = PartyEmails.get(0); // There is  only one email address (contactMechPurposeTypeId == "PRIMARY_EMAIL")
						contactMechId = PartyEmail.getString("contactMechId");
						infoString = PartyEmail.getString("infoString");
					}
				} catch (final GenericEntityException e) {
					Debug.logError(e, module);
					pos.showDialog("dialog/error/exception", e.getMessage());
					return null;
				}
				
				svcCtx.remove("thruDate");
				svcCtx.remove("contactMechTypeId");
				svcCtx.put("userLogin", userLogin);
				svcCtx.put("emailAddress", email);
				svcCtx.put("contactMechPurposeTypeId", "PRIMARY_EMAIL");
				if (UtilValidate.isNotEmpty(contactMechId)) {
					svcCtx.put("contactMechId", contactMechId);
				}
				if (UtilValidate.isNotEmpty(infoString) && !email.equals(infoString)
						|| UtilValidate.isEmpty(infoString)) {
					// Create or update email
					trace("createUpdatePartyEmailAddress");
					try {
						svcRes = dispatcher.runSync("createUpdatePartyEmailAddress", svcCtx);
					} catch (final GenericServiceException e) {
						Debug.logError(e, module);
						pos.showDialog("dialog/error/exception", e.getMessage());
						return null;
					}
					if (ServiceUtil.isError(svcRes)) {
						pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
						return null;
					}
				}
				
				if (!newLogin && UtilValidate.isNotEmpty(infoString) && !email.equals(infoString)) {
					// create a new UserLogin (Update a UserLoginId by creating a new one and expiring the old one). Keep the same password possibly changed just above if phone has also changed.
					trace("updateUserLoginId");
					try {
						svcRes = dispatcher.runSync("updateUserLoginId", UtilMisc.toMap("userLoginId", email, "userLogin", userLogin));
					} catch (final GenericServiceException e) {
						Debug.logError(e, module);
						pos.showDialog("dialog/error/exception", e.getMessage());
						return null;
					}
					if (ServiceUtil.isError(svcRes)) {
						pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
						return null;
					}
				} else if (newLogin && UtilValidate.isNotEmpty(phone)) {
					// createUserLogin
					trace("createUserLogin");
					try {
						svcRes = dispatcher.runSync("createUserLogin",
								UtilMisc.toMap("userLogin", userLogin,
										"userLoginId", email,
										"currentPassword", phone,
										"currentPasswordVerify", phone,
										"partyId", partyId));
					} catch (final GenericServiceException e) {
						Debug.logError(e, "Error calling updatePassword service", module);
						pos.showDialog("dialog/error/exception", e.getMessage());
						return null;
					}
					if (ServiceUtil.isError(svcRes)) {
						pos.showDialog("dialog/error/exception", ServiceUtil.getErrorMessage(svcRes));
						return null;
					}
				}
			}
		} else {
			pos.showDialog("dialog/error/exception", UtilProperties.getMessage(resource, "PosNoClientProfile", locale));
			return null;
		}
		return null;
	}
}

class MarketingCartItemInfo {
	protected String productSku;
	protected String productName;
	protected String productQty;
	protected String productPrice;
	
	public MarketingCartItemInfo() {}
	
	public MarketingCartItemInfo(final String sku, final String name, final String qty, final String price) {
		
		productSku = sku;
		productName = name;
		productQty = qty;
		productPrice = price;
	}
}