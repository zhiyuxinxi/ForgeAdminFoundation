package com.mdframe.forge.plugin.ai.agent.engine.hitl;

/** An exact model-produced call, retained without requesting a replacement from the model. */
public record PendingToolCall(String name, String arguments) { }
