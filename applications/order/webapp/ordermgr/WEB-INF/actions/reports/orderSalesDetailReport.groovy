import java.math.BigDecimal;
import java.util.*;
import java.sql.Timestamp;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.base.util.*;
import javolution.util.FastMap;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.accounting.invoice.*;

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
		reportCondition = EntityCondition.makeCondition([EntityCondition.makeCondition("createdDate", EntityOperator.GREATER_THAN_EQUAL_TO, fromDateTimestamp),
                        EntityCondition.makeCondition("createdDate", EntityOperator.LESS_THAN_EQUAL_TO, thruDateTimestamp),
                        EntityCondition.makeCondition("createdBy", EntityOperator.EQUALS,cashierUserLoginId)],
                    	EntityOperator.AND);                    
					reportItems = delegator.findList("OrderCasheirPaymentSummary",reportCondition, null, null, null, false);
		    		if(UtilValidate.isNotEmpty(reportItems)) {
						for(GenericValue listItem: reportItems) {
							Map reportMap = FastMap.newInstance();
						   	reportMap.paymentMethodTypeId = listItem.paymentMethodTypeId;
						   	reportMap.invoiceTotal = listItem.maxAmount;
						   	reportMap.createdDate = UtilDateTime.toDateString(listItem.createdDate);
							invoiceInfoList = delegator.findByAnd("OrderItemBilling",[orderId:listItem.orderId]);
							if (UtilValidate.isNotEmpty(invoiceInfoList)) {
							invoiceDetail = EntityUtil.getFirst(invoiceInfoList);
							reportMap.invoiceId = invoiceDetail.invoiceId;
							invoiceInfo = delegator.findOne("Invoice",[invoiceId:invoiceDetail.invoiceId],false);
							invTotal = InvoiceWorker.getInvoiceTotal(invoiceInfo);
							reportMap.amount = invTotal;
							}
							reportMap.cashierId = cashierUserLoginId;
							reportDataList.add(reportMap);
						  }						
					}
context.reportDataList1 =reportDataList;
           