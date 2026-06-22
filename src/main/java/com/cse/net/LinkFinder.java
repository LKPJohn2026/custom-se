package com.cse.net;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds HTTP(S) URLs from the anchor tags within HTML code.
 *
 */
public class LinkFinder {
	/**
	 * Determines whether the URI provided uses the HTTP or HTTPS protocol or scheme
	 * (case-insensitive).
	 *
	 * @param uri the URI to check
	 * @return true if the URI uses the HTTP or HTTPS protocol (or scheme)
	 */
	public static boolean isHttp(URI uri) {
		if (uri != null) {
			String scheme = uri.getScheme();
			return scheme != null && scheme.matches("(?i)http[s]?");
		} else {
			return false;
		}
	}


	/**
	 * Finds all the valid HTTP(S) links in the HREF attribute of the anchor tags in
	 * the provided HTML. The links will be converted to an absolute URI using the
	 * base URI and cleaned.
	 *
	 * Any links that do not use the HTTP/S protocol or are unable to be properly
	 * parsed for any reason will not be included.
	 *
	 * @param base the base URI used to convert to absolute URIs
	 * @param html the raw HTML associated with the base URI
	 * @param links the data structure to store found HTTP(S) links
	 *
	 * @see Pattern#compile(String)
	 * @see Matcher#find()
	 * @see Matcher#group(int)
	 *
	 * @see #toAbsolute(URI, String)
	 * @see #isHttp(URI)
	 * @see #clean(URI)
	 */
	public static void findLinks(URI base, String html, Collection<URI> links) {
		Matcher matcher = Pattern.compile("(?is)<a\\s+[^>]*?href\\s*=\\s*\"([^\"]*)\"[^>]*?>").matcher(html);

		while (matcher.find()) {
			URI uri = toAbsolute(base, matcher.group(1));

			if (uri != null && isHttp(uri)) {
				URI cleaned = clean(uri);
				links.add(cleaned);
			}
		}
	}

	/**
	 * Attempts to create a normalized absolute URI from the provided base URI and
	 * link text without the fragment component if it is included. If the conversion
	 * fails for any reason, will return null.
	 *
	 * @param base the base URI the link text was found on
	 * @param href the link text (usually from an anchor tag href attribute)
	 * @return the normalized absolute URI or {@code null}
	 *
	 * @see #clean(URI)
	 * @see #toUri(String)
	 *
	 * @see URI#resolve(URI)
	 */
	public static URI toAbsolute(URI base, String href) {
		try {
			URI uri = toUri(href);
			return base.isOpaque() ? clean(uri) : clean(base.resolve(uri));
		}
		catch (URISyntaxException e) {
			return null;
		}
	}

	/**
	 * Normalizes and removes the fragment from a URI. For non-opaque hierarchical
	 * URIs, will also make sure the path is default to / if it is missing.
	 *
	 * @param uri the URI to clean
	 * @return the cleaned URI
	 *
	 * @see URI#normalize()
	 * @see URI#isOpaque()
	 */
	public static URI clean(URI uri) {
		URI cleaned = uri.normalize();
		String path = cleaned.getPath();

		try {
			// only non-opque absolute URIs have / as the default path if missing
			cleaned = !cleaned.isOpaque() && cleaned.isAbsolute() && (path == null || path.isBlank())
					? new URI(cleaned.getScheme(), cleaned.getAuthority(), "/", cleaned.getQuery(), null)
					: new URI(cleaned.getScheme(), cleaned.getSchemeSpecificPart(), null);
		}
		catch (URISyntaxException ignored) {
			assert false; // should never occur
		}

		return cleaned;
	}

	/**
	 * Attempts to create a URI from the provided text, attempting to encode the
	 * link text if the process initially fails. Especially useful when dealing with
	 * relative links that have spaces or other special characters.
	 *
	 * @param link the text value to convert to URI
	 * @return the converted URI
	 * @throws URISyntaxException if unable to create
	 *
	 * @see URI#URI(String)
	 * @see URI#URI(String, String, String)
	 */
	public static URI toUri(String link) throws URISyntaxException {
		try {
			return new URI(link); // only works if no encoding needed
		}
		catch (URISyntaxException e) {
			Matcher matcher = URI_PARTS.matcher(link);

			if (matcher.matches()) { // use constructor that encodes special characters
				return new URI(matcher.group(1), matcher.group(2), matcher.group(3));
			}

			throw e; // re-throw if unable to convert
		}
	}

	/**
	 * Returns a list of all the valid HTTP(S) URIs found in the HREF attribute of
	 * the anchor tags in the provided HTML.
	 *
	 * @param base the base URI used to convert relative links to absolute URIs
	 * @param html the raw HTML associated with the base URL
	 * @return list of all valid HTTP(S) links in the order they were found
	 *
	 * @see #findLinks(URI, String, Collection)
	 */
	public static ArrayList<URI> listUris(URI base, String html) {
		ArrayList<URI> uris = new ArrayList<URI>();
		findLinks(base, html, uris);
		return uris;
	}

	/**
	 * Returns a set of all the unique valid HTTP(S) URIs found in the HREF
	 * attribute of the anchor tags in the provided HTML.
	 *
	 * @param base the base URI used to convert relative URIs to absolute3
	 * @param html the raw HTML associated with the base URI
	 * @return set of all valid and unique HTTP(S) links found
	 *
	 * @see #findLinks(URI, String, Collection)
	 */
	public static HashSet<URI> uniqueUris(URI base, String html) {
		HashSet<URI> uris = new HashSet<URI>();
		findLinks(base, html, uris);
		return uris;
	}

	/**
	 * Regular expression to break URI into high level component parts in the form:
	 *
	 * <pre>
	 * [scheme:]scheme-specific-part[#fragment]
	 * </pre>
	 *
	 * Group 1 is the scheme without the : colon symbol, group 2 is the scheme
	 * specific part, and group 3 is the fragment without the # hash symbol. Groups
	 * may be null.
	 *
	 * <p><em>Warning: Does not validate the URI.</em></p>
	 *
	 * @see URI
	 */
	public static Pattern URI_PARTS = Pattern.compile("^(?:([^:]*):)?([^#]+)?(?:#(.*))?$");

	/**
	 * Demonstrates this class.
	 *
	 * @param args unused
	 * @throws Exception if any issues occur
	 */
	public static void main(String[] args) throws Exception {
		// this demonstrates cleaning
		String pythonLink = "https://docs.python.org/3/library/functions.html?highlight=string#format";
		URI pythonUri = toUri(pythonLink);
		URI pythonClean = clean(pythonUri);

		System.out.println(pythonLink);
		System.out.println(pythonClean);
		System.out.println();

		// this demonstrates encoding
		String googleLink = "https://www.google.com/search?q=hello world";
		URI googleUri = toUri(googleLink);

		System.out.println(googleLink);
		System.out.println(googleUri);
		System.out.println();

		// this demonstrates a non-HTTP (but valid) URI
		String emailLink = "mailto:username@example.edu";
		URI emailUri = toUri(emailLink);
		System.out.println(emailLink);
		System.out.println(emailUri);
		System.out.println();

		// this demonstrates converting to absolute
		String relative = "index.html?q=hello world#top";
		System.out.println(relative);
		System.out.println(toAbsolute(pythonUri, relative));
		System.out.println(toAbsolute(googleUri, relative));
		System.out.println(toAbsolute(emailUri, relative)); // invalid base
	}

	/** Prevent instantiating this class of static methods. */
	private LinkFinder() {
	}
}
