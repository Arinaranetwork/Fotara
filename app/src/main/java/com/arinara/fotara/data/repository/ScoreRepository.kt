// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.repository

import android.content.ContentValues
import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.data.model.AcademicScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

interface ScoreRepository {
    fun observeScores(): Flow<List<AcademicScore>>
    suspend fun getScoresSync(): List<AcademicScore>
    suspend fun addScore(score: AcademicScore): Long
    suspend fun updateScore(score: AcademicScore): Boolean
    suspend fun deleteScore(id: Long): Boolean
    suspend fun importPresetScores(scores: List<AcademicScore>)
    suspend fun clearAllScores()
}

class SqliteScoreRepository(
    private val dbHelper: FotaraDbHelper
) : ScoreRepository {

    private val _scoresFlow = MutableStateFlow<List<AcademicScore>>(emptyList())

    init {
        ensureTable()
        refreshSync()
    }

    private fun ensureTable() {
        try {
            val db = dbHelper.getSafeWritableDatabase()
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS academic_scores (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    subject TEXT NOT NULL,
                    title TEXT NOT NULL,
                    category TEXT NOT NULL,
                    score REAL NOT NULL,
                    max_score REAL NOT NULL DEFAULT 100.0,
                    weight REAL NOT NULL DEFAULT 0.0,
                    evidence_uri TEXT,
                    evidence_name TEXT,
                    exam_date_millis INTEGER,
                    created_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
        } catch (_: Exception) {}
    }

    private fun refreshSync() {
        try {
            val db = dbHelper.getSafeReadableDatabase()
            val list = mutableListOf<AcademicScore>()
            val cursor = db.rawQuery(
                """
                SELECT id, subject, title, category, score, max_score, weight, evidence_uri, evidence_name, exam_date_millis, created_at
                FROM academic_scores
                ORDER BY created_at DESC, id DESC
                """.trimIndent(),
                null
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    list.add(
                        AcademicScore(
                            id = c.getLong(0),
                            subject = c.getString(1) ?: "",
                            title = c.getString(2) ?: "",
                            category = c.getString(3) ?: "Assignment",
                            score = c.getDouble(4),
                            maxScore = c.getDouble(5),
                            weight = c.getDouble(6),
                            evidenceUri = c.getString(7),
                            evidenceName = c.getString(8),
                            examDateMillis = if (c.isNull(9)) null else c.getLong(9),
                            createdAt = c.getLong(10)
                        )
                    )
                }
            }
            _scoresFlow.value = list
        } catch (_: Exception) {}
    }

    override fun observeScores(): Flow<List<AcademicScore>> = _scoresFlow.asStateFlow()

    override suspend fun getScoresSync(): List<AcademicScore> = withContext(Dispatchers.IO) {
        refreshSync()
        _scoresFlow.value
    }

    override suspend fun addScore(score: AcademicScore): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val cv = ContentValues().apply {
            put("subject", score.subject)
            put("title", score.title)
            put("category", score.category)
            put("score", score.score)
            put("max_score", score.maxScore)
            put("weight", score.weight)
            put("evidence_uri", score.evidenceUri)
            put("evidence_name", score.evidenceName)
            put("exam_date_millis", score.examDateMillis)
            put("created_at", score.createdAt)
        }
        val id = db.insert("academic_scores", null, cv)
        refreshSync()
        id
    }

    override suspend fun updateScore(score: AcademicScore): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val cv = ContentValues().apply {
            put("subject", score.subject)
            put("title", score.title)
            put("category", score.category)
            put("score", score.score)
            put("max_score", score.maxScore)
            put("weight", score.weight)
            put("evidence_uri", score.evidenceUri)
            put("evidence_name", score.evidenceName)
            put("exam_date_millis", score.examDateMillis)
        }
        val rows = db.update("academic_scores", cv, "id = ?", arrayOf(score.id.toString()))
        refreshSync()
        rows > 0
    }

    override suspend fun deleteScore(id: Long): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val rows = db.delete("academic_scores", "id = ?", arrayOf(id.toString()))
        refreshSync()
        rows > 0
    }

    override suspend fun importPresetScores(scores: List<AcademicScore>) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        db.beginTransaction()
        try {
            for (score in scores) {
                val cv = ContentValues().apply {
                    put("subject", score.subject)
                    put("title", score.title)
                    put("category", score.category)
                    put("score", score.score)
                    put("max_score", score.maxScore)
                    put("weight", score.weight)
                    put("evidence_uri", score.evidenceUri)
                    put("evidence_name", score.evidenceName)
                    put("exam_date_millis", score.examDateMillis)
                    put("created_at", score.createdAt)
                }
                db.insert("academic_scores", null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshSync()
    }

    override suspend fun clearAllScores() = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        db.delete("academic_scores", null, null)
        refreshSync()
    }
}
