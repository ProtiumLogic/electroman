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

import java.math.BigDecimal;
import java.util.*;
import java.sql.Timestamp;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.entity.GenericValue;

import org.ofbiz.base.util.*;
import javolution.util.FastMap;
import org.ofbiz.accounting.invoice.*;




returnItemsGroup =[];
fromDate = parameters.fromDate;
thruDate = parameters.thruDate;

    fromDateTimestamp = ObjectType.simpleTypeConvert(fromDate, "Timestamp", null, timeZone, locale, false);
    thruDateTimestamp = ObjectType.simpleTypeConvert(thruDate, "Timestamp", null, timeZone, locale, false);
    
ecl = EntityCondition.makeCondition([EntityCondition.makeCondition("entryDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDateTimestamp),
                        EntityCondition.makeCondition("entryDate", EntityOperator.LESS_THAN, thruDateTimestamp)],
                    EntityOperator.AND);
returnList = delegator.findList("ReturnHeader", ecl, null, ["entryDate"], null, false);
for (GenericValue item: returnList) {
returnInfo = delegator.findOne("ReturnHeader",[returnId:item.returnId],false);
returnItems = delegator.findByAnd("ReturnItem",[returnId:item.returnId]);
if (returnItems) {
    returnItems.each { retitem ->
        Map returnItemMap = FastMap.newInstance();
         returnItemMap.productId = retitem.productId
        returnItemMap.quantity = retitem.getBigDecimal("returnQuantity");
        returnItemMap.description = retitem.description;
        returnItemMap.returnprice = retitem.getBigDecimal("returnPrice");
        returnItemMap.returndate = UtilDateTime.toDateString(returnInfo.entryDate,"dd/MM/yyyy");
        returnItemMap.orderId = retitem.orderId;
        
        returnItemsGroup.add(returnItemMap);
       
        
        print("#############################"+item);
}
}
}


ZERO = BigDecimal.ZERO;
context.ZERO = ZERO;
context.returnItems = returnItemsGroup;