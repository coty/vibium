package com.vibium.bidi.types;

import java.util.List;

/**
 * The result of browsingContext.getTree command.
 *
 * @param contexts List of top-level browsing contexts
 */
public record BrowsingContextTree(List<BrowsingContextInfo> contexts) {
}
