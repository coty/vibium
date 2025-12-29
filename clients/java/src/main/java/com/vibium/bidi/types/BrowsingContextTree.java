package com.vibium.bidi.types;

import java.util.List;

/**
 * Tree of browsing contexts returned by getTree command.
 */
public record BrowsingContextTree(List<BrowsingContextInfo> contexts) {
}
