package com.diary.app.ui.relationships

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diary.app.DiaryApplication
import com.diary.app.data.PersonMention
import com.diary.app.data.TrackedPerson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PersonWithDetails(
    val person: TrackedPerson,
    val mentions: List<PersonMention>
)

class RelationshipViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as DiaryApplication).database.diaryDao()

    private val _persons = MutableStateFlow<List<PersonWithDetails>>(emptyList())
    val persons: StateFlow<List<PersonWithDetails>> = _persons.asStateFlow()

    init {
        loadPersons()
        scanForPersons()
    }

    fun loadPersons() {
        viewModelScope.launch {
            dao.getAllTrackedPersons().collect { personList ->
                val details = personList.map { person ->
                    val mentions = dao.getPersonMentions(person.id).first()
                    PersonWithDetails(person, mentions)
                }
                _persons.value = details
            }
        }
    }

    private fun scanForPersons() {
        viewModelScope.launch {
            val entries = dao.getAllEntriesOnce()
            val existingPersons = dao.getAllTrackedPersons().first()
            val existingNames = existingPersons.map { it.name }.toSet()
            val foundNames = mutableSetOf<String>()

            entries.forEach { entry ->
                val text = entry.plainText
                if (text.isBlank()) return@forEach

                // 简单的人名识别 - 查找常见的称呼模式
                val patterns = listOf(
                    Regex("(?:和|跟|与)([\\u4e00-\\u9fa5]{2,4})(?:一起|也|都|说)"),
                    Regex("([\\u4e00-\\u9fa5]{2,4})(?:说|告诉|问我)"),
                    Regex("(?:妈妈|爸爸|姐姐|哥哥|弟弟|妹妹|爷爷|奶奶|外公|外婆)")
                )

                patterns.forEach { pattern ->
                    pattern.findAll(text).forEach { match ->
                        val name = match.groupValues.getOrElse(1) { match.value }
                        if (name.length in 2..4 && name !in existingNames) {
                            foundNames.add(name)
                        }
                    }
                }

                // 检查家庭称呼
                val familyTerms = listOf("妈妈", "爸爸", "姐姐", "哥哥", "弟弟", "妹妹", "爷爷", "奶奶")
                familyTerms.forEach { term ->
                    if (text.contains(term) && term !in existingNames) {
                        foundNames.add(term)
                    }
                }
            }

            foundNames.forEach { name ->
                val person = TrackedPerson(
                    name = name,
                    mentionCount = 1,
                    lastMentionedAt = System.currentTimeMillis()
                )
                val id = dao.insertTrackedPerson(person)
                // 添加提及记录
                entries.filter { it.plainText.contains(name) }.take(5).forEach { entry ->
                    dao.insertPersonMention(
                        PersonMention(
                            personId = id,
                            diaryId = entry.id,
                            context = extractContext(entry.plainText, name),
                            sentiment = 0f
                        )
                    )
                }
            }
        }
    }

    private fun extractContext(text: String, name: String): String {
        val idx = text.indexOf(name)
        if (idx < 0) return ""
        val start = (idx - 20).coerceAtLeast(0)
        val end = (idx + name.length + 20).coerceAtMost(text.length)
        return text.substring(start, end).replace("\n", " ")
    }
}
