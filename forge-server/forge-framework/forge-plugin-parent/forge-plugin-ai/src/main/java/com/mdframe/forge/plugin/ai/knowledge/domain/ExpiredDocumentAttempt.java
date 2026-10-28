package com.mdframe.forge.plugin.ai.knowledge.domain;

/** Minimal recovery projection; does not load document content or online user credentials. */
public record ExpiredDocumentAttempt(Long id, Long tenantId, String attempt) { }
