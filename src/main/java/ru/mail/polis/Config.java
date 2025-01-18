package ru.mail.polis;

import java.io.File;

public record Config(File directory, long maxMemTableContentSizeInBytes, float memTableLoadFactor) {
}
