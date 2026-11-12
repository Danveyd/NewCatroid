package org.catrobat.catroid.test.common

import org.catrobat.catroid.common.ChEngineHttpManager
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ChEngineHttpManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testGetRequestAndFormulas() {
        val requestId = "test_get"
        val url = "https://httpbingo.org/get"

        ChEngineHttpManager.createRequest(requestId, "GET", url)

        ChEngineHttpManager.setConfig(requestId, "header", "User-Agent", "ChEngineTestClient")
        ChEngineHttpManager.setConfig(requestId, "query", "test_param", "working")

        val latch = CountDownLatch(1)
        ChEngineHttpManager.executeRequest(requestId) {
            latch.countDown()
        }

        assertTrue("Request timed out", latch.await(15, TimeUnit.SECONDS))

        val code = ChEngineHttpManager.getResponseCode(requestId)
        assertEquals(200, code)

        val text = ChEngineHttpManager.getResponseText(requestId)
        assertTrue("Response doesn't contain header", text.contains("ChEngineTestClient"))
        assertTrue("Response doesn't contain query param", text.contains("working"))
    }

    @Test
    fun testDownloadAndUploadFile() {
        val downloadId = "test_download"
        val uploadId = "test_upload"

        val testDir = tempFolder.newFolder("catroid_project")
        val localFile = File(testDir, "downloaded_logo.png")

        ChEngineHttpManager.createRequest(downloadId, "GET", "https://httpbingo.org/image/png")

        val downloadLatch = CountDownLatch(1)
        ChEngineHttpManager.executeRequest(downloadId) {
            downloadLatch.countDown()
        }
        assertTrue("Download timed out", downloadLatch.await(15, TimeUnit.SECONDS))

        val saveSuccess = ChEngineHttpManager.saveResponseToFile(downloadId, localFile)
        assertTrue("Failed to save response to file", saveSuccess)
        assertTrue("File does not exist on disk", localFile.exists())
        assertTrue("File is empty", localFile.length() > 0)

        ChEngineHttpManager.createRequest(uploadId, "POST", "https://httpbingo.org/post")
        ChEngineHttpManager.attachFile(uploadId, localFile, "file", "image/png")

        val uploadLatch = CountDownLatch(1)
        ChEngineHttpManager.executeRequest(uploadId) {
            uploadLatch.countDown()
        }
        assertTrue("Upload timed out", uploadLatch.await(15, TimeUnit.SECONDS))

        val uploadResponse = ChEngineHttpManager.getResponseText(uploadId)

        assertTrue("File was not uploaded correctly: $uploadResponse",
            uploadResponse.contains("downloaded_logo.png") || uploadResponse.contains("\"file\"")
        )
    }
}
