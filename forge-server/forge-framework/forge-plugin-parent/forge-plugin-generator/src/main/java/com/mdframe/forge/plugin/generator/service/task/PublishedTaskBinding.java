package com.mdframe.forge.plugin.generator.service.task;

/** Immutable content stored inside an existing low-code published version. */
public record PublishedTaskBinding(long id, String key, TaskBindingSpec spec) { }
