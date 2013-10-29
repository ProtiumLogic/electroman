/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jaison
 */
package org.ofbiz.accounting.invoice;
import java.util.Locale;
import java.util.Scanner;

import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.text.RuleBasedNumberFormat;

public class NumberToWords {

	 public static String convert(String amountTotal, Locale locale) {
	    	String amountBeforeDecimal = amountTotal.split("\\.")[0];
	    	String amountAfterDecimal = amountTotal.split("\\.")[1];
	    	BigDecimal firstAmount = new BigDecimal(amountBeforeDecimal);
	    	BigDecimal secondAmount = new BigDecimal(amountAfterDecimal);
	    	RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(locale, RuleBasedNumberFormat.SPELLOUT);
	    	String firstString = rbnf.format(firstAmount).toUpperCase();
	    	String secondString = rbnf.format(secondAmount).toUpperCase();String currFormat;
	    	if(!secondString.equals("ZERO")){
	    		currFormat = "OMR" + " " + firstString + " & " + "BAISA" +" " + secondString +" "+ "ONLY";
	    	}else{
	    		currFormat = "OMR" + " " + firstString +" "+ "ONLY";
	    	}

	    	return currFormat;
	    	}
}

	    	
