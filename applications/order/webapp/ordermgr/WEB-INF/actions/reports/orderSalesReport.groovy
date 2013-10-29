
import java.math.BigDecimal;
import java.util.*;
import java.sql.Timestamp;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.base.util.*;
import javolution.util.FastMap;
import org.ofbiz.accounting.invoice.*;


invoiceItemList =[:];
invoiceItemsGroup =[];
fromDate = parameters.fromDate;
thruDate = parameters.thruDate;
fromDateTimestamp = ObjectType.simpleTypeConvert(fromDate, "Timestamp", null, timeZone, locale, false);
thruDateTimestamp = ObjectType.simpleTypeConvert(thruDate, "Timestamp", null, timeZone, locale, false);
dayEnd = UtilDateTime.getDayEnd(fromDateTimestamp, timeZone, locale);
if (fromDateTimestamp.equals(thruDateTimestamp)) {
		thruDateTimestamp = dayEnd;
	}
thruDateTimestamp = UtilDateTime.getDayEnd(thruDateTimestamp, timeZone, locale);
invCondition = 		 EntityCondition.makeCondition([EntityCondition.makeCondition("invoiceItemTypeId", EntityOperator.EQUALS, "INV_FPROD_ITEM"),
					 EntityCondition.makeCondition("invoiceItemTypeId", EntityOperator.EQUALS, "INV_PROD_ITEM")],
					 EntityOperator.OR);

salesCondition = EntityCondition.makeCondition([EntityCondition.makeCondition("invoiceDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDateTimestamp),
						EntityCondition.makeCondition("invoiceTypeId", EntityOperator.EQUALS, "SALES_INVOICE"),
                        EntityCondition.makeCondition("invoiceDate", EntityOperator.LESS_THAN, thruDateTimestamp)],
                    	EntityOperator.AND);
returnCondition = EntityCondition.makeCondition([EntityCondition.makeCondition("entryDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDateTimestamp),
                        EntityCondition.makeCondition("entryDate", EntityOperator.LESS_THAN, thruDateTimestamp)],
                    EntityOperator.AND);


                    	

invoiceList = delegator.findList("Invoice",salesCondition, null, null, null, false);
returnList = delegator.findList("ReturnHeader",returnCondition, null, ["entryDate"], null, false);

for (GenericValue invoiceItem: invoiceList) {
	invItemCondition = EntityCondition.makeCondition([EntityCondition.makeCondition("invoiceId", EntityOperator.EQUALS, invoiceItem.invoiceId)], EntityOperator.AND);
	combinedCond = EntityCondition.makeCondition([invCondition, invItemCondition], EntityOperator.AND);

	invoiceInfo = delegator.findOne("Invoice",[invoiceId:invoiceItem.invoiceId],false);
	invoiceItems = delegator.findList("InvoiceItem",combinedCond,null,null,null,false);
	if (invoiceItems) {
	    	invoiceItems.each { item ->
		        invoiceItemSeqId = item.invoiceItemSeqId;
		        invoiceId = item.invoiceId;
		        Map invoiceItemMap = FastMap.newInstance();
		        invTotal = InvoiceWorker.getInvoiceTotal(invoiceInfo);
		        invoiceItemMap.invoiceTotal = invTotal;
				invoiceItemMap.partyId = invoiceInfo.partyId;
		        invoiceItemMap.amount = item.getBigDecimal("amount");
		        invoiceItemMap.netItemAmount = item.getBigDecimal("amount").multiply(item.getBigDecimal("quantity"));
		        invoiceItemMap.invoiceDate = UtilDateTime.toDateString(invoiceInfo.invoiceDate,"dd/MM/yyyy");
		        invoiceItemMap.invoiceTime = UtilDateTime.toTimeString(invoiceInfo.invoiceDate);
				invoiceItemMap.sortDate = invoiceInfo.invoiceDate;
				invoiceItemMap.entryType = "SALES_ORDER";
		        invoiceItemMap.putAll((Map) item);
		        invoiceItemsGroup.add(invoiceItemMap);
			}
	}
}
for (GenericValue item: returnList) {
	returnInfo = delegator.findOne("ReturnHeader",[returnId:item.returnId],false);
	returnItems = delegator.findByAnd("ReturnItem",[returnId:item.returnId]);
		if (returnItems) {
		    returnItems.each { retitem ->
		        Map returnItemMap = FastMap.newInstance();
		        returnItemMap.productId = retitem.productId
		        returnItemMap.returnquantity = retitem.getBigDecimal("returnQuantity");
		        returnItemMap.description = retitem.description;
				returnItemMap.fromPartyId = returnInfo.fromPartyId;
		        returnItemMap.returnprice = retitem.getBigDecimal("returnPrice").negate();
		        returnItemMap.returndate = UtilDateTime.toDateString(returnInfo.entryDate,"dd/MM/yyyy");
		        returnItemMap.sortDate = returnInfo.entryDate;
				returnItemMap.entryType = "SALES_RETURN";
		        returnItemMap.orderId = retitem.orderId;
				returnItemMap.returnId = retitem.returnId;
		        invoiceItemsGroup.add(returnItemMap);
			}
	}
}

invoiceItemsGroup.each { item ->
invoiceItemsGroup.sort{it.sortDate};
}
invoiceItemsGroup = invoiceItemsGroup.reverse();
context.invoiceItems = invoiceItemsGroup;

