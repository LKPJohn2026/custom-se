package com.cse.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Captures environment metadata for benchmark reports.
 */
public final class HardwareInfo {
	private final String cpu;
	private final String memory;
	private final String javaVersion;

	public HardwareInfo(String cpu, String memory, String javaVersion) {
		this.cpu = cpu == null || cpu.isBlank() ? "unknown CPU" : cpu.strip();
		this.memory = memory == null || memory.isBlank() ? "unknown RAM" : memory.strip();
		this.javaVersion = javaVersion == null || javaVersion.isBlank() ? "unknown JVM" : javaVersion.strip();
	}

	public static HardwareInfo capture() {
		return new HardwareInfo(readCpuModel(), readMemoryTotal(), System.getProperty("java.version"));
	}

	public String summary() {
		return cpu + " / " + memory + " / JDK " + javaVersion;
	}

	public String cpu() {
		return cpu;
	}

	public String memory() {
		return memory;
	}

	public String javaVersion() {
		return javaVersion;
	}

	private static String readCpuModel() {
		Optional<String> fromProc = readProcKey("/proc/cpuinfo", "model name");
		if (fromProc.isPresent()) {
			return fromProc.get();
		}
		String arch = System.getProperty("os.arch", "unknown");
		return Runtime.getRuntime().availableProcessors() + " cores (" + arch + ")";
	}

	private static String readMemoryTotal() {
		Optional<String> fromProc = readProcKey("/proc/meminfo", "MemTotal");
		if (fromProc.isPresent()) {
			return fromProc.get();
		}
		long maxMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
		return maxMb + " MB JVM max heap";
	}

	private static Optional<String> readProcKey(String path, String keyPrefix) {
		Path proc = Path.of(path);
		if (!Files.isRegularFile(proc)) {
			return Optional.empty();
		}
		try (Stream<String> lines = Files.lines(proc)) {
			return lines
					.filter(line -> line.startsWith(keyPrefix))
					.map(line -> line.substring(line.indexOf(':') + 1).strip())
					.findFirst();
		} catch (IOException e) {
			return Optional.empty();
		}
	}

	static String formatBytes(long bytes) {
		if (bytes < 0) {
			return "0 B";
		}
		double value = bytes;
		String[] units = { "B", "KB", "MB", "GB", "TB" };
		int unit = 0;
		while (value >= 1024 && unit < units.length - 1) {
			value /= 1024;
			unit++;
		}
		if (unit == 0) {
			return String.format(Locale.US, "%d %s", bytes, units[unit]);
		}
		return String.format(Locale.US, "%.2f %s", value, units[unit]);
	}
}
