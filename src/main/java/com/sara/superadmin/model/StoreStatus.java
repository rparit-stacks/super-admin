package com.sara.superadmin.model;

/**
 * Last-known liveness of a store, determined by pinging its health endpoint.
 */
public enum StoreStatus {
	/** Health check returned 2xx. */
	LIVE,
	/** Health check failed / timed out / non-2xx. */
	DOWN,
	/** Never checked yet. */
	UNKNOWN
}
