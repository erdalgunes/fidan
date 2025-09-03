package com.erdalgunes.fidan.utils

import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UrlUtilsTest {
    
    @Test
    fun `isValidUrl returns true for valid HTTP URL`() {
        assertTrue(UrlUtils.isValidUrl("http://example.com"))
    }
    
    @Test
    fun `isValidUrl returns true for valid HTTPS URL`() {
        assertTrue(UrlUtils.isValidUrl("https://example.com"))
    }
    
    @Test
    fun `isValidUrl returns true for GitHub sponsors URL`() {
        assertTrue(UrlUtils.isValidUrl("https://github.com/sponsors/erdalgunes"))
    }
    
    @Test
    fun `isValidUrl returns false for invalid scheme`() {
        assertFalse(UrlUtils.isValidUrl("ftp://example.com"))
        assertFalse(UrlUtils.isValidUrl("file://path/to/file"))
    }
    
    @Test
    fun `isValidUrl returns false for malformed URL`() {
        assertFalse(UrlUtils.isValidUrl("not-a-url"))
        assertFalse(UrlUtils.isValidUrl(""))
        assertFalse(UrlUtils.isValidUrl("https://"))
    }
    
    @Test
    fun `isValidUrl returns false for URL without host`() {
        assertFalse(UrlUtils.isValidUrl("https://"))
        assertFalse(UrlUtils.isValidUrl("http://"))
    }
    
    @Test
    fun `isValidUrl handles null and empty strings`() {
        assertFalse(UrlUtils.isValidUrl(""))
    }
    
    @Test
    fun `isValidUrl returns false for URLs with invalid characters`() {
        assertFalse(UrlUtils.isValidUrl("https://example.com with spaces"))
    }
    
    @Test
    fun `isValidUrl returns true for URLs with paths and query parameters`() {
        assertTrue(UrlUtils.isValidUrl("https://example.com/path?param=value"))
        assertTrue(UrlUtils.isValidUrl("https://github.com/erdalgunes/fidan/wiki/Transparency-Report"))
    }
}