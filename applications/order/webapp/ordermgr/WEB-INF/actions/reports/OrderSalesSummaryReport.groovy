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
oppDataList =[];

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
		giftTotal = BigDecimal.ZERO;
		
		reportCondition = EntityCondition.makeCondition([EntityCondition.makeCondition("orderDate", 
										EntityOperator.GREATER_THAN_EQUAL_TO, fromDateTimestamp),
                        	EntityCondition.makeCondition("orderDate", EntityOperator.LESS_THAN, nextDateTimestamp),
							EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER")
                        	],EntityOperator.AND);                    
		
		returnReportCondition = EntityCondition.makeCondition([EntityCondition.makeCondition("entryDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDateTimestamp),
		                        EntityCondition.makeCondition("entryDate", EntityOperator.LESS_THAN, nextDateTimestamp)],
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
		reportMap.returnTotal = returnTotal;
		reportItems = delegator.findList("OrderHeader",reportCondition, null, null, null, false);
		if(UtilValidate.isNotEmpty(reportItems)) {
			for(GenericValue listItem: reportItems) {
				cashCond = EntityCondition.makeCondition([EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, "CASH"),
																  EntityCondition.makeCondition("orderId", EntityOperator.EQUALS,listItem.orderId )
                        										  ],EntityOperator.AND);
				opps = delegator.findList("OrderPaymentPreference",cashCond,null,null,null,false);
				for(GenericValue item : opps) {
					oib = delegator.findByAnd("OrderItemBilling",[orderId:item.orderId]);
					if(oib) {
						cashTotal =  item.getBigDecimal("maxAmount").add(cashTotal);
					}
				}
			}
			reportMap.paymentMethodTypeId = "CASH";
			reportMap.cashTotal = cashTotal;
			for(GenericValue listItem: reportItems) {
				creditCond = EntityCondition.makeCondition([EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, "CREDIT_CARD"),
																	  EntityCondition.makeCondition("orderId", EntityOperator.EQUALS,listItem.orderId )
	                        										  ],EntityOperator.AND);
				opps = delegator.findList("OrderPaymentPreference",creditCond,null,null,null,false);
				for(GenericValue item : opps) {
					oib = delegator.findByAnd("OrderItemBilling",[orderId:item.orderId]);
					if(oib) {
						creditTotal =  item.getBigDecimal("maxAmount").add(creditTotal);
					}
				}
			}
		reportMap.paymentMethodTypeId = "CREDIT_CARD";
		reportMap.creditTotal = creditTotal;
		for(GenericValue listItem: reportItems) {
				gifitCond = EntityCondition.makeCondition([EntityCondition.makeCondition("paymentMethodTypeId", EntityOperator.EQUALS, "GIFT_CARD"),
																	  EntityCondition.makeCondition("orderId", EntityOperator.EQUALS,listItem.orderId )
	                        										  ],EntityOperator.AND);
				opps = delegator.findList("OrderPaymentPreference",gifitCond,null,null,null,false);
				for(GenericValue item : opps) {
					oib = delegator.findByAnd("OrderItemBilling",[orderId:item.orderId]);
					if(oib) {
						giftTotal =  item.getBigDecimal("maxAmount").add(creditTotal);
					}
				}
			}
		}
		reportMap.paymentMethodTypeId = "GIFT_CARD";
		reportMap.giftTotal = giftTotal;
		netTotal = cashTotal.add(creditTotal);
		netTotal = netTotal.add(giftTotal);
		cashTotal=BigDecimal.ZERO;
		creditTotal = BigDecimal.ZERO;
		giftTotal = BigDecimal.ZERO;
		reportMap.netTotal = netTotal;
		reportMap.createdDate =   UtilDateTime.toDateString(fromDateTimestamp);
		reportMap.createdTime =   UtilDateTime.toTimeString(fromDateTimestamp);
		fromDateTimestamp = UtilDateTime.getNextDayStart(fromDateTimestamp);
		reportDataList.add(reportMap);
}
context.reportDataList=reportDataList;

           