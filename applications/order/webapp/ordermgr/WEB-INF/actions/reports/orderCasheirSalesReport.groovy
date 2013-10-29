import java.math.BigDecimal;
import java.util.*;
import java.sql.Timestamp;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.base.util.*;
import javolution.util.FastMap;
import org.ofbiz.base.util.UtilNumber;


fromDate = parameters.fromDate;
thruDate = parameters.thruDate;
cashierUserLoginId = parameters.cashierUserLoginId;
reportType = parameters.reportType;

reportDataList =[];

fromDateTimestamp = ObjectType.simpleTypeConvert(fromDate, "Timestamp", null, timeZone, locale, false);
thruDateTimestamp = ObjectType.simpleTypeConvert(thruDate, "Timestamp", null, timeZone, locale, false);
dayEnd = UtilDateTime.getDayEnd(fromDateTimestamp, timeZone, locale);
	if (fromDateTimestamp.equals(thruDateTimestamp)) {
		thruDateTimestamp = dayEnd;
	}
thruDateTimestamp = UtilDateTime.getDayEnd(thruDateTimestamp, timeZone, locale);
context.thruDateTimestamp =thruDateTimestamp;
while(fromDateTimestamp.before(thruDateTimestamp) || fromDateTimestamp.equals(thruDateTimestamp)){
		nextDateTimestamp = UtilDateTime.getNextDayStart(fromDateTimestamp);
        Map reportMap = FastMap.newInstance();
		cashTotal=BigDecimal.ZERO;
		creditTotal = BigDecimal.ZERO;
		
cashReportCondition = EntityCondition.makeCondition([EntityCondition.makeCondition("createdDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDateTimestamp),
                        EntityCondition.makeCondition("createdDate", EntityOperator.LESS_THAN_EQUAL_TO, nextDateTimestamp),
                        EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, "CASH"),
                        EntityCondition.makeCondition("createdBy", EntityOperator.EQUALS,cashierUserLoginId)],
                    	EntityOperator.AND);                    
creditReportCondition = EntityCondition.makeCondition([EntityCondition.makeCondition("createdDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDateTimestamp),
                        EntityCondition.makeCondition("createdDate", EntityOperator.LESS_THAN_EQUAL_TO, nextDateTimestamp),
                        EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, "CREDIT_CARD"),
                        EntityCondition.makeCondition("createdBy", EntityOperator.EQUALS,cashierUserLoginId)],
                    	EntityOperator.AND);
                    	                    
					cashReportItems = delegator.findList("OrderCasheirPaymentSummary",cashReportCondition, null, null, null, false);
		    		if(UtilValidate.isNotEmpty(cashReportItems)) {
						for(GenericValue listItem: cashReportItems) {
						 	cashTotal = listItem.getBigDecimal("maxAmount").add(cashTotal);
						}						
					}
			reportMap.paymentMethodTypeId = "CASH";
			reportMap.cashTotal = cashTotal;
				creditReportItems = delegator.findList("OrderCasheirPaymentSummary",creditReportCondition, null, null, null, false);
    				if(UtilValidate.isNotEmpty(creditReportItems)) {
						for(GenericValue listItem: creditReportItems) {
						 	creditTotal = listItem.getBigDecimal("maxAmount").add(creditTotal);
						}						
			}
			reportMap.paymentMethodTypeId = "CREDIT_CARD";
			reportMap.creditTotal = creditTotal;
			netTotal = cashTotal.add(creditTotal);
		    reportMap.createdDate =  UtilDateTime.toDateString(fromDateTimestamp);
		    reportMap.netTotal = netTotal;
		    reportMap.cashierId = cashierUserLoginId;
		    reportDataList.add(reportMap);
			fromDateTimestamp = UtilDateTime.getNextDayStart(fromDateTimestamp);

}


context.reportDataList =reportDataList;


           