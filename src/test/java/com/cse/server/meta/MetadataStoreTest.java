package com.cse.server.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MetadataStoreTest {
	@Test
	public void testTopQueries() {
		MetadataStore store = new MetadataStore();
		store.recordQuery("hello");
		store.recordQuery("hello");
		store.recordQuery("world");
		var top = store.topQueries(5);
		assertEquals("hello", top.get(0).getKey());
		assertEquals(2L, top.get(0).getValue().longValue());
	}

	@Test
	public void testTopVisits() {
		MetadataStore store = new MetadataStore();
		store.recordVisit("http://a");
		store.recordVisit("http://b");
		store.recordVisit("http://a");
		var top = store.topVisits(5);
		assertEquals("http://a", top.get(0).getKey());
		assertEquals(2L, top.get(0).getValue().longValue());
	}
}
