package com.vibium.bidi.types;

import java.util.List;

/**
 * Information about a browsing context.
 *
 * @param context The browsing context ID
 * @param url The current URL
 * @param children Child browsing contexts
 * @param parent Parent browsing context ID (may be null)
 */
public record BrowsingContextInfo(
        String context,
        String url,
        List<BrowsingContextInfo> children,
        String parent
) {
}
