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
    suspend fun insertAll(blocks: List<Block>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg blocks: Block)

    @Query("DELETE FROM block")
    suspend fun clear()

    @Query("SELECT * FROM block")
    fun list(): LiveData<List<Block>>

    @Query("SELECT hash FROM block WHERE hash NOT IN (SELECT prev from block)")
    suspend fun head(): String?
}