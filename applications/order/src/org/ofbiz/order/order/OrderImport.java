package org.ofbiz.order.order;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

public class OrderImport {
	
	public final static String module = OrderImport.class.getName();
	
	public static Map<String, Object> importInvoiceIdFromFile(final DispatchContext dctx, final Map<String, ? extends Object> context) {
		final LocalDispatcher dispatcher = dctx.getDispatcher();
		// check the uploaded file
		final java.nio.ByteBuffer fileBytes = (java.nio.ByteBuffer) context.get("uploadedFile");
		if (fileBytes == null) {
			return ServiceUtil.returnError(
					"Invalid file");
		}
		
		final String encoding = System.getProperty("file.encoding");
		final String file = Charset.forName(encoding).decode(fileBytes).toString();
		// get the createProductPromoCode Model
//        ModelService promoModel;
//        try {
//            promoModel = dispatcher.getDispatchContext().getModelService("createProductPromoCode");
//        } catch (GenericServiceException e) {
//            Debug.logError(e, module);
//            return ServiceUtil.returnError(e.getMessage());
//        }
//
//        // make a temp context for invocations
//        Map<String, Object> invokeCtx = promoModel.makeValid(context, ModelService.IN_PARAM);
		
		// read the bytes into a reader
		final BufferedReader reader = new BufferedReader(new StringReader(file));
		final List<Object> errors = FastList.newInstance();
		int lines = 0;
		String line;
		
		// read the uploaded file and process each line
		try {
			while ((line = reader.readLine()) != null) {
				if (line.length() > 0 && !line.startsWith("#")) {
					if (line.length() > 0 && line.length() <= 20) {
						final Map<String, Object> inContext = FastMap.newInstance();
						Debug.logInfo("--------##########################################---------" + line, module);
						
						inContext.put("invoiceId", line);
						//Map result = createOrderFromInvoice(dctx,context);
						try {
							final Map<String, Object> result = dispatcher.runSync("createOrderFromInvoice", inContext);
						} catch (final GenericServiceException e) {
							e.printStackTrace();
						}
//                        if (result != null && ServiceUtil.isError(result)) {
//                            errors.add(line + ": " + ServiceUtil.getErrorMessage(result));
//                        }
//                    } else {
//                        // not valid ignore and notify
//                        errors.add(line + "Invalid invoiceId");
					}
					++lines;
				}
			}
		} catch (final IOException e) {
			Debug.logError(e, module);
		} finally {
			try {
				reader.close();
			} catch (final IOException e) {
				Debug.logError(e, module);
			}
		}
		
		// return errors or success
		if (errors.size() > 0) {
			return ServiceUtil.returnError(errors);
		} else if (lines == 0) {
			return ServiceUtil.returnError(
					"ProductPromoCodeImportEmptyFile");
		}
		
		return ServiceUtil.returnSuccess("The Orders are created for the imporetd invoices.");
		
	}
	
	public static Map<String, Object> createOrderFromInvoice(final DispatchContext dctx, final Map<String, ? extends Object> context) {
		final LocalDispatcher dispatcher = dctx.getDispatcher();
		final Delegator delegator = dctx.getDelegator();
		final String invoiceId = (String) context.get("invoiceId");
		long nextItemSeq = 1;
		
		final Map<String, Object> inputs = new HashMap<String, Object>();
		try {
			BigDecimal grandTotal = BigDecimal.ZERO;
			final ArrayList<GenericValue> orderItemList = new ArrayList<GenericValue>();
			final List<GenericValue> orderPaymentInfo = FastList.newInstance();
			final ArrayList<GenericValue> orderItemBillingList = new ArrayList<GenericValue>();
			final GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
			final List<GenericValue> invoiceItems = delegator.findByAnd("InvoiceItem", UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemTypeId", "INV_PROD_ITEM"));
			final Timestamp orderDate = Timestamp.valueOf(invoice.getString("invoiceDate"));
			final GenericValue orderHeader = delegator.makeValue("OrderHeader");
			orderHeader.set("orderTypeId", "SALES_ORDER");
			orderHeader.set("salesChannelEnumId", "POS_SALES_CHANNEL");
			orderHeader.set("orderDate", orderDate);
			final ArrayList<String> productList = new ArrayList<String>();
			Debug.logInfo("###################################################!!!!!!!!!" + UtilFormatOut.formatPaddedNumber(2, 5), invoiceId);
			for (final GenericValue item : invoiceItems) {
				final GenericValue orderItems = delegator.makeValue("OrderItem");
				final String orderItemSeqId = UtilFormatOut.formatPaddedNumber(nextItemSeq, 5);
				orderItems.set("orderItemSeqId", orderItemSeqId);
				orderItems.set("orderItemTypeId", "PRODUCT_ORDER_ITEM");
				orderItems.set("quantity", item.getBigDecimal("quantity"));
				orderItems.set("unitPrice", item.getBigDecimal("amount"));
				orderItems.set("productId", item.getString("productId"));
				orderItems.set("statusId", "ITEM_COMPLETED");
				orderItems.set("itemDescription", item.getString("description"));
				orderItemList.add(orderItems);
				nextItemSeq++;
				grandTotal = grandTotal.add(item.getBigDecimal("quantity").multiply(item.getBigDecimal("amount")));
			}
			final GenericValue opp = delegator.makeValue("OrderPaymentPreference");
			opp.set("orderPaymentPreferenceId", delegator.getNextSeqId("OrderPaymentPreference"));
			opp.set("createdDate", orderDate);
			opp.set("createdByUserLogin", "webmaster");
			opp.set("statusId", "PAYMENT_RECEIVED");
			opp.set("maxAmount", grandTotal);
			opp.set("paymentMethodTypeId", "CASH");
			orderPaymentInfo.add(opp);
			inputs.put("orderPaymentInfo", orderPaymentInfo);
			inputs.put("orderItems", orderItemList);
			inputs.put("grandTotal", grandTotal);
			inputs.put("orderDate", orderDate);
			inputs.put("orderTypeId", "SALES_ORDER");
			inputs.put("salesChannelEnumId", "POS_SALES_CHANNEL");
			inputs.put("productStoreId", "9100");
			inputs.put("partyId", invoice.getString("partyId"));
			inputs.put("currencyUom", "OMR");
			inputs.put("placingCustomerPartyId", invoice.getString("partyId"));
			inputs.put("endUserCustomerPartyId", invoice.getString("partyId"));
			inputs.put("billToCustomerPartyId", invoice.getString("partyId"));
			inputs.put("billFromVendorPartyId", "Company");
			for (final String productId : productList) {
				final String inventoryStatus = checkProductAvailability(productId, "MyRetailStore", dctx);
				if ("NOT_AVAILABLE".equals(inventoryStatus)) {
					Debug.logInfo("-------Not enoughNot Stock-skipping inventory reservation---------", module);
					break;
				}
				//inputs.put("originFacilityId", "MyRetailStore");
			}
			GenericValue userLogin = null;
			try {
				userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "webmaster"));
			} catch (final GenericEntityException e1) {
				e1.printStackTrace();
			}
			inputs.put("userLogin", userLogin);
			
			Map<String, Object> orderResult = null;
			try {
				orderResult = dispatcher.runSync("storeOrder", inputs);
			} catch (final GenericServiceException e) {
				Debug.logInfo(e, "Problem creating the order!", module);
			}
			if (orderResult != null) {
				final String createdOrderId = (String) orderResult.get("orderId");
				final Map<String, Object> reserveInput = new HashMap<String, Object>();
				reserveInput.put("orderId", createdOrderId);
				for (final GenericValue item : orderItemList) {
					final GenericValue orderItemBilling = delegator.makeValue("OrderItemBilling");
					orderItemBilling.set("orderId", createdOrderId);
					orderItemBilling.set("invoiceId", invoiceId);
					orderItemBilling.set("orderItemSeqId", item.getString("orderItemSeqId"));
					orderItemBilling.set("invoiceItemSeqId", item.getString("orderItemSeqId"));
					orderItemBilling.set("quantity", item.getBigDecimal("quantity"));
					orderItemBilling.set("amount", item.getBigDecimal("amount"));
					orderItemBillingList.add(orderItemBilling);
				}
				delegator.storeAll(orderItemBillingList);
				final Map<String, Object> serviceContextaprvd = UtilMisc.<String, Object>toMap("orderId", createdOrderId, "statusId", "ORDER_APPROVED", "userLogin", userLogin);
				final Map<String, Object> serviceContextcmpltd = UtilMisc.<String, Object>toMap("orderId", createdOrderId, "statusId", "ORDER_COMPLETED", "userLogin", userLogin);
				
				Map<String, Object> approvedSttsResult = null;
				try {
					approvedSttsResult = dispatcher.runSync("changeOrderStatus", serviceContextaprvd);
					approvedSttsResult = dispatcher.runSync("changeOrderStatus", serviceContextcmpltd);
					dispatcher.runSync("issueImmediatelyFulfilledOrder", reserveInput);
					
				} catch (final GenericServiceException e) {
					Debug.logError(e, "Problem calling the changeOrderStatus service", module);
				}
			}
		} catch (final GenericEntityException e) {
			Debug.logError(e, module);
		}
		return ServiceUtil.returnSuccess();
		
	}
	
	public static String checkProductAvailability(final String productId, final String facilityId, final DispatchContext dctx) {
		String productStatus = null;
		final LocalDispatcher dispatcher = dctx.getDispatcher();
		boolean beganTransaction = false;
		Map<String, Object> inventoryProductStatus = new HashMap();
		try {
			beganTransaction = TransactionUtil.begin();
			inventoryProductStatus = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", productId, "facilityId", facilityId));
			final BigDecimal ATP = (BigDecimal) inventoryProductStatus.get("availableToPromiseTotal");
			if (ATP.compareTo(BigDecimal.ZERO) > 0) {
				productStatus = "NOT_AVAILABLE";
			} else {
				productStatus = "NOT_AVAILABLE";
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
	
}
