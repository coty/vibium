package com.vibium.bidi.types;

import java.util.List;

/**
 * Information about a browsing context.
 */
public record BrowsingContextInfo(
    String context,
    String url,
    List<BrowsingContextInfo> children,
    String parent
) {
}
