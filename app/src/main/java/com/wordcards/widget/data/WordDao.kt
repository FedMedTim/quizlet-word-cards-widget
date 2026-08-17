package com.wordcards.widget.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface WordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSet(set: SetEntity)

    @Query("SELECT * FROM sets WHERE id = :setId")
    suspend fun getSet(setId: String): SetEntity?

    @Query("SELECT * FROM sets ORDER BY title")
    suspend fun getAllSets(): List<SetEntity>

    @Query("SELECT * FROM terms WHERE setId = :setId ORDER BY rank")
    suspend fun getTerms(setId: String): List<TermEntity>

    @Query("SELECT COUNT(*) FROM terms WHERE setId = :setId")
    suspend fun countTerms(setId: String): Int

    @Query("SELECT COUNT(*) FROM terms WHERE setId = :setId AND learned = 1")
    suspend fun countLearned(setId: String): Int

    @Query("SELECT * FROM terms WHERE setId = :setId ORDER BY rank LIMIT 1 OFFSET :offset")
    suspend fun getTermAt(setId: String, offset: Int): TermEntity?

    @Query("UPDATE terms SET learned = :learned WHERE id = :termId")
    suspend fun setLearned(termId: Long, learned: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerms(terms: List<TermEntity>)

    @Query("DELETE FROM terms WHERE setId = :setId AND id NOT IN (:keepIds)")
    suspend fun deleteMissing(setId: String, keepIds: List<Long>)

    @Query("DELETE FROM terms WHERE setId = :setId")
    suspend fun deleteAllTerms(setId: String)

    /**
     * Раскладывает свежую выгрузку набора поверх локальной: отметки «выучено»
     * переносятся по id, карточки, удалённые в Quizlet, вычищаются.
     */
    @Transaction
    suspend fun replaceSetContents(set: SetEntity, terms: List<TermEntity>) {
        val learnedIds = getTerms(set.id).filter { it.learned }.map { it.id }.toSet()
        upsertSet(set)
        if (terms.isEmpty()) {
            deleteAllTerms(set.id)
            return
        }
        insertTerms(terms.map { it.copy(learned = it.id in learnedIds) })
        deleteMissing(set.id, terms.map { it.id })
    }
}
