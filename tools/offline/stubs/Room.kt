// Compile-only annotations. No Room implementation is simulated.
package androidx.room
annotation class Entity(val tableName: String = "", val indices: Array<Index> = [])
annotation class Index(val value: Array<String>, val unique: Boolean = false)
annotation class PrimaryKey(val autoGenerate: Boolean = false)
annotation class ColumnInfo(val defaultValue: String = "", val collate: Int = 1) { companion object { const val NOCASE = 3 } }
annotation class Dao
annotation class Delete
annotation class Insert(val onConflict: Int = 3)
annotation class Update
annotation class Query(val value: String)
annotation class Transaction
object OnConflictStrategy { const val ABORT = 3; const val IGNORE = 5 }
