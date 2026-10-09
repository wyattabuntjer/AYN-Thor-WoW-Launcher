package app.gamenative.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdaterTest {

    @Test
    fun isNewer_higherPatch_returnsTrue() {
        assertTrue(AppUpdater.isNewer("v2.1.3", "2.1.2"))
    }

    @Test
    fun isNewer_higherMinor_returnsTrue() {
        assertTrue(AppUpdater.isNewer("2.2.0", "2.1.2"))
    }

    @Test
    fun isNewer_higherMajor_returnsTrue() {
        assertTrue(AppUpdater.isNewer("v3.0.0", "2.1.2"))
    }

    @Test
    fun isNewer_sameVersion_returnsFalse() {
        assertFalse(AppUpdater.isNewer("v2.1.2", "2.1.2"))
        assertFalse(AppUpdater.isNewer("2.1.2", "2.1.2"))
    }

    @Test
    fun isNewer_olderVersion_returnsFalse() {
        assertFalse(AppUpdater.isNewer("v2.1.1", "2.1.2"))
        assertFalse(AppUpdater.isNewer("1.9.9", "2.1.2"))
    }

    @Test
    fun isNewer_extraSegments_comparesCorrectly() {
        assertTrue(AppUpdater.isNewer("v2.1.2.1", "2.1.2"))
        assertFalse(AppUpdater.isNewer("2.1", "2.1.2"))
    }

    @Test
    fun isNewer_handlesPrereleaseSuffix() {
        assertFalse(AppUpdater.isNewer("v2.1.2-beta1", "2.1.2"))
        assertTrue(AppUpdater.isNewer("v2.1.3-rc1", "2.1.2"))
    }

    @Test
    fun parseRelease_validNewReleaseWithApk_parsesSuccessfully() {
        val json = """
            {
                "tag_name": "v2.1.3",
                "name": "WoW Forever v2.1.3",
                "body": "Bug fixes and improvements",
                "assets": [
                    {
                        "name": "WoW-Forever.apk",
                        "browser_download_url": "https://github.com/jaredgei/wow-forever-android/releases/download/v2.1.3/WoW-Forever.apk",
                        "size": 160000000
                    }
                ]
            }
        """.trimIndent()

        val release = AppUpdater.parseRelease(json, "2.1.2")

        assertNotNull(release)
        assertEquals("2.1.3", release?.version)
        assertEquals("WoW Forever v2.1.3", release?.name)
        assertEquals("Bug fixes and improvements", release?.notes)
        assertEquals("https://github.com/jaredgei/wow-forever-android/releases/download/v2.1.3/WoW-Forever.apk", release?.downloadUrl)
        assertEquals(160000000L, release?.sizeBytes)
    }

    @Test
    fun parseRelease_sameOrOlderVersion_returnsNull() {
        val json = """
            {
                "tag_name": "v2.1.2",
                "name": "WoW Forever v2.1.2",
                "body": "Existing release",
                "assets": [
                    {
                        "name": "WoW-Forever.apk",
                        "browser_download_url": "https://github.com/jaredgei/wow-forever-android/releases/download/v2.1.2/WoW-Forever.apk",
                        "size": 160000000
                    }
                ]
            }
        """.trimIndent()

        assertNull(AppUpdater.parseRelease(json, "2.1.2"))
    }

    @Test
    fun parseRelease_noApkAsset_returnsNull() {
        val json = """
            {
                "tag_name": "v2.1.3",
                "name": "WoW Forever v2.1.3",
                "body": "Source only release",
                "assets": [
                    {
                        "name": "source.tar.gz",
                        "browser_download_url": "https://example.com/source.tar.gz",
                        "size": 5000
                    }
                ]
            }
        """.trimIndent()

        assertNull(AppUpdater.parseRelease(json, "2.1.2"))
    }

    @Test
    fun parseRelease_invalidJson_returnsNull() {
        assertNull(AppUpdater.parseRelease("not-json", "2.1.2"))
    }

    @Test
    fun isNewer_letterSuffixBeta_sitsBetweenReleases() {
        assertTrue(AppUpdater.isNewer("v2.3.3b", "2.3.2"))
        assertFalse(AppUpdater.isNewer("v2.3.3b", "2.3.3"))
        assertTrue(AppUpdater.isNewer("v2.3.3", "2.3.3b"))
        assertFalse(AppUpdater.isNewer("v2.3.2", "2.3.3b"))
        assertTrue(AppUpdater.isNewer("v2.3.3c", "2.3.3b"))
    }

    @Test
    fun isPrerelease_detectsBetaTags() {
        assertTrue(AppUpdater.isPrerelease("v2.3.3b"))
        assertTrue(AppUpdater.isPrerelease("v2.3.3-beta1"))
        assertFalse(AppUpdater.isPrerelease("v2.3.3"))
    }

    private fun releaseJson(tag: String, prerelease: Boolean = false) = """
        {"tag_name": "$tag", "name": "$tag", "body": "", "prerelease": $prerelease,
         "assets": [{"name": "app.apk", "browser_download_url": "https://example.com/$tag.apk", "size": 1000}]}
    """.trimIndent()

    @Test
    fun parseRelease_beta_hiddenUnlessOptedIn() {
        assertNull(AppUpdater.parseRelease(releaseJson("v2.3.3b", prerelease = true), "2.3.2"))
        // Even if someone forgets to flag it as a pre-release, the tag keeps it from stable users.
        assertNull(AppUpdater.parseRelease(releaseJson("v2.3.3b"), "2.3.2"))
        assertEquals("2.3.3b", AppUpdater.parseRelease(releaseJson("v2.3.3b", true), "2.3.2", includeBeta = true)?.version)
    }

    @Test
    fun parseReleaseList_picksNewestUsableRelease() {
        val list = "[" + listOf(releaseJson("v2.3.3b", true), releaseJson("v2.3.2"), releaseJson("v2.3.1")).joinToString(",") + "]"
        assertEquals("2.3.3b", AppUpdater.parseReleaseList(list, "2.3.2", includeBeta = true)?.version)
        assertNull(AppUpdater.parseReleaseList(list, "2.3.2", includeBeta = false))
        assertEquals("2.3.3b", AppUpdater.parseReleaseList(list, "2.3.1", includeBeta = true)?.version)
    }
}
