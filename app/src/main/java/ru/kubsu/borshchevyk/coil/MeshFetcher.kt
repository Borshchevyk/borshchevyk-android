package ru.kubsu.borshchevyk.coil

import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
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
        
        val file = meshMediaTransferManager.getLocalFile(attachmentId)
            ?: throw FileNotFoundException("Mesh attachment $attachmentId not found locally.")

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
