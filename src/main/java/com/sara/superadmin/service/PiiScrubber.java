package com.sara.superadmin.service;

import java.util.regex.Pattern;

/**
 * Best-effort PII removal before error text is sent to a third-party LLM. Redacts
 * emails, phone numbers, card-like numbers, and long digit runs. Not perfect, but
 * keeps obvious customer data out of prompts.
 */
final class PiiScrubber {

	private PiiScrubber() {}

	private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}");
	private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");
	private static final Pattern PHONE = Pattern.compile("\\b(?:\\+?\\d{1,3}[ -]?)?\\d{10}\\b");
	private static final Pattern LONG_DIGITS = Pattern.compile("\\b\\d{7,}\\b");

	static String scrub(String text) {
		if (text == null || text.isBlank()) {
			return text;
		}
		String out = EMAIL.matcher(text).replaceAll("[EMAIL]");
		out = CARD.matcher(out).replaceAll("[CARD]");
		out = PHONE.matcher(out).replaceAll("[PHONE]");
		out = LONG_DIGITS.matcher(out).replaceAll("[NUM]");
		return out;
	}

	/** Trim to a max length to bound prompt size / storage. */
	static String truncate(String text, int max) {
		if (text == null) {
			return null;
		}
		return text.length() <= max ? text : text.substring(0, max) + "…[truncated]";
	}
}
