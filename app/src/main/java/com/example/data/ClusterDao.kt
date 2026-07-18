package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClusterDao {
    @Query("SELECT * FROM clusters ORDER BY updatedAt DESC")
    fun getAllClusters(): Flow<List<Cluster>>

    @Query("SELECT * FROM clusters WHERE clusterCode = :clusterCode LIMIT 1")
    suspend fun getClusterByCode(clusterCode: String): Cluster?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCluster(cluster: Cluster)

    @Delete
    suspend fun deleteCluster(cluster: Cluster)

    @Query("DELETE FROM clusters WHERE clusterCode = :clusterCode")
    suspend fun deleteClusterByCode(clusterCode: String)
}
