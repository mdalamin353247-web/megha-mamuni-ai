package com.meghamamuni.assistant

/**
 * Assistant Commands handler
 * মেঘা মামুনি এর কমান্ড সিস্টেম
 */
enum class CommandType {
    OPEN_APP, SEARCH, WEATHER, CALCULATE, GENERAL
}

class AssistantCommands {
    fun detectCommandType(input: String): CommandType {
        return when {
            input.contains("খোল") || input.contains("open") -> CommandType.OPEN_APP
            input.contains("সার্চ") || input.contains("search") -> CommandType.SEARCH
            input.contains("আবহাওয়া") || input.contains("weather") -> CommandType.WEATHER
            input.contains("হিসাব") || input.contains("calculate") -> CommandType.CALCULATE
            else -> CommandType.GENERAL
        }
    }
}
