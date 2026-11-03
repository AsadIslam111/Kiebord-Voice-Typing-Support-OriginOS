package io.kiebord.theme

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ThemeZipTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    @Test
    fun testIsZipBytesDetection() {
        val zipMagic = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x00)
        assertTrue(ThemeZipUtils.isZipBytes(zipMagic))

        val emptyZipMagic = byteArrayOf(0x50, 0x4B, 0x05, 0x06, 0x00)
        assertTrue(ThemeZipUtils.isZipBytes(emptyZipMagic))

        val jsonBytes = "{\"schemaVersion\": 2}".toByteArray(Charsets.UTF_8)
        assertFalse(ThemeZipUtils.isZipBytes(jsonBytes))

        val shortBytes = byteArrayOf(0x50, 0x4B)
        assertFalse(ThemeZipUtils.isZipBytes(shortBytes))
    }

    @Test
    fun testZipSlipProtection() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val evilEntry = ZipEntry("../../../evil.txt")
            zos.putNextEntry(evilEntry)
            zos.write("malicious payload".toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        val tempDir = Files.createTempDirectory("theme_zip_slip_test").toFile()
        try {
            var caught = false
            try {
                ThemeZipUtils.extractZipSafe(baos.toByteArray(), tempDir)
            } catch (e: SecurityException) {
                caught = true
            }
            assertTrue("Expected ZipSlip security exception", caught)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testZipPackageCreationExtractionAndAutoDiscovery() {
        val tempWorkDir = Files.createTempDirectory("theme_pack_test").toFile()
        val tempExtractDir = Files.createTempDirectory("theme_extract_test").toFile()

        try {
            // 1. Create a mock theme package directory with assets
            val bgFile = File(tempWorkDir, "wallpaper.png").apply {
                writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
            }
            val fontFile = File(tempWorkDir, "custom.ttf").apply {
                writeBytes(byteArrayOf(0x00, 0x01, 0x00, 0x00, 0x00))
            }
            val iconsDir = File(tempWorkDir, "icons").apply { mkdirs() }
            val shiftIconFile = File(iconsDir, "shift.png").apply {
                writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
            }
            val backspaceIconFile = File(iconsDir, "backspace.png").apply {
                writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
            }
            val actionIconFile = File(iconsDir, "action.png").apply {
                writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
            }

            val sourceSpec = KeyboardThemeSpec(
                schemaVersion = 2,
                id = "cyber_zip_test",
                name = "Cyber Zip Theme",
                isDark = true,
                background = ThemeBackgroundSpec(
                    image = ImageBackgroundSpec(
                        imagePath = bgFile.absolutePath,
                        scaleType = ImageScaleType.CROP,
                        blurRadiusDp = 12f
                    )
                ),
                fontPath = fontFile.absolutePath,
                icons = ThemeIconsSpec(
                    shift = shiftIconFile.absolutePath,
                    backspace = backspaceIconFile.absolutePath,
                    action = actionIconFile.absolutePath
                ),
                flatKeyLabelColor = "#FFFFFF",
                activeOptionHighlightColor = "#00FFFF"
            )

            // 2. Package into a .bornotheme ZIP
            val zipFile = File(tempWorkDir, "cyber_zip_test.bornotheme")
            ThemeZipUtils.packageThemeZip(sourceSpec, zipFile, json)

            assertTrue(zipFile.exists())
            assertTrue(zipFile.length() > 0)
            assertTrue(ThemeZipUtils.isZipBytes(zipFile.readBytes()))

            // 3. Extract safe
            val extractedFiles = ThemeZipUtils.extractZipSafe(zipFile.readBytes(), tempExtractDir)
            assertTrue(extractedFiles.any { it.name == "theme.json" })
            assertTrue(extractedFiles.any { it.name.startsWith("background") })
            assertTrue(extractedFiles.any { it.name.startsWith("font") })
            assertTrue(extractedFiles.any { it.name == "shift.png" })

            // 4. Discover and normalize
            val normalizedSpec = ThemeZipUtils.discoverAndNormalizeThemePackage(tempExtractDir, json)
            assertEquals("cyber_zip_test", normalizedSpec.id)
            assertEquals("Cyber Zip Theme", normalizedSpec.name)
            assertEquals(2, normalizedSpec.schemaVersion)
            assertEquals("#FFFFFF", normalizedSpec.flatKeyLabelColor)
            assertEquals("#00FFFF", normalizedSpec.activeOptionHighlightColor)

            // Verify paths are normalized to absolute local files in the target directory
            assertNotNull(normalizedSpec.background.image?.imagePath)
            assertTrue(File(normalizedSpec.background.image!!.imagePath!!).isAbsolute)
            assertTrue(File(normalizedSpec.background.image!!.imagePath!!).exists())

            assertNotNull(normalizedSpec.fontPath)
            assertTrue(File(normalizedSpec.fontPath!!).isAbsolute)
            assertTrue(File(normalizedSpec.fontPath!!).exists())

            assertNotNull(normalizedSpec.icons.shift)
            assertTrue(File(normalizedSpec.icons.shift!!).isAbsolute)
            assertTrue(File(normalizedSpec.icons.shift!!).exists())

            assertNotNull(normalizedSpec.icons.backspace)
            assertTrue(File(normalizedSpec.icons.backspace!!).exists())

            assertNotNull(normalizedSpec.icons.action)
            assertTrue(File(normalizedSpec.icons.action!!).exists())

            // 5. Test conversion to KeyboardColorScheme
            val scheme = normalizedSpec.toKeyboardColorScheme()
            assertEquals(normalizedSpec.fontPath, scheme.fontPath)
            assertEquals("#00FFFF", normalizedSpec.activeOptionHighlightColor)
            assertEquals(0x00 / 255f, scheme.effectiveActiveOptionHighlight.red, 0.01f)
            assertEquals(1.0f, scheme.effectiveActiveOptionHighlight.green, 0.01f)
            assertEquals(1.0f, scheme.effectiveActiveOptionHighlight.blue, 0.01f)

            // Test custom icon resolution
            val charKey = io.kiebord.common.model.KeyDef(label = "a")
            val shiftKey = io.kiebord.common.model.KeyDef(type = io.kiebord.common.model.KeyType.SHIFT)
            val actionKey = io.kiebord.common.model.KeyDef(type = io.kiebord.common.model.KeyType.ACTION)
            val deleteKey = io.kiebord.common.model.KeyDef(type = io.kiebord.common.model.KeyType.DELETE)

            assertEquals(normalizedSpec.icons.shift, scheme.getCustomIcon(shiftKey))
            assertEquals(normalizedSpec.icons.action, scheme.getCustomIcon(actionKey))
            assertEquals(normalizedSpec.icons.backspace, scheme.getCustomIcon(deleteKey))
            assertNull(scheme.getCustomIcon(charKey))
        } finally {
            tempWorkDir.deleteRecursively()
            tempExtractDir.deleteRecursively()
        }
    }

    @Test
    fun testAutoDiscoveryWithoutExplicitDeclaration() {
        val tempDir = Files.createTempDirectory("theme_auto_discover").toFile()
        try {
            // Write a minimal theme.json without fontPath, background, or icons declared
            val minimalJson = """
                {
                  "schemaVersion": 2,
                  "id": "auto_detect_theme",
                  "name": "Auto Detect Theme",
                  "background": {
                    "solidColor": "#121212"
                  }
                }
            """.trimIndent()
            File(tempDir, "theme.json").writeText(minimalJson)

            // Put companion assets directly in the directory
            val bgFile = File(tempDir, "background.jpg").apply { writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())) }
            val fontFile = File(tempDir, "font.otf").apply { writeBytes(byteArrayOf(0x4F, 0x54, 0x54, 0x4F)) }
            val iconsDir = File(tempDir, "icons").apply { mkdirs() }
            val deleteIcon = File(iconsDir, "backspace.png").apply { writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)) }

            val spec = ThemeZipUtils.discoverAndNormalizeThemePackage(tempDir, json)
            assertEquals("auto_detect_theme", spec.id)
            assertEquals(bgFile.absolutePath, spec.background.image?.imagePath)
            assertEquals(fontFile.absolutePath, spec.fontPath)
            assertEquals(deleteIcon.absolutePath, spec.icons.backspace)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}

