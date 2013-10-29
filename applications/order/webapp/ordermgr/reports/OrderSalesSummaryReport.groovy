import java.math.BigDecimal;
import java.util.*;
import java.sql.Timestamp;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.base.util.*;
import javolution.util.FastMap;
import org.ofbiz.base.util.UtilNumber;


fromDate = parameters.fromOrderDate;
thruDate = parameters.thruOrderDate;
cashierUserLoginId = parameters.cashierUserLoginId;

reportDataList =[];


fromDateTimestamp = ObjectType.simpleTypeConvert(fromDate, "Timestamp", null, timeZone, locale, false);
thruDateTimestamp = ObjectType.simpleTypeConvert(thruDate, "Timestamp", null, timeZone, locale, false);
dayEnd = UtilDateTime.getDayEnd(fromDateTimestamp, timeZone, locale);
	if (fromDateTimestamp.equals(thruDateTimestamp)) {
		thruDateTimestamp = dayEnd;
	}
context.thruDateTimestamp =thruDateTimestamp;
while(fromDateTimestamp.before(thruDateTimestamp) || fromDateTimestamp.equals(thruDateTimestamp)){
		nextDateTimestamp = UtilDateTime.getNextDayStart(fromDateTimestamp);
        Map reportMap = FastMap.newInstance();
		cashTotal=BigDecimal.ZERO;
		creditTotal = BigDecimal.ZERO;
		returnTotal = BigDecimal.ZERO;
		
cashReportCondition = EntityCondition.makeCondition([EntityCondition.makeCondition("createdDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDateTimestamp),
                        EntityCondition.makeCondition("createdDate", EntityOperator.LESS_THAN_EQUAL_TO, nextDateTimestamp),
                        EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, "CASH"),
						EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER")
                        ],
                    	EntityOperator.AND);                    
creditReportCondition = EntityCondition.makeCondition([EntityCondition.makeCondition("createdDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDateTimestamp),
                        EntityCondition.makeCondition("createdDate", EntityOperator.LESS_THAN_EQUAL_TO, nextDateTimestamp),
                        EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, "CREDIT_CARD"),
						EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER")
                        ],EntityOperator.AND);
returnReportCondition = EntityCondition.makeCondition([EntityCondition.makeCondition("entryDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDateTimestamp),
                        EntityCondition.makeCondition("entryDate", EntityOperator.LESS_THAN_EQUAL_TO, nextDateTimestamp)],
                    	EntityOperator.AND);
                    	returnList = delegator.findList("ReturnHeader", returnReportCondition, null, null, null, false);
						for (GenericValue item: returnList) {
						returnInfo = delegator.findOne("ReturnHeader",[returnId:item.returnId],false);
						returnItems = delegator.findByAnd("ReturnItem",[returnId:item.returnId]);
						if (UtilValidate.isNotEmpty(returnItems)) {
							for(GenericValue listItem: returnItems) {
							 	returnTotal = listItem.getBigDecimal("returnPrice").add(returnTotal);
							}
						}
						}                    
						cashReportItems = delegator.findList("OrderCasheirPaymentSummary",cashReportCondition, null, null, null, false);
			    		if(UtilValidate.isNotEmpty(cashReportItems)) {
							for(GenericValue listItem: cashReportItems) {
							 	cashTotal = listItem.getBigDecimal("maxAmount").add(cashTotal);
							}
							
						}
			reportMap.paymentMethodTypeId = "CASH";
			reportMap.cashTotal = cashTotal;
			reportMap.returnTotal = returnTotal;
				creditReportItems = delegator.findList("OrderCasheirPaymentSummary",creditReportCondition, null, null, null, false);
    				if(UtilValidate.isNotEmpty(creditReportItems)) {
						for(GenericValue listItem: creditReportItems) {
						 	creditTotal = listItem.getBigDecimal("maxAmount").add(creditTotal);
						}
						
			}
			reportMap.paymentMethodTypeId = "CREDIT_CARD";
			reportMap.creditTotal = creditTotal;
			netTotal = cashTotal.add(creditTotal);
			cashTotal=BigDecimal.ZERO;
			creditTotal = BigDecimal.ZERO;
		    reportMap.netTotal = netTotal;
			reportMap.createdDate =   UtilDateTime.toDateString(fromDateTimestamp);
			reportMap.createdTime =   UtilDateTime.toTimeString(fromDateTimestamp);
			fromDateTimestamp = UtilDateTime.getNextDayStart(fromDateTimestamp);
			reportDataList.add(reportMap);
			

}
context.reportDataList =reportDataList;

           