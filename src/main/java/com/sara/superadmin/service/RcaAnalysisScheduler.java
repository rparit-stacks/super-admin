package com.sara.superadmin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically analyzes NEW incidents. Decoupled from ingest so ingest stays fast and
 * LLM concurrency is bounded by the sweep size.
 */
@Component
public class RcaAnalysisScheduler {

	private static final Logger log = LoggerFactory.getLogger(RcaAnalysisScheduler.class);

	private final AiAnalysisService analysisService;
	private final int maxPerSweep;

	public RcaAnalysisScheduler(AiAnalysisService analysisService,
								@Value("${rca.analysis.max-per-sweep:10}") int maxPerSweep) {
		this.analysisService = analysisService;
		this.maxPerSweep = maxPerSweep;
	}

	@Scheduled(fixedDelayString = "${rca.analysis.interval-ms:120000}")
	public void sweep() {
		try {
			int done = analysisService.analyzeNewIncidents(maxPerSweep);
			if (done > 0) {
				log.info("RCA sweep analyzed {} incident(s)", done);
			}
		} catch (Exception e) {
			log.debug("RCA sweep error: {}", e.getMessage());
		}
	}
}
