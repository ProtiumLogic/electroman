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


reportBy = parameters.reportBy;
exportType = parameters.exportType;
reportType = parameters.reportType;

if (UtilValidate.isEmpty(parameters.fromDate)) {
    request.setAttribute("_ERROR_MESSAGE_", "Please select From Date.");
    return "error";
}

if (UtilValidate.isEmpty(parameters.thruDate)) {
    request.setAttribute("_ERROR_MESSAGE_", "Please select Thru Date.");
    return "error";
}

if (exportType == "pdf") {
		if(reportType == "summary"){
		    return "SUMMARY_PDF";
		}
		else if(reportType == "inDetail"){
			return "INDETAIL_PDF";
		}
		else {
        request.setAttribute("_ERROR_MESSAGE_", "Please select Report By.");
        return "error";
    		}
    } 
    else if (exportType == "csv"){
	        if(reportType == "summary"){
		    return "SUMMARY_CSV";
		}
		else if(reportType == "inDetail"){
			return "INDETAIL_CSV";
		}
		else {
        request.setAttribute("_ERROR_MESSAGE_", "Please select Report By.");
        return "error";
    		}
    }
     else {
        request.setAttribute("_ERROR_MESSAGE_", "Please select Export By.");
        return "error";
    }

