package io.github.naharaoss.skpd.resource

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceContentStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    val root = File(context.filesDir, "resource/content")

    suspend fun referenceRootOf(reference: String): File = withContext(Dispatchers.IO) {
        val referenceRoot = File(root, reference)
        referenceRoot.mkdirs()
        referenceRoot
    }
}