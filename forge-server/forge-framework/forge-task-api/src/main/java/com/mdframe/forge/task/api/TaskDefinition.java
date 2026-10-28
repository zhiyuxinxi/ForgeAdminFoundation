package com.mdframe.forge.task.api;

/** Immutable task definition version in the authenticated tenant. */
public record TaskDefinition(long id, String code, int version, String title, String sourceType) { }
