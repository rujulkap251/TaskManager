package com.teamtaskmanager.utilities;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FormatDate {

	public static Date toDate(String date) {
		try {
			return new SimpleDateFormat("yyyy-MM-dd").parse(date);
		} catch (ParseException e) {
			throw new IllegalArgumentException("Bad date: " + date, e);
		}
	}

}
