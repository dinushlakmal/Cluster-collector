package com.example.data

import kotlinx.coroutines.flow.Flow

class ClusterRepository(private val clusterDao: ClusterDao) {
    val allClusters: Flow<List<Cluster>> = clusterDao.getAllClusters()

    suspend fun getClusterByCode(clusterCode: String): Cluster? {
        return clusterDao.getClusterByCode(clusterCode)
    }

    suspend fun insert(cluster: Cluster) {
        clusterDao.insertCluster(cluster)
    }

    suspend fun delete(cluster: Cluster) {
        clusterDao.deleteCluster(cluster)
    }

    suspend fun deleteByCode(clusterCode: String) {
        clusterDao.deleteClusterByCode(clusterCode)
    }
}
