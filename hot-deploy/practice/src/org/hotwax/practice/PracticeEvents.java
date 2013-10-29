package org.hotwax.practice;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;

import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericServiceException;

public class PracticeEvents {
    
    public static final String module = PracticeEvents.class.getName();
     
    public static String createPracticePersonJavaEvent(HttpServletRequest request, HttpServletResponse response) {
           LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
           GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
           GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
           
           String salutation = (String) request.getParameter("salutation");
           String firstName = (String) request.getParameter("firstName");
           String middleName=(String) request.getParameter("middleName");
           String lastName=(String) request.getParameter("lastName");
           String suffix=(String) request.getParameter("suffix");
          
//         Map createPersonContext = new HashMap();
//         createPersonContext.put("firstName", firstName);
//         createPersonContext.put("middleName", middleName);
//         createPersonContext.put("lastName", lastName);
//         createPersonContext.put("suffix", suffix);
                       
           Map createPersonCtx = UtilMisc.toMap("salutation", salutation, "firstName", firstName, "middleName", middleName, "lastName", lastName, "suffix", suffix, "userLogin", userLogin);
           try {
               Map person = dispatcher.runSync("createPracticePerson", createPersonCtx);
           } catch (GenericServiceException e) {
               Debug.logError(e.toString(), module);
               return "error";
           }
           return "success";
    }           
            
}
