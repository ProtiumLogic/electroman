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


partyId = parameters.partyId ?: parameters.party_id;
productIds = parameters.productIds
context.productIds =productIds;
if(UtilValidate.isEmpty(partyId)){
	request.setAttribute("_ERROR_MESSAGE_", "Please Select a Supplier");
	return "error";
}

supplierPartyId = parameters.partyId ?: parameters.party_id;
rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");
decimals = 3;
context.rounding = rounding;
context.decimals = decimals;


conditionList = [];
supProdList = [];
supProdList = parameters.productIds;
supProd = parameters.productId;
conditionList.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, supplierPartyId));
conditionList.add(EntityCondition.makeConditionDate("availableFromDate", "availableThruDate"));
conditions = EntityCondition.makeCondition(conditionList, EntityOperator.AND);
selectedFields = ["productId", "supplierProductId", "supplierProductName", "lastPrice", "updatedLastPrice"] as Set;
supplierProducts = delegator.findList("SupplierProduct", conditions, selectedFields, ["productId"], null, false);
context.allsupplierProducts = supplierProducts;
supplierProducts2 =supplierProducts;
nowTimeStamp = UtilDateTime.nowTimestamp();
if (supProdList) {

iter = supplierProducts.iterator();
while (iter) {
  supplierProduct = iter.next();  
  if (!supProdList.contains(supplierProduct.productId)) {
    iter.remove();
  }
}
}
context.supplierProducts = supplierProducts2;
context.supplierPartyId=supplierPartyId;
context.nowTimeStamp=nowTimeStamp;
ZERO = BigDecimal.ZERO;
context.ZERO = ZERO;