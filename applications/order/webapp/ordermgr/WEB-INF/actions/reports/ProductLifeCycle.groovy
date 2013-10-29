/*
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
 */

import javax.servlet.http.HttpServletRequest;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.*;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.order.shoppingcart.ShoppingCartEvents;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.base.util.UtilDateTime;
import javolution.util.FastMap;

productId = parameters.productId;
openingBalanceDate = null;
context.productId = productId;
reportDataList =[];
if (!productId) {
	prodCondition1 = 	EntityCondition.makeCondition([EntityCondition.makeCondition("productTypeId", EntityOperator.NOT_EQUAL, "MARKETING_PKG"),
						 EntityCondition.makeCondition("productTypeId", EntityOperator.NOT_EQUAL, "MARKETING_PKG_AUTO")],
						 EntityOperator.OR);
	prodCondition2 = EntityCondition.makeCondition([EntityCondition.makeCondition("productId", EntityOperator.NOT_EQUAL, null)],EntityOperator.AND);
	combinedCond = EntityCondition.makeCondition([prodCondition1, prodCondition2], EntityOperator.AND);
	prodList = delegator.findList("Product", combinedCond, null, null, null, false);
	for (GenericValue itr: prodList) {
		itemQtyTotal = BigDecimal.ZERO;
		invItemList = delegator.findByAnd("InventoryItem",[productId:itr.productId],["-datetimeReceived"]);
		if(UtilValidate.isNotEmpty(invItemList)) {
		for (GenericValue itm:invItemList ) {
				itemQtyTotal = itemQtyTotal.add(itm.getBigDecimal("accountingQuantityTotal"));
			}
			invItem = EntityUtil.getFirst(invItemList);
			repeatFlag="N";
			invItemDetailList = delegator.findByAnd("InventoryItemDetail",[inventoryItemId:invItem.inventoryItemId],["-effectiveDate"]);
			if (invItemDetailList) {
						invItemDet = EntityUtil.getFirst(invItemDetailList);
						openingBalanceDate = invItemDet.effectiveDate;
						opbCount = opbCheck(openingBalanceDate,itr.productId);
						Map inventoryItemMap = FastMap.newInstance();
						inventoryItemMap.effectiveDate = invItemDet.effectiveDate;
						inventoryItemMap.openingBalance = opbCount;
						if (invItemDet.orderId!=null) {
							inventoryItemMap.orderId = invItemDet.orderId;
						}else {
							inventoryItemMap.orderId = "_NA_";
						}
						if (invItem.partyId!=null) {						
							inventoryItemMap.partyId = invItem.partyId;
						} else {
							inventoryItemMap.partyId ="_NA_"
						}						
						inventoryItemMap.productId = invItem.productId;
						inventoryItemMap.itemQtyTotal =itemQtyTotal;
						reportDataList.add(inventoryItemMap);
						repeatFlag="Y";
				}					
			}
		}
} else {
	itemQtyTotal = BigDecimal.ZERO;
	invItemList = delegator.findByAnd("InventoryItem",[productId:productId],["-datetimeReceived"]);
		if(UtilValidate.isNotEmpty(invItemList)) {
			for (GenericValue itm:invItemList ) {
				itemQtyTotal = itemQtyTotal.add(itm.getBigDecimal("accountingQuantityTotal"));
			}
			repeatFlag="N";
			invItem = EntityUtil.getFirst(invItemList);
			invItemDetailList = delegator.findByAnd("InventoryItemDetail",[inventoryItemId:invItem.inventoryItemId],["-effectiveDate"]);
			if (invItemDetailList) {
						    invItemDet = EntityUtil.getFirst(invItemDetailList);
						    openingBalanceDate = invItemDet.effectiveDate;
						    opbCount = opbCheck(openingBalanceDate,productId);
							Map inventoryItemMap = FastMap.newInstance();
							inventoryItemMap.effectiveDate = invItemDet.effectiveDate;
							inventoryItemMap.openingBalance = opbCount;
							if (invItemDet.orderId!=null) {
								inventoryItemMap.orderId = invItemDet.orderId;
							}else {
								inventoryItemMap.orderId = "_NA_";
								}
							if (invItem.partyId!=null) {						
								inventoryItemMap.partyId = invItem.partyId;
							} else {
								inventoryItemMap.partyId ="_NA_"
							}
							inventoryItemMap.productId = invItem.productId;
							inventoryItemMap.itemQtyTotal =itemQtyTotal;
							reportDataList.add(inventoryItemMap);
							repeatFlag="Y";
			}					
		}
	}
reportDataList.each { item ->
reportDataList.sort{it.effectiveDate};
}
context.reportDataList=reportDataList;
def opbCheck(openingDate,productId) {
	openingBalanceCount = BigDecimal.ZERO;
	openingDateStart = UtilDateTime.getDayStart(openingDate, timeZone, locale);
	openingDateEnd = UtilDateTime.getDayEnd(openingDate, timeZone, locale);
	if (openingDate!=null) {
	opbCondition = EntityCondition.makeCondition([EntityCondition.makeCondition("datetimeReceived", EntityOperator.GREATER_THAN_EQUAL_TO, openingDateStart),
							EntityCondition.makeCondition("productId", EntityOperator.EQUALS,productId),
	                        EntityCondition.makeCondition("datetimeReceived", EntityOperator.LESS_THAN, openingDateEnd)],
	                    	EntityOperator.AND);
	invoiceItems = delegator.findList("InventoryItem",opbCondition,null,null,null,false);
	} else {
	opbCondition = EntityCondition.makeCondition([EntityCondition.makeCondition("productId", EntityOperator.EQUALS,productId)
	                        ],EntityOperator.AND);
	invoiceItems = delegator.findList("InventoryItem",opbCondition,null,null,null,false);
	}
	for (GenericValue item: invoiceItems) {
		openingBalanceCount = openingBalanceCount.add(item.getBigDecimal("accountingQuantityTotal"));		
	}
	return openingBalanceCount;
}
