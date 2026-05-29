package io.github.naharaoss.skpd.resource

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppDatabaseModule {
    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase = Room
        .databaseBuilder(context = context, klass = AppDatabase::class.java, name = "sketchpad-index")
        .build()

    @Singleton
    @Provides
    fun provideBadgeDao(database: AppDatabase): AppDatabase.BadgeDao = database.badgeDao()

    @Singleton
    @Provides
    fun provideBrushDao(database: AppDatabase): AppDatabase.BrushDao = database.brushDao()
}

@Database(
    version = 1,
    entities = [
        AppDatabase.Badge::class,
        AppDatabase.LibraryItem::class,
        AppDatabase.Brush::class,
        AppDatabase.Tag::class,
        AppDatabase.BrushTag::class
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun badgeDao(): BadgeDao
    abstract fun libraryDao(): LibraryDao
    abstract fun tagDao(): TagDao
    abstract fun brushDao(): BrushDao

    @Dao
    interface BadgeDao {
        @Query("SELECT * FROM badge")
        suspend fun getAll(): List<Badge>

        @Insert
        suspend fun insert(badge: Badge)
    }

    @Dao
    interface LibraryDao {
        @Query("SELECT * FROM library WHERE parentId == NULL")
        suspend fun getRoot(): List<LibraryItem>

        @Query("SELECT * FROM library WHERE parentId = :parentId")
        suspend fun getChildrenByParentId(parentId: Long): List<LibraryItem>

        @Insert
        suspend fun insert(item: LibraryItem): Long

        @Update
        suspend fun update(item: LibraryItem)

        @Delete
        suspend fun delete(item: LibraryItem)
    }

    @Dao
    interface TagDao {
        @Query("SELECT * FROM tag")
        suspend fun getAll(): List<Tag>

        @Insert
        suspend fun insert(tag: Tag): Long

        @Update
        suspend fun update(tag: Tag)

        @Delete
        suspend fun delete(tag: Tag)
    }

    @Dao
    interface BrushDao {
        @Query("SELECT * FROM brush")
        suspend fun getAll(): List<Brush>

        @Query("SELECT * FROM brush WHERE name LIKE '%' || :keyword || '%'")
        suspend fun searchAll(keyword: String): List<Brush>

        @Query("""
            SELECT brush.brushId, brush.name, brush.icon, brush.reference FROM brush
            INNER JOIN brushTag ON brush.brushId = brushTag.brushId
            INNER JOIN tag ON tag.tagId = brushTag.tagId
            WHERE tag.tagId = :tagId
        """)
        suspend fun getByTagId(tagId: Long): List<Brush>
        suspend fun getByTag(tag: Tag) = getByTagId(tag.tagId)

        @Query("""
            SELECT brush.brushId, brush.name, brush.icon, brush.reference FROM brush
            INNER JOIN brushTag ON brush.brushId = brushTag.brushId
            INNER JOIN tag ON tag.tagId = brushTag.tagId
            WHERE tag.tagId = :tagId AND brush.name LIKE '%' || :keyword || '%'
        """)
        suspend fun searchByTagId(tagId: Long, keyword: String): List<Brush>
        suspend fun searchByTag(tag: Tag, keyword: String) = searchByTagId(tag.tagId, keyword)

        @Query("""
            SELECT tag.tagId, tag.name, tag.icon FROM tag
            INNER JOIN brushTag ON tag.tagId = brushTag.tagId
            INNER JOIN brush ON brush.brushId = brushTag.brushId
            WHERE brush.brushId = :brushId
        """)
        suspend fun getTagsByBrushId(brushId: Long): List<Tag>
        suspend fun getTagsByBrush(brush: Brush) = getTagsByBrushId(brush.brushId)

        @Insert
        suspend fun insert(brush: Brush): Long

        @Update
        suspend fun update(brush: Brush)

        @Delete
        suspend fun delete(brush: Brush)

        @Insert
        suspend fun tagBrush(tagger: BrushTag)

        @Delete
        suspend fun untagBrush(tagger: BrushTag)
    }

    @Entity(tableName = "badge")
    data class Badge(
        @PrimaryKey(autoGenerate = false)
        @ColumnInfo(name = "badgeId")
        val badgeId: String
    )

    @Entity(
        tableName = "library",
        indices = [Index("parentId")],
        foreignKeys = [
            ForeignKey(
                entity = LibraryItem::class,
                parentColumns = ["libraryId"],
                childColumns = ["parentId"],
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE
            )
        ]
    )
    data class LibraryItem(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "libraryId")
        val libraryId: Long = 0,

        @ColumnInfo(name = "parentId")
        val parentId: Long?,

        @ColumnInfo(name = "name")
        val name: String,

        @ColumnInfo(name = "reference")
        val reference: String?
    )

    data class LibraryFolderWithChildren(
        @Embedded
        val parent: LibraryItem,

        @Relation(parentColumn = "libraryId", entityColumn = "parentId")
        val children: List<LibraryItem>
    )

    @Entity(tableName = "brush")
    data class Brush(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "brushId")
        val brushId: Long = 0,

        @ColumnInfo(name = "name")
        val name: String,

        @ColumnInfo(name = "icon", defaultValue = "NULL")
        val icon: String?,

        @ColumnInfo(name = "reference")
        val reference: String
    )

    @Entity(tableName = "tag")
    data class Tag(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "tagId")
        val tagId: Long = 0,

        @ColumnInfo(name = "name")
        val name: String,

        @ColumnInfo(name = "icon")
        val icon: String?
    )

    @Entity(
        tableName = "brushTag",
        indices = [Index("tagId")],
        primaryKeys = ["brushId", "tagId"],
        foreignKeys = [
            ForeignKey(
                entity = Brush::class,
                parentColumns = ["brushId"],
                childColumns = ["brushId"],
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE
            ),
            ForeignKey(
                entity = Tag::class,
                parentColumns = ["tagId"],
                childColumns = ["tagId"],
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE
            )
        ]
    )
    data class BrushTag(
        @ColumnInfo(name = "brushId")
        val brushId: Long,

        @ColumnInfo(name = "tagId")
        val tagId: Long
    )

    data class BrushWithTags(
        @Embedded
        val brush: Brush,

        @Relation(
            parentColumn = "brushId",
            entityColumn = "tagId",
            associateBy = Junction(
                value = BrushTag::class,
                parentColumn = "brushId",
                entityColumn = "tagId"
            )
        )
        val tags: List<Tag>
    )
}