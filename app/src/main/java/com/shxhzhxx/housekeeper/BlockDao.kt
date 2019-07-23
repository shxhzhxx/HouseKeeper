package com.shxhzhxx.housekeeper

import androidx.lifecycle.LiveData
import androidx.room.*
import java.math.BigInteger
import java.security.MessageDigest


val md: MessageDigest by lazy { MessageDigest.getInstance("MD5") }
@Synchronized
fun String.hash() = BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')

@Entity(
    tableName = "block",
    indices = [Index(value = ["prev"])]
)
data class Block(
    val prev: String,
    val data: String,
    @PrimaryKey val hash: String = "${prev.hash()}data".hash()
)

@Dao
interface BlockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg blocks: Block)

    @Delete
    fun delete(vararg blocks: Block)

    @Query("DELETE FROM block")
    suspend fun clear()

    @Suppress("AndroidUnresolvedRoomSqlReference")
    @Query("WITH RECURSIVE result(prev,data,hash) AS (SELECT prev,data,hash FROM block WHERE prev=:hash UNION SELECT block.prev,block.data,block.hash FROM block,result WHERE result.hash==block.prev) SELECT * FROM result")
    fun subChain(hash: String): List<Block>

    @Query("SELECT * FROM block WHERE hash=:hash")
    fun get(hash: String): Block?

    @Query("SELECT * FROM block")
    fun observableList(): LiveData<List<Block>>

    @Query("SELECT hash FROM block WHERE hash NOT IN (SELECT prev from block)")
    fun head(): String?
}