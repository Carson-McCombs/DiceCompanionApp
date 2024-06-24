package model.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import model.database.dao.ExpressionDao
import model.database.dao.ExpressionDependencyDao
import model.database.dao.GroupDao
import model.database.dao.GroupWithChildrenDao
import model.database.entity.ExpressionDirectDependenciesEntity
import model.database.entity.ExpressionEntity
import model.database.entity.GroupEntity

@Database(entities = [GroupEntity::class, ExpressionEntity::class, ExpressionDirectDependenciesEntity::class], version = 27, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expressionDao(): ExpressionDao
    abstract fun groupDao(): GroupDao
    abstract fun groupWithChildrenDao(): GroupWithChildrenDao
    abstract fun expressionDependencyDao(): ExpressionDependencyDao

    companion object{

        @Volatile
        private var instance: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase {
            return (instance ?: synchronized(this){
                val newInstance = Room.databaseBuilder(
                    context = context,
                    klass = AppDatabase::class.java,
                    name = "app_database"
                ).fallbackToDestructiveMigration().build()
                instance = newInstance
                instance
            }) as AppDatabase
        }
    }
}