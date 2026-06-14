package ru.kubsu.borshchevyk.coil

import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import okio.Path.Companion.toOkioPath
import ru.kubsu.borshchevyk.core.network.mesh.MeshMediaTransferManager
import java.io.FileNotFoundException

class MeshFetcher(
    private val data: Uri,
    private val options: Options,
    private val meshMediaTransferManager: MeshMediaTransferManager
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val attachmentId = data.lastPathSegment ?: return null
        
        var file = meshMediaTransferManager.getLocalFile(attachmentId)
        
        if (file == null) {
            // Trigger a pull just in case
            meshMediaTransferManager.pullFile(attachmentId)
            
            // Suspend and wait for the file to arrive over the mesh network
            file = withTimeoutOrNull(30_000) {
                kotlinx.coroutines.flow.flow {
                    val initialCheck = meshMediaTransferManager.getLocalFile(attachmentId)
                    if (initialCheck != null) {
                        emit(initialCheck)
                    } else {
                        meshMediaTransferManager.incomingFiles.collect {
                            if (it.metadata?.attachmentId == attachmentId) emit(it.file)
                        }
                    }
                }.first()
            }
        }

        if (file == null) {
            throw FileNotFoundException("Mesh attachment $attachmentId timed out or failed to download.")
        }

        return SourceResult(
            source = ImageSource(
                file = file.toOkioPath()
            ),
            mimeType = null,
            dataSource = DataSource.DISK
        )
    }

    class Factory(
        private val meshMediaTransferManager: MeshMediaTransferManager
    ) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != "mesh") return null
            return MeshFetcher(data, options, meshMediaTransferManager)
        }
    }
}
