package com.example.simpletalk;

import android.content.Context;

public final class Greeting {
	private static final int RESOURCE_ID = R.xml.greeting_map;
	private static final int[] greetingIds = {
		R.string.hello,
		R.string.evening,
		R.string.morning,
	};
	private static final String DELIMITER = ";";
	
	/**
	 * Return the response word for given greeting
	 * @param context context
	 * @param greeting the greeting word
	 * @return the response for the parameter
	 */
	public static String getResponse(Context context, String greeting) {
		for (int i=0; i<greetingIds.length; i++) {
			String words = context.getResources().getString(greetingIds[i]);
			if (words.contains(greeting)) {
				// these are also response words
				String[] response = words.split(DELIMITER);
				return response[0];
			}
		}
		return null;
	}
}
