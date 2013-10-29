import java.util.Date;
import java.sql.Timestamp;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.base.util.*;
import javolution.util.FastMap;


facilityList = [];
facilityMapList =[];
quoteId = parameters.quoteId;
		println("##############@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@22#########################"+quoteId);

		facilityList.add(UtilProperties.getPropertyValue("general.properties", "electroman.showroom.facility"));
		facilityList.add(UtilProperties.getPropertyValue("general.properties", "electroman.displaylocation.facility"));
		facilityList.add(UtilProperties.getPropertyValue("general.properties", "electroman.backdoor.facility"));
		facilityList.add(UtilProperties.getPropertyValue("general.properties", "electroman.mainwarehouse.facility"));
		facilityList.add(UtilProperties.getPropertyValue("general.properties", "electroman.consignee.facility"));
		
		productId = parameters.productId;
		if (productId) {
			for (String facilityId : facilityList ) {
					Map resultMap = FastMap.newInstance();
			        stockResult = dispatcher.runSync("getInventoryAvailableByFacility", [productId:productId , facilityId:facilityId]);
			        resultMap.facilityId = facilityId;
			        resultMap.availableToPromiseTotal = stockResult.availableToPromiseTotal;
			        facilityMapList.add(resultMap);
			}			
		context.facilityList = facilityList;
		context.facilityMapList = facilityMapList;
		context.productId = productId;
		context.quoteId = quoteId;
		
		return "success";
		
		}