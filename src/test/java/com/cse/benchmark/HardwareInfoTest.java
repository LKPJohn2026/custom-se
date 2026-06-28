package com.cse.benchmark;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class HardwareInfoTest {
	@Test
	public void testFormatBytes() {
		assertTrue(HardwareInfo.formatBytes(512).contains("512"));
		assertTrue(HardwareInfo.formatBytes(1024).contains("KB"));
	}

	@Test
	public void testCapture() {
		HardwareInfo info = HardwareInfo.capture();
		assertTrue(info.summary().contains("JDK"));
	}
}
