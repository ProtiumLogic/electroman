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

public class InventoryImport {


	public final static String module = InventoryImport.class.getName();


	public static Map<String,Object>importProductIdFromFile(DispatchContext dctx, Map<String, ?extends Object> context){
		LocalDispatcher dispatcher = dctx.getDispatcher();
		java.nio.ByteBuffer fileBytes = (java.nio.ByteBuffer) context.get("uploadedFile");
		if (fileBytes == null) {
			return ServiceUtil.returnError(
					"Invalid file");
		}
		String encoding = System.getProperty("file.encoding");
		String file = Charset.forName(encoding).decode(fileBytes).toString();
		BufferedReader reader = new BufferedReader(new StringReader(file));
		List<Object> errors = FastList.newInstance();
		int lines = 0;
		String line;

		try {
			while ((line = reader.readLine()) != null) {
				if (line.length() > 0 && !line.startsWith("#")) {
					if (line.length() > 0 && line.length() <= 20) {
						Map<String, String> inContext = FastMap.newInstance();
						String[] lineItem = line.split("-");
						inContext.put("productId", lineItem[0]);
						inContext.put("productQtySold", lineItem[1]);
						try {

							Map<String, Object> result = dispatcher.runSync("manualBalanceInventory", inContext);
						} catch (GenericServiceException e) {
							e.printStackTrace();
						}
					}
					++lines;
				}
			}
		} catch (IOException e) {
			Debug.logError(e, module);
		}
		finally {
			try {
				reader.close();
			} catch (IOException e) {
				Debug.logError(e, module);
			}
		}

		if (errors.size() > 0) {
			return ServiceUtil.returnError(errors);
		} else if (lines == 0) {
			return ServiceUtil.returnError(
					"ProductPromoCodeImportEmptyFile");
		}

		return ServiceUtil.returnSuccess("The Orders are created for the imporetd invoices.");

	}
	public static Map<String,Object>manualBalanceInventory(DispatchContext dctx, Map<String, ?extends Object> context){
		String productId = (String) context.get("productId");
		BigDecimal productQtySold = new BigDecimal((String)context.get("productQtySold"));
		String facilityId = "MyRetailStore";
		String inventoryStatus = checkProductAvailability(productId,facilityId,dctx,productQtySold);
		//if ("AVAILABLE".equals(inventoryStatus)) {
		

			try {
				final List<GenericValue> inventoryItemList = dctx.getDelegator().findByAnd("InventoryItem", UtilMisc.toMap("productId", productId, "facilityId", facilityId));
						BigDecimal acTotal = BigDecimal.ZERO;
				for (final GenericValue inventoryItem : inventoryItemList) {
					 acTotal = inventoryItem.getBigDecimal("accountingQuantityTotal").add(acTotal);
					 inventoryItem.set("availableToPromiseTotal", BigDecimal.ZERO);
					 inventoryItem.set("quantityOnHandTotal", BigDecimal.ZERO);
					 dctx.getDelegator().store(inventoryItem);
				}
				Debug.logInfo("################### acTotAL"+acTotal, module);
				for (final GenericValue inventoryItem : inventoryItemList) {
					if (acTotal.compareTo(productQtySold) != -1) {
						Debug.logInfo("################### main flag"+inventoryItem.getBigDecimal("accountingQuantityTotal").compareTo(productQtySold), module);
						BigDecimal currStockLevel = acTotal.subtract(productQtySold);
						Debug.logInfo("################### inside inventory balencing"+currStockLevel, module);
						Debug.logInfo("################### product to balance"+productId, module);
						Debug.logInfo("################### flag inventory "+currStockLevel.compareTo(BigDecimal.ZERO), module);
						if (currStockLevel.compareTo(BigDecimal.ZERO) >0) {
							inventoryItem.set("availableToPromiseTotal", currStockLevel);
							inventoryItem.set("quantityOnHandTotal", currStockLevel);
							dctx.getDelegator().store(inventoryItem);
							Debug.logInfo("################### flag inventory "+inventoryItem, module);
							break;
						}
					} else {
						inventoryItem.set("availableToPromiseTotal",BigDecimal.ZERO);
						inventoryItem.set("quantityOnHandTotal", BigDecimal.ZERO);
						dctx.getDelegator().store(inventoryItem);
						break;
					}
				}
			} catch (GenericEntityException e) {
				Debug.logError(e, module);
			}
//		} else {
//			try {
//				final List<GenericValue> inventoryItemList = dctx.getDelegator().findByAnd("InventoryItem", UtilMisc.toMap("productId", productId, "facilityId", facilityId));
//				for (final GenericValue inventoryItem : inventoryItemList) {
//					if (inventoryItem.getBigDecimal("accountingQuantityTotal").compareTo(productQtySold) == 0) {
//					
//					}
//				}
//
//			} catch (GenericEntityException e) {
//				e.printStackTrace();
//			}
//			Debug.logInfo("######################### productId "+productId+" doesn't have enough stock in "+facilityId, module);
//		}


		return ServiceUtil.returnSuccess();
	}

	public static String checkProductAvailability(final String productId, final String facilityId, DispatchContext dctx, BigDecimal quantity) {
		String productStatus = null;
		final LocalDispatcher dispatcher = dctx.getDispatcher();
		boolean beganTransaction = false;
		Map<String, Object> inventoryProductStatus = new HashMap();
		try {
			beganTransaction = TransactionUtil.begin();
			inventoryProductStatus = dispatcher.runSync("getInventoryAvailableByFacility", UtilMisc.toMap("productId", productId, "facilityId", facilityId));
			final BigDecimal ATP = (BigDecimal) inventoryProductStatus.get("availableToPromiseTotal");
			if (ATP.compareTo(quantity) != -1) {
				productStatus = "AVAILABLE";
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